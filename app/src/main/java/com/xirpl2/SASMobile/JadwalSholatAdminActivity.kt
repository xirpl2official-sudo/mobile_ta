package com.xirpl2.SASMobile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.JadwalSholatUpdateRequest
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class JadwalSholatAdminActivity : BaseAdminActivity() {

    private val TAG = "JadwalSholatAdminActivity"
    private val repository = BerandaRepository()

    // Jadwal list will be fetched from API
    private var jadwalList: List<JadwalSholatData> = emptyList()

    // Dropdown options
    private lateinit var dhuhaAdapter: DhuhaScheduleAdapter
    private val modifiedDhuhaItems = mutableSetOf<JadwalSholatData>()

    // Dropdown options
    private val daysOptions = listOf("Semua Hari", "Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    private val jurusanOptions = listOf("Semua Jurusan", "TKJ", "RPL", "DKV", "ANM", "BC", "TAV", "TEI", "TMT")

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.JADWAL_SHOLAT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_sholat_admin)
        setupStatusBar()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Drawer and Sidebar (from BaseAdminActivity)
        setupDrawerAndSidebar()

        // Setup Menu Icon (from BaseAdminActivity)
        setupMenuIcon()

        // Setup Buttons (dengan pemisahan permission view vs edit)
        setupButtons()

        // Load jadwal list from API
        loadJadwalList()
    }

    /**
     * Load jadwal sholat list from API
     */
    private fun loadJadwalList() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    jadwalList = list
                    Log.d(TAG, "Loaded ${list.size} jadwal items")

                    // 🔍 DEBUG: Log semua item untuk troubleshooting
                    list.forEach { item ->
                        Log.d(TAG, "Jadwal: ${item.jenis_sholat}, Hari: ${item.hari}, Jurusan: ${item.jurusan}")
                    }

                    // Update UI 
                    updateJadwalUI()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed loading jadwal: ${error.message}")
                }
            )
        }
    }

    /**
     * Find jadwal ID by jenis sholat name
     * Prioritize general schedule (jurusan is null/empty)
     */
    private fun findJadwalIdByJenis(jenisSholat: String): Int? {
        // First try to find general schedule
        var jadwal = jadwalList.find {
            it.jenis_sholat.equals(jenisSholat, ignoreCase = true) && it.jurusan.isNullOrEmpty()
        }

        // If not found, take any for that type
        if (jadwal == null) {
            jadwal = jadwalList.find {
                it.jenis_sholat.equals(jenisSholat, ignoreCase = true)
            }
        }

        return jadwal?.id
    }

    /**
     * Update UI with jadwal data - TANPA filtering berdasarkan role
     * Semua role bisa melihat semua jadwal yang tersedia
     */
    private fun updateJadwalUI() {
        // ✅ Populate table Dhuha untuk SEMUA role (termasuk wali kelas)
        populateDhuhaTable()

        // ✅ Update card Dhuha - ambil data apa saja yang tersedia
        val dhuhaJadwal = jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true)
        }

        findViewById<TextView>(R.id.tvWaktuDhuha)?.text =
            if (dhuhaJadwal != null) "Waktu: ${dhuhaJadwal.jam_mulai} - ${dhuhaJadwal.jam_selesai}"
            else "Waktu: -"

        // ✅ Update Zuhur dengan pola yang sama
        val zuhurJadwal = jadwalList.find {
            it.jenis_sholat.equals("Dzuhur", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Dzuhur", ignoreCase = true)
        }
        findViewById<TextView>(R.id.tvWaktuZuhur)?.text =
            if (zuhurJadwal != null) "Waktu: ${zuhurJadwal.jam_mulai} - ${zuhurJadwal.jam_selesai}"
            else "Waktu: -"

        // ✅ Update Jumat dengan pola yang sama
        val jumatJadwal = jadwalList.find {
            it.jenis_sholat.equals("Jumat", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Jumat", ignoreCase = true)
        }
        findViewById<TextView>(R.id.tvWaktuJumat)?.text =
            if (jumatJadwal != null) "Waktu: ${jumatJadwal.jam_mulai} - ${jumatJadwal.jam_selesai}"
            else "Waktu: -"
    }

    private fun populateDhuhaTable() {
        val rvDhuhaSchedule = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDhuhaSchedule) ?: return

        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
        val dhuhaSchedules = jadwalList.filter { it.jenis_sholat.equals("Dhuha", ignoreCase = true) }

        val rows = days.map { day ->
            val schedulesForDay = dhuhaSchedules.filter { it.hari.equals(day, ignoreCase = true) }
            DhuhaDayRow(
                day = day,
                slot1 = schedulesForDay.getOrNull(0),
                slot2 = schedulesForDay.getOrNull(1)
            )
        }

        if (!::dhuhaAdapter.isInitialized) {
            dhuhaAdapter = DhuhaScheduleAdapter(
                rows = rows,
                onModified = { item ->
                    // mark the item as modified
                    modifiedDhuhaItems.add(item)
                },
                onEditClick = null // <-- Dihapus agar tidak muncul edit dialog saat klik jurusan
            )
            rvDhuhaSchedule.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvDhuhaSchedule.adapter = dhuhaAdapter
        } else {
            dhuhaAdapter.updateData(rows)
        }
    }

    /**
     * Setup button click handlers
     * 🔹 Memisahkan permission VIEW (semua role) vs EDIT (hanya admin)
     */
    private fun setupButtons() {
        val btnTambah = findViewById<MaterialButton>(R.id.btnTambah)
        val role = getSharedPreferences("UserData", MODE_PRIVATE).getString("user_role", "")

        // 🔹 TENTUKAN PERMISSION
        val isReadOnly = role == "wali_kelas" || role == "guru"
        val canEdit = !isReadOnly  // Hanya admin yang bisa edit

        // 🔹 ATUR VISIBILITY BUTTON CRUD (hanya untuk yang bisa edit)
        if (!canEdit) {
            // Sembunyikan button CRUD untuk read-only users
            btnTambah.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditDhuha)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditWaktuDhuha)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditZuhur)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditJumat)?.visibility = View.GONE
            findViewById<MaterialButton>(R.id.btnSaveDhuha)?.visibility = View.GONE

            // ⚠️ JANGAN sembunyikan container/data jadwal!
            // Biarkan wali kelas bisa MELIHAT semua data
            return  // Exit early, tidak perlu setup listener untuk CRUD
        }

        // 🔹 SETUP CRUD BUTTONS (hanya untuk admin)
        btnTambah.setOnClickListener {
            Toast.makeText(this, "Fitur Tambah Jadwal belum tersedia di API", Toast.LENGTH_LONG).show()
        }

        // Edit Jadwal Dhuha per Jurusan (table) with Click-to-Swap logic
        val btnEditDhuha = findViewById<ImageView>(R.id.btnEditDhuha)
        val btnSaveDhuha = findViewById<MaterialButton>(R.id.btnSaveDhuha)

        btnEditDhuha.setOnClickListener {
            if (!::dhuhaAdapter.isInitialized) return@setOnClickListener
            dhuhaAdapter.isEditMode = true
            btnEditDhuha.visibility = View.GONE
            btnSaveDhuha.visibility = View.VISIBLE
            Toast.makeText(this, "Mode Edit Aktif: Klik dua jurusan untuk bertukar posisi", Toast.LENGTH_LONG).show()
        }

        btnSaveDhuha.setOnClickListener {
            if (!::dhuhaAdapter.isInitialized) return@setOnClickListener
            dhuhaAdapter.isEditMode = false
            btnSaveDhuha.visibility = View.GONE
            btnEditDhuha.visibility = View.VISIBLE

            if (modifiedDhuhaItems.isNotEmpty()) {
                saveSwappedDhuhaItems()
            } else {
                Toast.makeText(this, "Tidak ada perubahan yang disimpan.", Toast.LENGTH_SHORT).show()
            }
        }

        // Edit Waktu Sholat
        findViewById<ImageView>(R.id.btnEditWaktuDhuha)?.setOnClickListener {
            showEditDialogByJenis("Dhuha")
        }
        findViewById<ImageView>(R.id.btnEditZuhur)?.setOnClickListener {
            showEditDialogByJenis("Dzuhur")
        }
        findViewById<ImageView>(R.id.btnEditJumat)?.setOnClickListener {
            showEditDialogByJenis("Jumat")
        }
    }

    /**
     * Show edit dialog by finding jadwal ID from jenis sholat name
     */
    private fun showEditDialogByJenis(jenisSholat: String) {
        val jadwalId = findJadwalIdByJenis(jenisSholat)
        if (jadwalId != null) {
            showEditDialog(jadwalId, jenisSholat)
        } else {
            // If jadwal list not loaded yet, try to load and retry
            Toast.makeText(this, "Mencari jadwal $jenisSholat...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val token = getAuthToken()
                if (token.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Sesi telah berakhir", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                repository.getJadwalSholat(token).fold(
                    onSuccess = { list ->
                        jadwalList = list
                        updateJadwalUI()
                        val id = list.find { it.jenis_sholat.equals(jenisSholat, ignoreCase = true) }?.id
                        runOnUiThread {
                            if (id != null) {
                                showEditDialog(id, jenisSholat)
                            } else {
                                Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal $jenisSholat tidak ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Gagal memuat jadwal: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    /**
     * Check if selected jurusan already exists for Dhuha on a different day
     * Returns the existing day if duplicate found, null otherwise
     */
    private fun checkDuplicateJurusan(selectedJurusan: String, selectedHari: String, currentJadwalId: Int): String? {
        val existing = jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true) &&
                    it.jurusan.equals(selectedJurusan, ignoreCase = true) &&
                    !it.hari.equals(selectedHari, ignoreCase = true) &&
                    it.id != currentJadwalId
        }
        return existing?.hari
    }

    /**
     * Show edit dialog for jadwal sholat with enhanced dropdowns and validation
     */
    private fun showEditDialog(jadwalId: Int, namaSholat: String) {
        var currentJadwalId = jadwalId
        val token = getAuthToken()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_jadwal_sholat, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etNamaSholat = dialogView.findViewById<TextInputEditText>(R.id.etNamaSholat)
        val etJamMulai = dialogView.findViewById<TextInputEditText>(R.id.etJamMulai)
        val etJamSelesai = dialogView.findViewById<TextInputEditText>(R.id.etJamSelesai)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val tilJurusan = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilJurusan)
        val tvJurusanLabel = dialogView.findViewById<TextView>(R.id.tvJurusanLabel)
        
        val validationContainer = dialogView.findViewById<LinearLayout>(R.id.validationStatusContainer)
        val ivValidationIcon = dialogView.findViewById<ImageView>(R.id.ivValidationIcon)
        val tvValidationStatus = dialogView.findViewById<TextView>(R.id.tvValidationStatus)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        // Variables to store original values for comparison
        var originalHari = ""
        var originalJurusan = ""
        var originalJamMulai = ""
        var originalJamSelesai = ""

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Function to update validation status
        fun updateValidationStatus() {
            val selectedJurusan = actvJurusan.text.toString().trim()
            val selectedHari = actvHari.text.toString().trim()

            if (selectedJurusan.isEmpty() || selectedHari.isEmpty()) {
                validationContainer.visibility = View.GONE
                return
            }

            // Only show validation for Dhuha
            if (!namaSholat.equals("Dhuha", ignoreCase = true)) {
                validationContainer.visibility = View.GONE
                return
            }

            val duplicateDay = checkDuplicateJurusan(selectedJurusan, selectedHari, currentJadwalId)
            validationContainer.visibility = View.VISIBLE

            if (duplicateDay != null) {
                // Duplicate found - show warning
                ivValidationIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivValidationIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                tvValidationStatus.text = "⚠️ Jurusan $selectedJurusan sudah ada di hari $duplicateDay!"
                tvValidationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            } else {
                // No duplicate - show success
                ivValidationIcon.setImageResource(android.R.drawable.ic_menu_send)
                ivValidationIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                tvValidationStatus.text = "✅ Jurusan tersedia"
                tvValidationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
        }

        // Add listener for jurusan selection change
        actvJurusan.setOnItemClickListener { _, _, _, _ ->
            updateValidationStatus()
        }

        // Add listener for hari selection change
        actvHari.setOnItemClickListener { _, _, _, _ ->
            val selectedHari = actvHari.text.toString().trim()

            // IF DAY CHANGED: Try to find data for THAT day, so we EDIT that instead of MOVING this
            if (selectedHari != originalHari && selectedHari != "Semua Hari") {
                val existingData = jadwalList.find {
                    it.jenis_sholat.equals(namaSholat, ignoreCase = true) &&
                            it.hari.equals(selectedHari, ignoreCase = true) &&
                            (if (namaSholat.equals("Dhuha", ignoreCase = true)) it.jurusan == originalJurusan else true)
                }

                if (existingData != null) {
                    // SWITCH TARGET: load its data
                    currentJadwalId = existingData.id
                    etJamMulai.setText(existingData.jam_mulai)
                    etJamSelesai.setText(existingData.jam_selesai)

                    // Update originals to match the new target
                    originalHari = existingData.hari ?: ""
                    originalJamMulai = existingData.jam_mulai
                    originalJamSelesai = existingData.jam_selesai

                    Toast.makeText(this, "Memuat jadwal hari $selectedHari", Toast.LENGTH_SHORT).show()
                }
            }

            updateValidationStatus()
        }

        // Load current data
        lifecycleScope.launch {
            repository.getJadwalSholatById(token, currentJadwalId).fold(
                onSuccess = { jadwal ->
                    runOnUiThread {
                        etNamaSholat.setText(jadwal.jenis_sholat)
                        etJamMulai.setText(jadwal.jam_mulai)
                        etJamSelesai.setText(jadwal.jam_selesai)

                        // Set hari dropdown
                        val hari = jadwal.hari ?: ""
                        actvHari.setText(hari, false)

                        // Set jurusan dropdown
                        val jurusan = jadwal.jurusan ?: ""
                        actvJurusan.setText(jurusan, false)

                        // Hide jurusan selector for Dhuha as requested by user
                        if (namaSholat.equals("Dhuha", ignoreCase = true)) {
                            tilJurusan.visibility = View.GONE
                            tvJurusanLabel.visibility = View.GONE
                            // Always default to "Semua Jurusan" for Dhuha edit from card
                            actvJurusan.setText("Semua Jurusan", false)
                            tvTitle.text = "Edit Jadwal Sholat $namaSholat"
                        } else if (jurusan.isNotEmpty()) {
                            tvTitle.text = "Edit Jadwal Sholat $namaSholat - $jurusan"
                        }

                        // Store original values
                        originalHari = hari
                        originalJurusan = jurusan
                        originalJamMulai = jadwal.jam_mulai
                        originalJamSelesai = jadwal.jam_selesai

                        // Initial validation check
                        updateValidationStatus()
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading jadwal: ${error.message}")
                    runOnUiThread {
                        // Set defaults based on prayer type
                        etNamaSholat.setText(namaSholat)
                        when (namaSholat) {
                            "Dhuha" -> {
                                etJamMulai.setText("06:30")
                                etJamSelesai.setText("09:00")
                            }
                            "Dzuhur" -> {
                                etJamMulai.setText("11:30")
                                etJamSelesai.setText("13:00")
                            }
                            "Jumat" -> {
                                etJamMulai.setText("11:00")
                                etJamSelesai.setText("13:00")
                                actvHari.setText("Jumat", false)
                            }
                        }
                    }
                }
            )
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Save button - show confirmation dialog first
        btnSave.setOnClickListener {
            val updatedNama = etNamaSholat.text.toString().trim()
            val updatedJamMulai = etJamMulai.text.toString().trim()
            val updatedJamSelesai = etJamSelesai.text.toString().trim()
            val updatedHari = actvHari.text.toString().trim()
            val updatedJurusan = actvJurusan.text.toString().trim()

            // Validate required fields
            if (updatedNama.isEmpty() || updatedJamMulai.isEmpty() || updatedJamSelesai.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate time format
            if (!isValidTimeFormat(updatedJamMulai) || !isValidTimeFormat(updatedJamSelesai)) {
                Toast.makeText(this, "Format waktu tidak valid (HH:mm atau HH:mm:ss)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate waktu_selesai > waktu_mulai
            if (!isEndTimeAfterStart(updatedJamMulai, updatedJamSelesai)) {
                Toast.makeText(this, "Waktu selesai harus lebih besar dari waktu mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for duplicate and show confirmation
            val duplicateDay = if (namaSholat.equals("Dhuha", ignoreCase = true) && updatedJurusan.isNotEmpty()) {
                checkDuplicateJurusan(updatedJurusan, updatedHari, currentJadwalId)
            } else null

            showConfirmationDialog(
                originalHari = originalHari,
                newHari = updatedHari,
                originalJurusan = originalJurusan,
                newJurusan = updatedJurusan,
                originalWaktu = "$originalJamMulai - $originalJamSelesai",
                newWaktu = "$updatedJamMulai - $updatedJamSelesai",
                duplicateDay = duplicateDay,
                onConfirm = {
                    // Proceed with save
                    saveJadwal(
                        dialog = dialog,
                        jadwalId = currentJadwalId,
                        token = token,
                        updatedNama = updatedNama,
                        updatedJamMulai = updatedJamMulai,
                        updatedJamSelesai = updatedJamSelesai,
                        updatedHari = updatedHari,
                        updatedJurusan = updatedJurusan
                    )
                }
            )
        }

        dialog.show()
    }

    /**
     * Show confirmation dialog before saving
     */
    private fun showConfirmationDialog(
        originalHari: String,
        newHari: String,
        originalJurusan: String,
        newJurusan: String,
        originalWaktu: String,
        newWaktu: String,
        duplicateDay: String?,
        onConfirm: () -> Unit
    ) {
        val changes = StringBuilder()

        if (originalHari != newHari && newHari.isNotEmpty()) {
            changes.append("• Hari: $originalHari → $newHari\n")
        }
        if (originalJurusan != newJurusan && newJurusan.isNotEmpty()) {
            changes.append("• Jurusan: $originalJurusan → $newJurusan\n")
        }
        if (originalWaktu != newWaktu) {
            changes.append("• Waktu: $originalWaktu → $newWaktu\n")
        }

        val message = if (changes.isEmpty()) {
            "Tidak ada perubahan yang terdeteksi.\nApakah Anda tetap ingin menyimpan?"
        } else {
            val warningText = if (duplicateDay != null) {
                "\n⚠️ Peringatan: Jurusan $newJurusan sudah ada di hari $duplicateDay.\nLanjutkan menyimpan?"
            } else ""

            "Anda akan mengubah jadwal:\n\n$changes$warningText"
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Perubahan")
            .setMessage(message)
            .setPositiveButton("Ya, Simpan") { _, _ -> onConfirm() }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Save jadwal to API
     */
    private fun saveJadwal(
        dialog: AlertDialog,
        jadwalId: Int,
        token: String,
        updatedNama: String,
        updatedJamMulai: String,
        updatedJamSelesai: String,
        updatedHari: String,
        updatedJurusan: String
    ) {
        // Show loading state
        Toast.makeText(this, "Menyimpan perubahan...", Toast.LENGTH_SHORT).show()

        if (updatedHari == "Semua Hari" || updatedJurusan == "Semua Jurusan") {
            // BULK UPDATE Logic
            lifecycleScope.launch {
                var successCount = 0
                var failCount = 0

                val itemsToUpdate = if (updatedJurusan == "Semua Jurusan") {
                    // Match ALL items of this type, potentially filtered by day
                    jadwalList.filter {
                        it.jenis_sholat.equals(updatedNama, ignoreCase = true) &&
                                (if (updatedHari == "Semua Hari") true else it.hari.equals(updatedHari, ignoreCase = true))
                    }
                } else {
                    // Match specific jurusan or items with NO jurusan
                    jadwalList.filter {
                        it.jenis_sholat.equals(updatedNama, ignoreCase = true) &&
                                (if (updatedJurusan.isEmpty()) it.jurusan.isNullOrEmpty() else it.jurusan == updatedJurusan) &&
                                (if (updatedHari == "Semua Hari") true else it.hari.equals(updatedHari, ignoreCase = true))
                    }
                }

                itemsToUpdate.forEach { item ->
                    val request = JadwalSholatUpdateRequest(
                        jenis_sholat = updatedNama,
                        jam_mulai = updatedJamMulai,
                        jam_selesai = updatedJamSelesai,
                        hari = if (updatedHari == "Semua Hari") item.hari else updatedHari,
                        jurusan = if (updatedJurusan == "Semua Jurusan") item.jurusan else updatedJurusan.ifEmpty { null }
                    )

                    repository.updateJadwalSholat(token, item.id, request).fold(
                        onSuccess = { successCount++ },
                        onFailure = { failCount++ }
                    )
                }

                runOnUiThread {
                    if (failCount == 0) {
                        Toast.makeText(this@JadwalSholatAdminActivity, "✅ Berhasil memperbarui $successCount jadwal", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@JadwalSholatAdminActivity, "⚠️ $successCount berhasil, $failCount gagal", Toast.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                    loadJadwalList()
                }
            }
        } else {
            // SINGLE UPDATE Logic (as originally intended, but safer)
            val request = JadwalSholatUpdateRequest(
                jenis_sholat = updatedNama,
                jam_mulai = updatedJamMulai,
                jam_selesai = updatedJamSelesai,
                hari = updatedHari.ifEmpty { null },
                jurusan = updatedJurusan.ifEmpty { null }
            )

            lifecycleScope.launch {
                repository.updateJadwalSholat(token, jadwalId, request).fold(
                    onSuccess = {
                        runOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "✅ Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadJadwalList()
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error updating jadwal: ${error.message}")
                        runOnUiThread {
                            val errorMessage = "❌ Gagal menyimpan: ${error.message}"
                            Toast.makeText(this@JadwalSholatAdminActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }

    /**
     * Save swapped dhuha items automatically
     */
    private fun saveSwappedDhuhaItems() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        Toast.makeText(this, "Menyimpan pertukaran jadwal...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            var allSuccess = true
            var errorMsg = ""

            for (item in modifiedDhuhaItems) {
                // Determine new request payload according to swapped fields
                // item.jurusan has already been swapped by DhuhaScheduleAdapter logic
                val request = JadwalSholatUpdateRequest(
                    jenis_sholat = item.jenis_sholat,
                    jam_mulai = item.jam_mulai,
                    jam_selesai = item.jam_selesai,
                    hari = item.hari,
                    jurusan = item.jurusan
                )
                try {
                    val result = repository.updateJadwalSholat(token, item.id, request)
                    if (result.isFailure) {
                        allSuccess = false
                        errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    }
                } catch (e: Exception) {
                    allSuccess = false
                    errorMsg = e.message ?: "Network error"
                }
            }

            runOnUiThread {
                if (allSuccess) {
                    Toast.makeText(this@JadwalSholatAdminActivity, "✅ Berhasil menukar jadwal!", Toast.LENGTH_SHORT).show()
                    modifiedDhuhaItems.clear()
                    // Reload data from API to sync changes properly
                    loadJadwalList()
                } else {
                    Toast.makeText(this@JadwalSholatAdminActivity, "❌ Gagal menyimpan beberapa jadwal: $errorMsg", Toast.LENGTH_LONG).show()
                    modifiedDhuhaItems.clear()
                    // Revert UI to match API state
                    loadJadwalList()
                }
            }
        }
    }

    /**
     * Validate time format HH:mm or HH:mm:ss
     */
    private fun isValidTimeFormat(time: String): Boolean {
        return try {
            val parts = time.split(":")
            if (parts.size !in 2..3) return false
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val seconds = if (parts.size == 3) parts[2].toInt() else 0
            hours in 0..23 && minutes in 0..59 && seconds in 0..59
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate that end time is after start time
     */
    private fun isEndTimeAfterStart(start: String, end: String): Boolean {
        return try {
            val startParts = start.split(":")
            val endParts = end.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            endMinutes > startMinutes
        } catch (e: Exception) {
            false
        }
    }
}
