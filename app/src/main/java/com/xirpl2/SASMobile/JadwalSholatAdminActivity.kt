package com.xirpl2.SASMobile

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.adapter.PrayerScheduleAdapter
import com.xirpl2.SASMobile.adapter.PrayerScheduleItem
import com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian
import com.xirpl2.SASMobile.model.JadwalSholatCreateRequest
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.JurusanItem
import com.xirpl2.SASMobile.model.JadwalSholatUpdateRequest
import com.xirpl2.SASMobile.model.JenisSholatData
import com.xirpl2.SASMobile.model.PrayerTime
import com.xirpl2.SASMobile.model.PrayerType
import com.xirpl2.SASMobile.model.PrayerTypeRequest
import com.xirpl2.SASMobile.model.PrayerTimeRequest
import com.xirpl2.SASMobile.model.SholatDhuhaDetail
import com.xirpl2.SASMobile.model.SholatDzuhurDetail
import com.xirpl2.SASMobile.model.WaktuSholatData
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar

class JadwalSholatAdminActivity : BaseAdminActivity() {

    private val TAG = "JadwalSholatAdminActivity"
    private val repository = BerandaRepository()

    
    private var jadwalList: List<JadwalSholatData> = emptyList()
    private var dhuhaKeahlianList: List<com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian> = emptyList()
    private var prayerTimesList: List<PrayerTime> = emptyList()
    private var prayerTypesList: List<PrayerType> = emptyList()

    private val daysOptions = listOf("Semua Hari", "Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    private var jurusanOptions = listOf("Semua Jurusan")

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.JADWAL_SHOLAT
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_jadwal_sholat_admin)
    setupStatusBar()

    val topBarContent = findViewById<View>(R.id.topBarContent)
    applyEdgeToEdge(topBarContent)

