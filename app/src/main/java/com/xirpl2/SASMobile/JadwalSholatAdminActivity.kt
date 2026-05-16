package com.xirpl2.SASMobile

import android.os.Bundle
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
import com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.JadwalSholatUpdateRequest
import com.xirpl2.SASMobile.model.SholatDhuhaDetail
import com.xirpl2.SASMobile.model.SholatDzuhurDetail
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class JadwalSholatAdminActivity : BaseAdminActivity() {

    private val TAG = "JadwalSholatAdminActivity"
    private val repository = BerandaRepository()

    
    private var jadwalList: List<JadwalSholatData> = emptyList()
    private var dhuhaKeahlianList: List<com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian> = emptyList()

    
    private lateinit var dhuhaAdapter: DhuhaScheduleAdapter

    
    private val daysOptions = listOf("Semua Hari", "Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    private val allKeahlian = arrayOf("RPL", "TKJ", "TAV", "AM", "TMT", "BC", "TEI", "DKV")
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

        
        setupDrawerAndSidebar()

        
        setupMenuIcon()

        
        setupButtons()

        
        loadJadwalList()
        loadSynchronizedData()
    }

    private fun loadSynchronizedData() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            android.util.Log.e(TAG, "Token is empty, skipping loadSynchronizedData")
            return
        }

        android.util.Log.d(TAG, "Loading synchronized data...")

        lifecycleScope.launch {
            // Load Dhuha Keahlian Table - COMMENTED OUT: Phantom API method (not in backend)
            /*
            repository.getJadwalDhuhaKeahlian(token).fold(
                onSuccess = { list ->
                    android.util.Log.d(TAG, "Dhuha Keahlian loaded successfully: ${list.size} items")
                    dhuhaKeahlianList = list
                    runOnUiThread {
                        populateDhuhaTable()
                    }
                },
                onFailure = { error ->
                    android.util.Log.e(TAG, "Failed to load Dhuha Keahlian: ${error.message}")
                    runOnUiThread {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Gagal memuat jadwal dhuha: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )
            */

            // Load Dhuha Detail Card - COMMENTED OUT: Phantom API method (not in backend)
            /*
            repository.getSholatDhuhaDetail(token).onSuccess { detail ->
                android.util.Log.d(TAG, "Sholat Dhuha detail loaded")
                runOnUiThread {
                    findViewById<TextView>(R.id.tvWaktuDhuha)?.text = "Waktu: ${detail.waktuMulai} - ${detail.waktuSelesai}"
                    findViewById<TextView>(R.id.tvHariDhuha)?.text = detail.hari
                    findViewById<TextView>(R.id.tvKelasDhuha)?.text = "Kelas: ${detail.kelas}"
                    
                    // Store ID for editing
                    findViewById<ImageView>(R.id.btnEditWaktuDhuha)?.tag = detail.id
                }
            }.onFailure { error ->
                android.util.Log.e(TAG, "Failed to load Sholat Dhuha detail: ${error.message}")
            }
            */

            // Load Dzuhur Detail Card - COMMENTED OUT: Phantom API method (not in backend)
            /*
            repository.getSholatDzuhurDetail(token).onSuccess { detail ->
                android.util.Log.d(TAG, "Sholat Dzuhur detail loaded")
                runOnUiThread {
                    findViewById<TextView>(R.id.tvWaktuZuhur)?.text = "Waktu: ${detail.waktuMulai} - ${detail.waktuSelesai}"
                    findViewById<TextView>(R.id.tvHariZuhur)?.text = detail.hari
                    findViewById<TextView>(R.id.tvKelasZuhur)?.text = "Kelas: ${detail.kelas}"
                    findViewById<TextView>(R.id.tvJurusanZuhur)?.text = "Jurusan: ${detail.jurusan}"
                    
                    // Store ID for editing
                    findViewById<ImageView>(R.id.btnEditZuhur)?.tag = detail.id
                }
            }.onFailure { error ->
                android.util.Log.e(TAG, "Failed to load Sholat Dzuhur detail: ${error.message}")
            }
            */
        }
    }

    private fun loadJadwalList() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    jadwalList = list

                    
                    list.forEach { item ->
                    }

                    
                    updateJadwalUI()
                },
                onFailure = { error ->
                }
            )
        }
    }

    private fun findJadwalIdByJenis(jenisSholat: String): Int? {
        
        var jadwal = jadwalList.find {
            it.jenis_sholat.equals(jenisSholat, ignoreCase = true) && it.jurusan.isNullOrEmpty()
        }

        
        if (jadwal == null) {
            jadwal = jadwalList.find {
                it.jenis_sholat.equals(jenisSholat, ignoreCase = true)
            }
        }

        return jadwal?.id
    }

    private fun updateJadwalUI() {
        
        populateDhuhaTable()

        
        val dhuhaJadwal = jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true)
        }

        findViewById<TextView>(R.id.tvWaktuDhuha)?.text =
            if (dhuhaJadwal != null) "Waktu: ${dhuhaJadwal.jam_mulai} - ${dhuhaJadwal.jam_selesai}"
            else "Waktu: -"

        findViewById<TextView>(R.id.tvHariDhuha)?.text =
            if (dhuhaJadwal != null) "${dhuhaJadwal.hari ?: "Semua Hari"}"
            else "-"

        findViewById<TextView>(R.id.tvKelasDhuha)?.text =
            if (dhuhaJadwal != null) "Kelas: ${dhuhaJadwal.kelas ?: "Semua Kelas"}"
            else "Kelas: -"

        
        val zuhurJadwal = jadwalList.find {
            it.jenis_sholat.equals("Dzuhur", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Dzuhur", ignoreCase = true)
        }
        findViewById<TextView>(R.id.tvWaktuZuhur)?.text =
            if (zuhurJadwal != null) "Waktu: ${zuhurJadwal.jam_mulai} - ${zuhurJadwal.jam_selesai}"
            else "Waktu: -"

        findViewById<TextView>(R.id.tvHariZuhur)?.text =
            if (zuhurJadwal != null) "${zuhurJadwal.hari ?: "Semua Hari"}"
            else "-"

        findViewById<TextView>(R.id.tvKelasZuhur)?.text =
            if (zuhurJadwal != null) "Kelas: ${zuhurJadwal.kelas ?: "Semua Kelas"}"
            else "Kelas: Semua"

        
        val jumatJadwal = jadwalList.find {
            it.jenis_sholat.equals("Jumat", ignoreCase = true) && it.jurusan.isNullOrEmpty()
        } ?: jadwalList.find {
            it.jenis_sholat.equals("Jumat", ignoreCase = true)
        }
        findViewById<TextView>(R.id.tvWaktuJumat)?.text =
            if (jumatJadwal != null) "Waktu: ${jumatJadwal.jam_mulai} - ${jumatJadwal.jam_selesai}"
            else "Waktu: -"

        findViewById<TextView>(R.id.tvHariJumat)?.text =
            if (jumatJadwal != null) "${jumatJadwal.hari ?: "Jumat"}"
            else "-"

        findViewById<TextView>(R.id.tvKelasJumat)?.text =
            if (jumatJadwal != null) "Kelas: ${jumatJadwal.kelas ?: "Semua Kelas"}"
            else "Kelas: Semua"
    }

    private fun populateDhuhaTable() {
        val rvDhuhaSchedule = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDhuhaSchedule) ?: run {
            android.util.Log.e(TAG, "RecyclerView rvDhuhaSchedule not found in layout!")
            return
        }

        android.util.Log.d(TAG, "Populating Dhuha Table with ${dhuhaKeahlianList.size} items")

        if (!::dhuhaAdapter.isInitialized) {
            android.util.Log.d(TAG, "Initializing DhuhaScheduleAdapter")
            dhuhaAdapter = DhuhaScheduleAdapter(
                rows = dhuhaKeahlianList,
                onEditClick = { item ->
                    showEditDhuhaKeahlianDialog(item)
                }
            )
            rvDhuhaSchedule.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            rvDhuhaSchedule.adapter = dhuhaAdapter
        } else {
            android.util.Log.d(TAG, "Updating existing DhuhaScheduleAdapter with new data")
            dhuhaAdapter.updateData(dhuhaKeahlianList)
        }
    }

    private fun setupButtons() {
        val btnTambah = findViewById<MaterialButton>(R.id.btnTambah)
        val role = getSharedPreferences("UserData", MODE_PRIVATE).getString("user_role", "")?.lowercase() ?: ""

        
        val isReadOnly = role.contains("wali") || role == "guru"
        val canEdit = !isReadOnly && role.contains("admin") 

        
        if (!canEdit) {
            
            btnTambah.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditDhuha)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditWaktuDhuha)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditZuhur)?.visibility = View.GONE
            findViewById<ImageView>(R.id.btnEditJumat)?.visibility = View.GONE
            findViewById<MaterialButton>(R.id.btnSaveDhuha)?.visibility = View.GONE

            
            
            return  
        }

        
        btnTambah.setOnClickListener {
            showTambahDhuhaKeahlianDialog()
        }

        
        val btnEditDhuha = findViewById<ImageView>(R.id.btnEditDhuha)
        val btnSaveDhuha = findViewById<MaterialButton>(R.id.btnSaveDhuha)

        btnEditDhuha.setOnClickListener {
            if (!::dhuhaAdapter.isInitialized) return@setOnClickListener
            dhuhaAdapter.isEditMode = true
            btnEditDhuha.visibility = View.GONE
            btnSaveDhuha.visibility = View.VISIBLE
            Toast.makeText(this, "Mode Edit Aktif: Klik baris untuk mengedit", Toast.LENGTH_LONG).show()
        }

        btnSaveDhuha.setOnClickListener {
            if (!::dhuhaAdapter.isInitialized) return@setOnClickListener
            dhuhaAdapter.isEditMode = false
            btnSaveDhuha.visibility = View.GONE
            btnEditDhuha.visibility = View.VISIBLE
        }

        
        findViewById<ImageView>(R.id.btnEditWaktuDhuha)?.setOnClickListener {
            val id = it.tag as? Int
            if (id != null) {
                showEditSholatDhuhaCardDialog(id)
            } else {
                showEditDialogByJenis("Dhuha")
            }
        }
        findViewById<ImageView>(R.id.btnEditZuhur)?.setOnClickListener {
            val id = it.tag as? Int
            if (id != null) {
                showEditSholatDzuhurCardDialog(id)
            } else {
                showEditDialogByJenis("Dzuhur")
            }
        }
        findViewById<ImageView>(R.id.btnEditJumat)?.setOnClickListener {
            showEditDialogByJenis("Jumat")
        }
    }

    private fun showEditDialogByJenis(jenisSholat: String) {
        val jadwalId = findJadwalIdByJenis(jenisSholat)
        if (jadwalId != null) {
            showEditDialog(jadwalId, jenisSholat)
        } else {
            
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

    private fun checkDuplicateJurusan(selectedJurusan: String, selectedHari: String, currentJadwalId: Int): String? {
        val existing = jadwalList.find {
            it.jenis_sholat.equals("Dhuha", ignoreCase = true) &&
                    it.jurusan.equals(selectedJurusan, ignoreCase = true) &&
                    !it.hari.equals(selectedHari, ignoreCase = true) &&
                    it.id != currentJadwalId
        }
        return existing?.hari
    }

    private fun showEditDialog(jadwalId: Int, namaSholat: String) {
        var currentJadwalId = jadwalId
        val token = getAuthToken()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_jadwal_sholat, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etJamMulai = dialogView.findViewById<TextInputEditText>(R.id.etJamMulai)
        val etJamSelesai = dialogView.findViewById<TextInputEditText>(R.id.etJamSelesai)
        val cbKelas10 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas10)
        val cbKelas11 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas11)
        val cbKelas12 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas12)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val tilJurusan = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilJurusan)
        val tvJurusanLabel = dialogView.findViewById<TextView>(R.id.tvJurusanLabel)
        
        val validationContainer = dialogView.findViewById<LinearLayout>(R.id.validationStatusContainer)
        val ivValidationIcon = dialogView.findViewById<ImageView>(R.id.ivValidationIcon)
        val tvValidationStatus = dialogView.findViewById<TextView>(R.id.tvValidationStatus)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        
        var originalHari = ""
        var originalJurusan = ""
        var originalJamMulai = ""
        var originalJamSelesai = ""
        var originalKelas = ""

        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Semua Hari")
        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days))

        
        val jurusans = listOf("RPL", "BC", "TKJ", "Semua Jurusan")
        actvJurusan.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jurusans))

        
        fun updateValidationStatus() {
            val selectedJurusan = actvJurusan.text.toString().trim()
            val selectedHari = actvHari.text.toString().trim()

            if (selectedJurusan.isEmpty() || selectedHari.isEmpty()) {
                validationContainer.visibility = View.GONE
                return
            }

            
            if (!namaSholat.equals("Dhuha", ignoreCase = true)) {
                val duplicateDay = checkDuplicateJurusan(selectedJurusan, selectedHari, currentJadwalId)
                validationContainer.visibility = View.VISIBLE

                if (duplicateDay != null) {
                    
                    ivValidationIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                    ivValidationIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                    tvValidationStatus.text = "⚠️ Jurusan $selectedJurusan sudah ada di hari $duplicateDay!"
                    tvValidationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                } else {
                    
                    ivValidationIcon.setImageResource(android.R.drawable.ic_menu_send)
                    ivValidationIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    tvValidationStatus.text = "✅ Jurusan tersedia"
                    tvValidationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                }
            } else {
                validationContainer.visibility = View.GONE
            }
        }

        
        actvJurusan.setOnItemClickListener { _, _, _, _ ->
            updateValidationStatus()
        }

        
        actvHari.setOnItemClickListener { _, _, _, _ ->
            val selectedHari = actvHari.text.toString().trim()

            
            if (selectedHari != originalHari && selectedHari != "Semua Hari") {
                val existingData = jadwalList.find {
                    it.jenis_sholat.equals(namaSholat, ignoreCase = true) &&
                            it.hari.equals(selectedHari, ignoreCase = true) &&
                            (if (namaSholat.equals("Dhuha", ignoreCase = true)) it.jurusan == originalJurusan else true)
                }

                if (existingData != null) {
                    
                    currentJadwalId = existingData.id
                    etJamMulai.setText(existingData.jam_mulai)
                    etJamSelesai.setText(existingData.jam_selesai)

                    
                    originalHari = existingData.hari ?: ""
                    originalJamMulai = existingData.jam_mulai
                    originalJamSelesai = existingData.jam_selesai

                    Toast.makeText(this, "Memuat jadwal hari $selectedHari", Toast.LENGTH_SHORT).show()
                }
            }

            updateValidationStatus()
        }

        
        lifecycleScope.launch {
            repository.getJadwalSholatById(token, currentJadwalId).fold(
                onSuccess = { jadwal ->
                    runOnUiThread {
                        etJamMulai.setText(jadwal.jam_mulai)
                        etJamSelesai.setText(jadwal.jam_selesai)

                        
                        val hari = jadwal.hari ?: ""
                        actvHari.setText(hari, false)

                        
                        val jurusan = jadwal.jurusan ?: ""
                        actvJurusan.setText(jurusan, false)

                        
                        val kelasStr = jadwal.kelas ?: ""
                        cbKelas10.isChecked = kelasStr.contains("10")
                        cbKelas11.isChecked = kelasStr.contains("11")
                        cbKelas12.isChecked = kelasStr.contains("12")

                        
                        if (namaSholat.equals("Dhuha", ignoreCase = true)) {
                            tilJurusan.visibility = View.GONE
                            tvJurusanLabel.visibility = View.GONE
                            
                            actvJurusan.setText("Semua Jurusan", false)
                            actvHari.setText("Semua Hari", false)
                            tvTitle.text = "Edit Jadwal Sholat $namaSholat"
                        } else if (jurusan.isNotEmpty()) {
                            tvTitle.text = "Edit Jadwal Sholat $namaSholat - $jurusan"
                        }

                        
                        originalHari = hari
                        originalJurusan = jurusan
                        originalJamMulai = jadwal.jam_mulai
                        originalJamSelesai = jadwal.jam_selesai
                        originalKelas = kelasStr

                        
                        updateValidationStatus()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        when (namaSholat) {
                            "Dhuha" -> {
                                etJamMulai.setText("06:30")
                                etJamSelesai.setText("09:00")
                                originalKelas = "{10,11,12}" 
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

        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        
        btnSave.setOnClickListener {
            val updatedNama = namaSholat
            val updatedJamMulai = etJamMulai.text.toString().trim()
            val updatedJamSelesai = etJamSelesai.text.toString().trim()
            val updatedHari = actvHari.text.toString().trim()
            val updatedJurusan = actvJurusan.text.toString().trim()

            
            val selectedKelas = mutableListOf<String>()
            if (cbKelas10.isChecked) selectedKelas.add("10")
            if (cbKelas11.isChecked) selectedKelas.add("11")
            if (cbKelas12.isChecked) selectedKelas.add("12")
            val updatedKelas = selectedKelas.joinToString(", ")

            
            if (updatedJamMulai.isEmpty() || updatedJamSelesai.isEmpty() || updatedKelas.isEmpty()) {
                val errorMsg = if (updatedKelas.isEmpty()) "Harap pilih minimal satu kelas" else "Harap isi semua field wajib"
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            
            if (!isValidTimeFormat(updatedJamMulai) || !isValidTimeFormat(updatedJamSelesai)) {
                Toast.makeText(this, "Format waktu tidak valid (HH:mm atau HH:mm:ss)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            
            if (!isEndTimeAfterStart(updatedJamMulai, updatedJamSelesai)) {
                Toast.makeText(this, "Waktu selesai harus lebih besar dari waktu mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            
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
                    
                    saveJadwal(
                        dialog = dialog,
                        jadwalId = currentJadwalId,
                        token = token,
                        updatedNama = updatedNama,
                        updatedJamMulai = updatedJamMulai,
                        updatedJamSelesai = updatedJamSelesai,
                        updatedHari = if (namaSholat.equals("Dhuha", ignoreCase = true)) "Semua Hari" else updatedHari,
                        updatedJurusan = if (namaSholat.equals("Dhuha", ignoreCase = true)) "Semua Jurusan" else updatedJurusan,
                        updatedKelas = updatedKelas
                    )
                }
            )
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

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

    private fun saveJadwal(
        dialog: AlertDialog,
        jadwalId: Int,
        token: String,
        updatedNama: String,
        updatedJamMulai: String,
        updatedJamSelesai: String,
        updatedHari: String,
        updatedJurusan: String,
        updatedKelas: String
    ) {
        
        Toast.makeText(this, "Menyimpan perubahan...", Toast.LENGTH_SHORT).show()

        if (updatedHari == "Semua Hari" || updatedJurusan == "Semua Jurusan") {
            
            lifecycleScope.launch {
                var successCount = 0
                var failCount = 0

                val itemsToUpdate = if (updatedJurusan == "Semua Jurusan") {
                    
                    jadwalList.filter {
                        it.jenis_sholat.equals(updatedNama, ignoreCase = true) &&
                                (if (updatedHari == "Semua Hari") true else it.hari.equals(updatedHari, ignoreCase = true))
                    }
                } else {
                    
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
                        jurusan = if (updatedJurusan == "Semua Jurusan") item.jurusan else updatedJurusan.ifEmpty { null },
                        kelas = updatedKelas.ifEmpty { null }
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
            
            val request = JadwalSholatUpdateRequest(
                jenis_sholat = updatedNama,
                jam_mulai = updatedJamMulai,
                jam_selesai = updatedJamSelesai,
                hari = updatedHari.ifEmpty { null },
                jurusan = updatedJurusan.ifEmpty { null },
                kelas = updatedKelas.ifEmpty { null }
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
                        runOnUiThread {
                            val errorMessage = "❌ Gagal menyimpan: ${error.message}"
                            Toast.makeText(this@JadwalSholatAdminActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }

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

    private fun showTambahJadwalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tambah_jadwal, null)
        
        val actvJenisSholat = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJenisSholat)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val cbKelas10 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas10)
        val cbKelas11 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas11)
        val cbKelas12 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas12)
        val etJamMulai = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etJamMulai)
        val etJamSelesai = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etJamSelesai)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatal)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpan)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        
        val jenisSholatOptions = listOf("Dhuha", "Dzuhur", "Jumat")
        actvJenisSholat.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jenisSholatOptions))
        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, daysOptions))
        actvJurusan.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jurusanOptions))

        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val jenisSholat = actvJenisSholat.text.toString().trim()
            val hari = actvHari.text.toString().trim()
            val jurusan = actvJurusan.text.toString().trim()
            val jamMulai = etJamMulai.text.toString().trim()
            val jamSelesai = etJamSelesai.text.toString().trim()

            
            val selectedKelas = mutableListOf<String>()
            if (cbKelas10.isChecked) selectedKelas.add("10")
            if (cbKelas11.isChecked) selectedKelas.add("11")
            if (cbKelas12.isChecked) selectedKelas.add("12")
            val kelasStr = selectedKelas.joinToString(", ")

            
            if (jenisSholat.isEmpty()) {
                Toast.makeText(this, "Pilih jenis sholat", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (hari.isEmpty() || hari == "Semua Hari") {
                Toast.makeText(this, "Pilih hari yang spesifik", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (jamMulai.isEmpty() || jamSelesai.isEmpty()) {
                Toast.makeText(this, "Isi jam mulai dan selesai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (kelasStr.isEmpty()) {
                Toast.makeText(this, "Pilih minimal satu kelas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isValidTimeFormat(jamMulai) || !isValidTimeFormat(jamSelesai)) {
                Toast.makeText(this, "Format waktu tidak valid (HH:mm)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            
            val request = com.xirpl2.SASMobile.model.JadwalSholatCreateRequest(
                jenis_sholat = jenisSholat,
                jam_mulai = jamMulai,
                jam_selesai = jamSelesai,
                hari = hari,
                jurusan = if (jurusan == "Semua Jurusan") null else jurusan,
                kelas = kelasStr
            )

            
            btnSimpan.isEnabled = false
            btnSimpan.text = "MENYIMPAN..."

            val token = getAuthToken()
            lifecycleScope.launch {
                repository.createJadwalSholat(token, request).fold(
                    onSuccess = {
                        runOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadJadwalList()
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                            btnSimpan.isEnabled = true
                            btnSimpan.text = "SIMPAN JADWAL"
                        }
                    }
                )
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showEditDhuhaKeahlianDialog(item: com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian) {
        showDhuhaKeahlianDialog(item)
    }

    private fun showTambahDhuhaKeahlianDialog() {
        showDhuhaKeahlianDialog(null)
    }

    private fun showDhuhaKeahlianDialog(item: com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dhuha_keahlian, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val tvKeahlian1 = dialogView.findViewById<TextView>(R.id.tvKeahlian1)
        val tvKeahlian2 = dialogView.findViewById<TextView>(R.id.tvKeahlian2)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)

        tvTitle.text = if (item == null) "Tambah Jadwal Dhuha" else "Edit Jadwal Dhuha"

        val selectedKeahlian1 = item?.keahlian1?.toMutableList() ?: mutableListOf()
        val selectedKeahlian2 = item?.keahlian2?.toMutableList() ?: mutableListOf()

        tvKeahlian1.text = selectedKeahlian1.joinToString(", ").ifEmpty { "Pilih Keahlian 1" }
        tvKeahlian2.text = selectedKeahlian2.joinToString(", ").ifEmpty { "Pilih Keahlian 2" }

        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days))
        if (item != null) {
            actvHari.setText(item.hari, false)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        tvKeahlian1.setOnClickListener {
            showMultiSelectDialog("Pilih Keahlian 1", selectedKeahlian1) {
                tvKeahlian1.text = selectedKeahlian1.joinToString(", ").ifEmpty { "Pilih Keahlian 1" }
            }
        }

        tvKeahlian2.setOnClickListener {
            showMultiSelectDialog("Pilih Keahlian 2", selectedKeahlian2) {
                tvKeahlian2.text = selectedKeahlian2.joinToString(", ").ifEmpty { "Pilih Keahlian 2" }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val hari = actvHari.text.toString()
            if (hari.isEmpty()) {
                Toast.makeText(this, "Pilih hari", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian(
                id = item?.id,
                hari = hari,
                keahlian1 = selectedKeahlian1,
                keahlian2 = selectedKeahlian2
            )

            // COMMENTED OUT: Phantom API methods (not in backend)
            /*
            lifecycleScope.launch {
                val token = getAuthToken()
                val result = if (item == null) {
                    repository.createJadwalDhuhaKeahlian(token, request)
                } else {
                    repository.updateJadwalDhuhaKeahlian(token, item.id!!, request)
                }

                runOnUiThread {
                    if (result.isSuccess) {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Berhasil menyimpan", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadSynchronizedData()
                    } else {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            */
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showMultiSelectDialog(title: String, selectedItems: MutableList<String>, onResult: () -> Unit) {
        val checkedItems = BooleanArray(allKeahlian.size) { i ->
            selectedItems.contains(allKeahlian[i])
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMultiChoiceItems(allKeahlian, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    if (!selectedItems.contains(allKeahlian[which])) {
                        selectedItems.add(allKeahlian[which])
                    }
                } else {
                    selectedItems.remove(allKeahlian[which])
                }
            }
            .setPositiveButton("OK") { _, _ -> onResult() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditSholatDhuhaCardDialog(id: Int) {
        // COMMENTED OUT: Phantom API method (not in backend)
        /*
        lifecycleScope.launch {
            val token = getAuthToken()
            repository.getSholatDhuhaDetail(token).onSuccess { detail ->
                runOnUiThread {
                    showEditSholatCardDialog(detail.id, "Dhuha", detail.hari, detail.waktuMulai, detail.waktuSelesai, detail.kelas, null)
                }
            }
        }
        */
    }

    private fun showEditSholatDzuhurCardDialog(id: Int) {
        // COMMENTED OUT: Phantom API method (not in backend)
        /*
        lifecycleScope.launch {
            val token = getAuthToken()
            repository.getSholatDzuhurDetail(token).onSuccess { detail ->
                runOnUiThread {
                    showEditSholatCardDialog(detail.id, "Dzuhur", detail.hari, detail.waktuMulai, detail.waktuSelesai, detail.kelas, detail.jurusan)
                }
            }
        }
        */
    }

    private fun showEditSholatCardDialog(
        id: Int,
        jenis: String,
        hari: String,
        mulai: String,
        selesai: String,
        kelas: String,
        jurusan: String?
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_jadwal_sholat, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etJamMulai = dialogView.findViewById<TextInputEditText>(R.id.etJamMulai)
        val etJamSelesai = dialogView.findViewById<TextInputEditText>(R.id.etJamSelesai)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val tilJurusan = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilJurusan)
        val cbKelas10 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas10)
        val cbKelas11 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas11)
        val cbKelas12 = dialogView.findViewById<android.widget.CheckBox>(R.id.cbKelas12)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvTitle.text = "Edit Sholat $jenis"
        etJamMulai.setText(mulai)
        etJamSelesai.setText(selesai)
        
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days))
        actvHari.setText(hari, false)

        if (jurusan != null) {
            tilJurusan.visibility = View.VISIBLE
            actvJurusan.setText(jurusan)
        } else {
            tilJurusan.visibility = View.GONE
        }

        cbKelas10.isChecked = kelas.contains("10")
        cbKelas11.isChecked = kelas.contains("11")
        cbKelas12.isChecked = kelas.contains("12")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val updatedMulai = etJamMulai.text.toString()
            val updatedSelesai = etJamSelesai.text.toString()
            val updatedHari = actvHari.text.toString()
            
            val selectedKelas = mutableListOf<String>()
            if (cbKelas10.isChecked) selectedKelas.add("Kelas X")
            if (cbKelas11.isChecked) selectedKelas.add("Kelas XI")
            if (cbKelas12.isChecked) selectedKelas.add("Kelas XII")
            if (selectedKelas.size == 3) {
                selectedKelas.clear()
                selectedKelas.add("Semua Kelas")
            }
            val updatedKelas = selectedKelas.joinToString(", ")

            val body = com.google.gson.JsonObject().apply {
                addProperty("hari", updatedHari)
                addProperty("waktu_mulai", updatedMulai)
                addProperty("waktu_selesai", updatedSelesai)
                addProperty("kelas", updatedKelas)
                if (jurusan != null) {
                    addProperty("jurusan", actvJurusan.text.toString())
                }
            }

            // COMMENTED OUT: Phantom API methods updateSholatDhuha/updateSholatDzuhur (not in backend)
            /*
            lifecycleScope.launch {
                val token = getAuthToken()
                val result = if (jenis == "Dhuha") {
                    repository.updateSholatDhuha(token, id, body)
                } else {
                    repository.updateSholatDzuhur(token, id, body)
                }

                runOnUiThread {
                    if (result.isSuccess) {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadSynchronizedData()
                    } else {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Gagal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            */
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

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