    setupDrawerAndSidebar()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.blue_theme)
        swipeRefresh.setOnRefreshListener { refreshAll() }

        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        rvPrayerSchedules = findViewById(R.id.rvPrayerSchedules)
        rvPrayerSchedules.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvPrayerSchedules.isNestedScrollingEnabled = false

        setupMenuIcon()

        setupButtons()

        showLoading(true)
        loadJadwalList()
    }

    private fun loadJadwalList() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            var hasError = false

            val deferredJadwal = async { repository.getJadwalSholat(token) }
            val deferredTimes = async { repository.getPrayerTimes(token) }
            val deferredTypes = async { repository.getPrayerTypes(token) }
            val deferredDhuha = async { repository.getJadwalDhuhaKeahlian(token) }
            val deferredJurusan = async { repository.getJurusanLookup(token) }

            deferredJadwal.await().fold(
                onSuccess = { list -> jadwalList = list },
                onFailure = { hasError = true }
            )
            deferredTimes.await().fold(
                onSuccess = { times -> prayerTimesList = times },
                onFailure = { hasError = true }
            )
            deferredTypes.await().fold(
                onSuccess = { types -> prayerTypesList = types },
                onFailure = { hasError = true }
            )
            deferredDhuha.await().fold(
                onSuccess = { list -> dhuhaKeahlianList = list },
                onFailure = { hasError = true }
            )
            deferredJurusan.await().fold(
                onSuccess = { list ->
                    jurusanOptions = listOf("Semua Jurusan") + list.map { it.nama }.sorted()
                },
                onFailure = { /* keep default */ }
            )

            if (!isFinishing && !isDestroyed) {
                showLoading(false)
                swipeRefresh.isRefreshing = false
                updateJadwalUI()

                if (hasError && jadwalList.isEmpty()) {
                    showErrorWithRetry("Gagal memuat jadwal sholat")
                }
            }
        }
    }

    override fun onDestroy() {
        if (::rvPrayerSchedules.isInitialized) {
            rvPrayerSchedules.adapter = null
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::rvPrayerSchedules.isInitialized) {
                rvPrayerSchedules.clearOnScrollListeners()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in onPause: ${e.message}")
        }
    }

    private fun safeUIUpdate(action: () -> Unit) {
        if (!isFinishing && !isDestroyed) {
            try {
                action()
            } catch (e: android.os.DeadObjectException) {
                android.util.Log.e(TAG, "DeadObjectException during UI update: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error during UI update: ${e.message}")
            }
        }
    }

    private fun safeRunOnUiThread(action: () -> Unit) {
        if (!isFinishing && !isDestroyed) {
            try {
                runOnUiThread {
                    try {
                        if (!isFinishing && !isDestroyed) {
                            action()
                        }
                    } catch (e: android.os.DeadObjectException) {
                        android.util.Log.e(TAG, "DeadObjectException in safeRunOnUiThread: ${e.message}")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error in safeRunOnUiThread: ${e.message}")
                    }
                }
            } catch (e: android.os.DeadObjectException) {
                android.util.Log.e(TAG, "DeadObjectException posting to UI thread: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error posting to UI thread: ${e.message}")
            }
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
        val items = mutableListOf<PrayerScheduleItem>()

        // Add Dhuha Keahlian table first
        items.add(PrayerScheduleItem.DhuhaKeahlian(dhuhaKeahlianList))

        // Build maps for enrichment — same approach as desktop
        // 1. Existing jadwal entries (generic, no jurusan) grouped by prayer type
        val genericPrayers = jadwalList.filter { it.jurusan.isNullOrEmpty() }
        val jadwalByType = genericPrayers.associateBy { it.jenis_sholat.lowercase() }

        // 2. Prayer times map: id_jenis -> PrayerTime (for time enrichment)
        val timeByTypeId = prayerTimesList.associateBy { it.id_jenis_sholat }

        // 3. Prayer types map: id -> PrayerType (for names)
        val typeById = prayerTypesList.associateBy { it.id }

        // Preferred display order
        val displayOrder = listOf("dhuha", "dzuhur", "jumat")
        val addedTypes = mutableSetOf<String>()

        // Build cards for ALL prayer types (matching desktop behavior)
        for (typeKey in displayOrder) {
            val existingJadwal = jadwalByType[typeKey]
            if (existingJadwal != null) {
                // Has a jadwal_sholat entry — use it directly
                items.add(PrayerScheduleItem.PrayerCard(existingJadwal))
                addedTypes.add(typeKey)
            } else {
                // No jadwal_sholat entry — create placeholder from prayer_times + prayer_types
                val matchingType = typeById.values.find { it.nama_jenis.lowercase() == typeKey }
                if (matchingType != null) {
                    val pt = timeByTypeId[matchingType.id]
                    val placeholder = JadwalSholatData(
                        id = 0,
                        hari = null,
                        jurusan = null,
                        kelas = null,
                        waktuSholat = WaktuSholatData(
                            waktuMulai = pt?.waktu_mulai ?: "",
                            waktuSelesai = pt?.waktu_selesai ?: "",
                            jenisSholat = JenisSholatData(namaJenis = matchingType.nama_jenis)
                        )
                    )
                    items.add(PrayerScheduleItem.PrayerCard(placeholder))
                    addedTypes.add(typeKey)
                }
            }
        }

        // Add remaining jadwal types not in displayOrder
        val groupedByType = genericPrayers.groupBy { it.jenis_sholat.lowercase() }
        for ((typeKey, jadwals) in groupedByType) {
            if (typeKey !in addedTypes && jadwals.isNotEmpty()) {
                items.add(PrayerScheduleItem.PrayerCard(jadwals.first()))
                addedTypes.add(typeKey)
            }
        }
        for (type in typeById.values) {
            val key = type.nama_jenis.lowercase()
            if (key !in addedTypes) {
                val pt = timeByTypeId[type.id]
                val placeholder = JadwalSholatData(
                    id = 0,
                    waktuSholat = WaktuSholatData(
                        waktuMulai = pt?.waktu_mulai ?: "",
                        waktuSelesai = pt?.waktu_selesai ?: "",
                        jenisSholat = JenisSholatData(namaJenis = type.nama_jenis)
                    )
                )
                items.add(PrayerScheduleItem.PrayerCard(placeholder))
            }
        }

        showEmptyState(items.isEmpty())

        if (prayerAdapter == null) {
            prayerAdapter = PrayerScheduleAdapter(
                items = items,
                canEdit = canUserEdit(),
                onEditPrayer = { jenisSholat -> showEditDialogByJenis(jenisSholat) },
                onDeletePrayer = { jadwal -> showDeleteConfirmation(jadwal) },
                onDhuhaKeahlianSwap = { row1, col1, row2, col2 ->
                    handleSwap(row1, col1, row2, col2)
                    prayerAdapter?.getDhuhaAdapter()?.submitList(dhuhaKeahlianList)
                },
                onSaveDhuhaKeahlian = { saveDhuhaKeahlianToApi() }
            )
            rvPrayerSchedules.adapter = prayerAdapter
        } else {
            prayerAdapter?.updateItems(items)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        rvPrayerSchedules.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        rvPrayerSchedules.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun refreshAll() {
        loadJadwalList()
        swipeRefresh.isRefreshing = false
    }

    private fun showErrorWithRetry(message: String) {
        if (isFinishing || isDestroyed) return
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Coba Lagi") { refreshAll() }
            .setActionTextColor(getColor(R.color.blue_theme))
            .show()
    }

    private fun canUserEdit(): Boolean {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        val isReadOnly = role.contains("wali") || role == "guru"
        return !isReadOnly && role.contains("admin")
    }

    private fun saveDhuhaKeahlianToApi() {
        val updates = mutableListOf<Pair<Int, String>>()
        dhuhaKeahlianList.forEach { daySchedule ->
            val day = daySchedule.hari
            daySchedule.jurusan1?.id_jurusan?.let { updates.add(it to day) }
            daySchedule.jurusan2?.id_jurusan?.let { updates.add(it to day) }
        }

        lifecycleScope.launch {
            val token = getAuthToken()
            var hasError = false
            updates.forEach { (id, day) ->
                val res = repository.updateJurusanDhuhaDay(token, id, day)
                if (res.isFailure) hasError = true
            }
            safeRunOnUiThread {
                if (hasError) {
                    Toast.makeText(this@JadwalSholatAdminActivity, "Beberapa perubahan gagal disimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal Dhuha berhasil disimpan", Toast.LENGTH_SHORT).show()
                }
                loadJadwalList()
            }
        }
    }

    private lateinit var rvPrayerSchedules: androidx.recyclerview.widget.RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private var prayerAdapter: PrayerScheduleAdapter? = null

    private fun handleSwap(row1: Int, col1: Int, row2: Int, col2: Int) {
        val list = dhuhaKeahlianList.toMutableList()

        // Bounds check to prevent IndexOutOfBoundsException
        if (list.isEmpty()) {
            android.util.Log.w(TAG, "handleSwap called on empty dhuhaKeahlianList")
            return
        }
        if (row1 < 0 || row1 >= list.size || row2 < 0 || row2 >= list.size) {
            android.util.Log.e(TAG, "handleSwap: row index out of bounds (row1=$row1, row2=$row2, size=${list.size})")
            return
        }
        if (col1 !in 1..2 || col2 !in 1..2) {
            android.util.Log.e(TAG, "handleSwap: col index out of bounds (col1=$col1, col2=$col2, expected 1 or 2)")
            return
        }

        val item1 = list[row1]
        val item2 = list[row2]

        val j1 = if (col1 == 1) item1.jurusan1 else item1.jurusan2
        val j2 = if (col2 == 1) item2.jurusan1 else item2.jurusan2

        val newItem1 = if (col1 == 1) item1.copy(jurusan1 = j2) else item1.copy(jurusan2 = j2)
        val newItem2 = if (row1 == row2) {
            if (col2 == 1) newItem1.copy(jurusan1 = j1) else newItem1.copy(jurusan2 = j1)
        } else {
            if (col2 == 1) item2.copy(jurusan1 = j1) else item2.copy(jurusan2 = j1)
        }

        list[row1] = newItem1
        if (row1 != row2) {
            list[row2] = newItem2
        }

        dhuhaKeahlianList = list
    }

    private fun setupButtons() {
        val btnTambah = findViewById<FloatingActionButton>(R.id.btnTambah)

        if (!canUserEdit()) {
            btnTambah.visibility = View.GONE
            prayerAdapter?.canEdit = false
            return
        }

        btnTambah.setOnClickListener {
            showTambahJadwalDialog()
        }
    }

    private fun showDeleteConfirmation(jadwal: JadwalSholatData) {
        val nama = "Sholat ${jadwal.jenis_sholat}"
        val detail = buildString {
            if (!jadwal.hari.isNullOrEmpty()) append("Hari: ${jadwal.hari}\n")
            if (!jadwal.jurusan.isNullOrEmpty()) append("Jurusan: ${jadwal.jurusan}\n")
            append("Waktu: ${jadwal.jam_mulai} - ${jadwal.jam_selesai}")
        }

        AlertDialog.Builder(this)
            .setTitle("Hapus Jadwal")
            .setMessage("Hapus jadwal $nama?\n\n$detail")
            .setPositiveButton("Hapus") { _, _ -> deleteJadwal(jadwal) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteJadwal(jadwal: JadwalSholatData) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        showLoading(true)

        lifecycleScope.launch {
            repository.deleteJadwalSholat(token, jadwal.id).fold(
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        safeRunOnUiThread {
                            showLoading(false)
                            Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal ${jadwal.jenis_sholat} berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadJadwalList()
                        }
                    }
                },
                onFailure = {
                    if (!isFinishing && !isDestroyed) {
                        safeRunOnUiThread {
                            showLoading(false)
                            Toast.makeText(this@JadwalSholatAdminActivity, "Gagal menghapus jadwal", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
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
                    if (!isFinishing && !isDestroyed) {
                        safeRunOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Sesi telah berakhir", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }

                repository.getJadwalSholat(token).fold(
                    onSuccess = { list ->
                        if (isFinishing || isDestroyed) return@fold
                        jadwalList = list
                        updateJadwalUI()
                        val id = list.find { it.jenis_sholat.equals(jenisSholat, ignoreCase = true) }?.id
                        safeRunOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                if (id != null) {
                                    showEditDialog(id, jenisSholat)
                                } else {
                                    Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal $jenisSholat tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        if (isFinishing || isDestroyed) return@fold
                        safeRunOnUiThread {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@JadwalSholatAdminActivity, "Gagal memuat jadwal", Toast.LENGTH_SHORT).show()
                            }
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

        
        val jurusans = listOf("Semua Jurusan") + JurusanHelper.getAllJurusan(this).map { it.nama }
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
                    ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_warning))
                    tvValidationStatus.text = "Jurusan $selectedJurusan sudah ada di hari $duplicateDay!"
                    tvValidationStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                } else {

                    ivValidationIcon.setImageResource(android.R.drawable.ic_menu_send)
                    ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_success))
                    tvValidationStatus.text = "Jurusan tersedia"
                    tvValidationStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success))
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
                    safeRunOnUiThread {
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
                    safeRunOnUiThread {
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

        etJamMulai.setOnClickListener { showTimePicker(etJamMulai) }
        etJamSelesai.setOnClickListener { showTimePicker(etJamSelesai) }
        etJamMulai.isFocusable = false
        etJamSelesai.isFocusable = false

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

            
            btnSave.isEnabled = false
            btnSave.text = "MENYIMPAN..."

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
                },
                onCancel = {
                    btnSave.isEnabled = true
                    btnSave.text = "SIMPAN"
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
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
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
                "\nPeringatan: Jurusan $newJurusan sudah ada di hari $duplicateDay.\nLanjutkan menyimpan?"
            } else ""

            "Anda akan mengubah jadwal:\n\n$changes$warningText"
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Perubahan")
            .setMessage(message)
            .setPositiveButton("Ya, Simpan") { _, _ -> onConfirm() }
            .setNegativeButton("Batal") { dialog, _ ->
                onCancel?.invoke()
                dialog.dismiss()
            }
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
        
        showLoading(true)

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
                        waktu_mulai = updatedJamMulai,
                        waktu_selesai = updatedJamSelesai,
                        hari = if (updatedHari == "Semua Hari") item.hari else updatedHari
                    )

                    repository.updateJadwalSholat(token, item.id, request).fold(
                        onSuccess = { successCount++ },
                        onFailure = { failCount++ }
                    )
                }

                if (!isFinishing && !isDestroyed) {
                    safeRunOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            showLoading(false)
                            if (failCount == 0) {
                                Toast.makeText(this@JadwalSholatAdminActivity, "Berhasil memperbarui $successCount jadwal", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@JadwalSholatAdminActivity, "$successCount berhasil, $failCount gagal", Toast.LENGTH_LONG).show()
                            }
                            dialog.dismiss()
                            loadJadwalList()
                        }
                    }
                }
            }
        } else {
            
            val request = JadwalSholatUpdateRequest(
                waktu_mulai = updatedJamMulai,
                waktu_selesai = updatedJamSelesai,
                hari = updatedHari.ifEmpty { null }
            )

            lifecycleScope.launch {
                repository.updateJadwalSholat(token, jadwalId, request).fold(
                    onSuccess = {
                        if (!isFinishing && !isDestroyed) {
                            safeRunOnUiThread {
                                if (!isFinishing && !isDestroyed) {
                                    showLoading(false)
                                    Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                    loadJadwalList()
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        if (!isFinishing && !isDestroyed) {
                            safeRunOnUiThread {
                                if (!isFinishing && !isDestroyed) {
                                    showLoading(false)
                                    Toast.makeText(this@JadwalSholatAdminActivity, "Gagal menyimpan jadwal", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun showTimePicker(target: com.google.android.material.textfield.TextInputEditText) {
        val currentText = target.text.toString()
        val parts = currentText.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        android.app.TimePickerDialog(this, { _, h, m ->
            target.setText(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
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

        val etNamaSholat = dialogView.findViewById<TextInputEditText>(R.id.etNamaSholat)
        val etWaktuMulai = dialogView.findViewById<TextInputEditText>(R.id.etWaktuMulai)
        val etWaktuSelesai = dialogView.findViewById<TextInputEditText>(R.id.etWaktuSelesai)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatal)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        etWaktuMulai.setOnClickListener { showTimePicker { time -> etWaktuMulai.setText(time) } }
        etWaktuSelesai.setOnClickListener { showTimePicker { time -> etWaktuSelesai.setText(time) } }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val namaSholat = etNamaSholat.text.toString().trim()
            val waktuMulai = etWaktuMulai.text.toString().trim()
            val waktuSelesai = etWaktuSelesai.text.toString().trim()

            if (namaSholat.isEmpty()) { etNamaSholat.error = "Nama sholat wajib diisi"; return@setOnClickListener }
            if (waktuMulai.isEmpty()) { etWaktuMulai.error = "Waktu mulai wajib diisi"; return@setOnClickListener }
            if (waktuSelesai.isEmpty()) { etWaktuSelesai.error = "Waktu selesai wajib diisi"; return@setOnClickListener }

            val message = "Nama Sholat: $namaSholat\n" +
                    "Waktu Mulai: $waktuMulai\n" +
                    "Waktu Selesai: $waktuSelesai\n\n" +
                    "Jadwal akan dibuat untuk hari Senin s/d Jumat\n" +
                    "dengan semua jurusan. Lanjutkan?"

            AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage(message)
                .setPositiveButton("Ya, Simpan") { _, _ ->
                    executeCreateSequence(dialog, btnSimpan, namaSholat, waktuMulai, waktuSelesai)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showTimePicker(onTimeSet: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            onTimeSet(String.format("%02d:%02d", hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun executeCreateSequence(
        dialog: AlertDialog,
        btnSimpan: MaterialButton,
        namaSholat: String,
        waktuMulai: String,
        waktuSelesai: String
    ) {
        btnSimpan.isEnabled = false
        btnSimpan.text = "MEMPROSES..."
        val token = getAuthToken()

        lifecycleScope.launch {
            val result = runCatching {
                
                val prayerTypesResult = repository.getPrayerTypes(token)
                val existingType = prayerTypesResult.getOrNull()?.find { it.nama_jenis == namaSholat }
                val jenisId = if (existingType != null) {
                    existingType.id
                } else {
                    val createResult = repository.createPrayerType(
                        token, PrayerTypeRequest(nama_jenis = namaSholat)
                    )
                    createResult.getOrThrow().id
                }

                
                val berlakuMulai = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val waktuResult = repository.createPrayerTime(
                    token, PrayerTimeRequest(
                        id_jenis_sholat = jenisId,
                        waktu_mulai = waktuMulai,
                        waktu_selesai = waktuSelesai,
                        berlaku_mulai = berlakuMulai
                    )
                )
                val idWaktu = waktuResult.getOrThrow().id

                
                val jurusanResult = repository.getJurusanLookup(token)
                val allJurusanIds = jurusanResult.getOrNull()?.map { it.id } ?: emptyList()

                
                val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
                var lastError: Throwable? = null
                for (hari in days) {
                    val req = JadwalSholatCreateRequest(
                        hari = hari,
                        id_waktu = idWaktu,
                        jurusan_ids = allJurusanIds.ifEmpty { null }
                    )
                    val scheduleResult = repository.createJadwalSholat(token, req)
                    if (scheduleResult.isFailure) {
                        lastError = scheduleResult.exceptionOrNull()
                    }
                }
                lastError?.let { throw it }
            }

            safeRunOnUiThread {
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadJadwalList()
                    },
                    onFailure = { error ->
                        val msg = error.message ?: "Gagal menyimpan data"
                        Toast.makeText(this@JadwalSholatAdminActivity, msg, Toast.LENGTH_LONG).show()
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "SIMPAN JADWAL"
                    }
                )
            }
        }
    }

    private fun executeCreate(
        dialog: AlertDialog,
        btnSimpan: MaterialButton,
        token: String,
        hari: String,
        idWaktu: Int,
        jurusanIds: List<Int>?
    ) {
        btnSimpan.isEnabled = false
        btnSimpan.text = "MENYIMPAN..."

        val request = JadwalSholatCreateRequest(
            hari = hari,
            id_waktu = idWaktu,
            jurusan_ids = jurusanIds
        )

        lifecycleScope.launch {
            repository.createJadwalSholat(token, request).fold(
                onSuccess = {
                    safeRunOnUiThread {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadJadwalList()
                    }
                },
                onFailure = { error ->
                    safeRunOnUiThread {
                        val msg = error.message ?: "Gagal menyimpan data"
                        Toast.makeText(this@JadwalSholatAdminActivity, msg, Toast.LENGTH_LONG).show()
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "SIMPAN JADWAL"
                    }
                }
            )
        }
    }



    private fun showEditSholatDhuhaCardDialog(id: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_jadwal_sholat, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etJamMulai = dialogView.findViewById<TextInputEditText>(R.id.etJamMulai)
        val etJamSelesai = dialogView.findViewById<TextInputEditText>(R.id.etJamSelesai)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val tilJurusan = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilJurusan)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        tvTitle.text = "Edit Waktu Dhuha Keahlian"
        tilJurusan.visibility = View.VISIBLE
        actvJurusan.setText("Semua Jurusan", false)
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Semua Hari")
        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, days))
        actvHari.setText("Semua Hari", false)

        val jurusanOptions = listOf("Semua Jurusan") + dhuhaKeahlianList.flatMap { listOfNotNull(it.jurusan1, it.jurusan2) }.map { it.nama_jurusan }
        actvJurusan.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jurusanOptions))

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        btnSave.setOnClickListener {
            val updatedMulai = etJamMulai.text.toString().trim()
            val updatedSelesai = etJamSelesai.text.toString().trim()
            val updatedHari = actvHari.text.toString().trim()
            val selectedJurusan = actvJurusan.text.toString().trim()

            if (updatedMulai.isEmpty() || updatedSelesai.isEmpty()) {
                Toast.makeText(this, "Isi waktu mulai dan selesai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "MENYIMPAN..."

            val jurusansToUpdate = if (selectedJurusan == "Semua Jurusan") {
                dhuhaKeahlianList.flatMap { listOfNotNull(it.jurusan1, it.jurusan2) }
            } else {
                dhuhaKeahlianList.flatMap { listOfNotNull(it.jurusan1, it.jurusan2) }.filter { it.nama_jurusan == selectedJurusan }
            }

            lifecycleScope.launch {
                val token = getAuthToken()
                var hasError = false
                jurusansToUpdate.forEach { jur ->
                    val h = if (updatedHari == "Semua Hari" || updatedHari.isEmpty()) jur.hari_dhuha ?: "" else updatedHari
                    val req = com.xirpl2.SASMobile.model.JadwalDhuhaTimeUpdateRequest(
                        waktu_mulai = updatedMulai,
                        waktu_selesai = updatedSelesai,
                        hari = h
                    )
                    val res = repository.updateJadwalDhuhaTime(token, jur.id_jurusan, req)
                    if (res.isFailure) hasError = true
                }
                
                safeRunOnUiThread {
                    if (hasError) {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Gagal memperbarui beberapa jadwal", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@JadwalSholatAdminActivity, "Waktu Dhuha berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadJadwalList()
                    }
                    btnSave.isEnabled = true
                    btnSave.text = "SIMPAN JADWAL"
                }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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
            if (cbKelas10.isChecked) selectedKelas.add("10")
            if (cbKelas11.isChecked) selectedKelas.add("11")
            if (cbKelas12.isChecked) selectedKelas.add("12")
            if (selectedKelas.size == 3) {
                selectedKelas.clear()
                selectedKelas.add("Semua Kelas")
            }
            val updatedKelas = selectedKelas.joinToString(", ")

            val request = JadwalSholatUpdateRequest(
                waktu_mulai = updatedMulai,
                waktu_selesai = updatedSelesai,
                hari = updatedHari.ifEmpty { null }
            )

            lifecycleScope.launch {
                val token = getAuthToken()
                repository.updateJadwalSholat(token, id, request).fold(
                    onSuccess = {
                        safeRunOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadJadwalList()
                        }
                    },
                    onFailure = { error ->
                        safeRunOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
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
