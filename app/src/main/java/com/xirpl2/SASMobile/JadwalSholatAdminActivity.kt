package com.xirpl2.SASMobile

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.adapter.PrayerScheduleAdapter
import com.xirpl2.SASMobile.adapter.PrayerScheduleItem
import com.xirpl2.SASMobile.model.JadwalDuhaKeahlian
import com.xirpl2.SASMobile.model.JadwalSholatCreateRequest
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.JurusanItem
import com.xirpl2.SASMobile.model.JadwalSholatUpdateRequest
import com.xirpl2.SASMobile.model.JenisSholatData
import com.xirpl2.SASMobile.model.PrayerTime
import com.xirpl2.SASMobile.model.PrayerType
import com.xirpl2.SASMobile.model.PrayerTypeRequest
import com.xirpl2.SASMobile.model.PrayerTimeRequest
import com.xirpl2.SASMobile.model.SholatDuhaDetail
import com.xirpl2.SASMobile.model.SholatZuhurDetail
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
    private var DuhaKeahlianList: List<com.xirpl2.SASMobile.model.JadwalDuhaKeahlian> = emptyList()
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
        rvPrayerSchedules.isNestedScrollingEnabled = true

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
            val deferredDuha = async { repository.getJadwalDuhaKeahlian(token) }
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
            deferredDuha.await().fold(
                onSuccess = { list -> DuhaKeahlianList = list },
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
        if (jadwal == null) {
            val matchingType = prayerTypesList.find { it.nama_jenis.equals(jenisSholat, ignoreCase = true) }
            if (matchingType != null) {
                jadwal = jadwalList.find {
                    it.idWaktu != null && prayerTimesList.any { pt ->
                        pt.id == it.idWaktu && pt.id_jenis_sholat == matchingType.id
                    }
                }
            }
        }
        return jadwal?.id
    }

    private fun updateJadwalUI() {
        // Enrich jadwalList with waktuSholat data from prayerTimesList/prayerTypesList (like desktop)
        val typeMap = prayerTypesList.associateBy { it.id }
        val timeByIdWaktu = prayerTimesList.associateBy { it.id }

        jadwalList = jadwalList.map { jadwal ->
            if (jadwal.waktuSholat == null && jadwal.idWaktu != null) {
                // Match by id_waktu
                val matchingTime = timeByIdWaktu[jadwal.idWaktu]
                if (matchingTime != null) {
                    val jenisData = typeMap[matchingTime.id_jenis_sholat]
                    jadwal.copy(
                        waktuSholat = WaktuSholatData(
                            waktuMulai = matchingTime.waktu_mulai,
                            waktuSelesai = matchingTime.waktu_selesai,
                            jenisSholat = JenisSholatData(namaJenis = jenisData?.nama_jenis)
                        )
                    )
                } else jadwal
            } else jadwal
        }

        // Build maps for enrichment
        val items = mutableListOf<PrayerScheduleItem>()

        // Add Duha Keahlian table first
        items.add(PrayerScheduleItem.DuhaKeahlian(DuhaKeahlianList))

        val genericPrayers = jadwalList.filter { it.jurusan.isNullOrEmpty() }

        // 2. Prayer times map: id_jenis -> PrayerTime (for time enrichment)
        val timeByTypeId = prayerTimesList.associateBy { it.id_jenis_sholat }

        // 3. Prayer types map: id -> PrayerType (for names)
        val typeById = prayerTypesList.associateBy { it.id }

        // Preferred display order
        val displayOrder = listOf("Duha", "Zuhur", "jumat")
        val addedTypes = mutableSetOf<String>()

        fun matchesTypeKey(namaJenis: String, typeKey: String): Boolean {
            val lower = namaJenis.lowercase()
            return when (typeKey) {
                "Duha" -> lower == "Duha" || lower == "duha"
                "Zuhur" -> lower == "Zuhur" || lower == "zuhur"
                else -> lower == typeKey
            }
        }

        // Build cards only from actual jadwal schedules (not prayer type placeholders)
        for (typeKey in displayOrder) {
            val existingJadwal = jadwalList.find {
                matchesTypeKey(it.jenis_sholat, typeKey) && it.jurusan.isNullOrEmpty()
            }
            if (existingJadwal != null) {
                items.add(PrayerScheduleItem.PrayerCard(existingJadwal))
                addedTypes.add(existingJadwal.jenis_sholat.lowercase())
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
                val fallbackWaktu = if (pt == null) {
                    jadwalList.find {
                        it.jenis_sholat.equals(type.nama_jenis, ignoreCase = true) &&
                            it.waktuSholat?.waktuMulai?.isNotEmpty() == true
                    }?.waktuSholat
                } else null
                val placeholder = JadwalSholatData(
                    id = 0,
                    waktuSholat = WaktuSholatData(
                        waktuMulai = pt?.waktu_mulai ?: fallbackWaktu?.waktuMulai ?: "",
                        waktuSelesai = pt?.waktu_selesai ?: fallbackWaktu?.waktuSelesai ?: "",
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
                onDuhaKeahlianSwap = { row1, col1, row2, col2 ->
                    handleSwap(row1, col1, row2, col2)
                    prayerAdapter?.getDuhaAdapter()?.submitList(DuhaKeahlianList)
                },
                onSaveDuhaKeahlian = { saveDuhaKeahlianToApi() }
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

    private fun saveDuhaKeahlianToApi() {
        val updates = mutableListOf<Pair<Int, String>>()
        DuhaKeahlianList.forEach { daySchedule ->
            val day = daySchedule.hari
            daySchedule.jurusan1?.id_jurusan?.let { updates.add(it to day) }
            daySchedule.jurusan2?.id_jurusan?.let { updates.add(it to day) }
        }

        lifecycleScope.launch {
            val token = getAuthToken()
            var hasError = false
            updates.forEach { (id, day) ->
                val res = repository.updateJurusanDuhaDay(token, id, day)
                if (res.isFailure) hasError = true
            }
            safeRunOnUiThread {
                if (hasError) {
                    Toast.makeText(this@JadwalSholatAdminActivity, "Beberapa perubahan gagal disimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@JadwalSholatAdminActivity, "Jadwal Duha berhasil disimpan", Toast.LENGTH_SHORT).show()
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
        val list = DuhaKeahlianList.toMutableList()

        // Bounds check to prevent IndexOutOfBoundsException
        if (list.isEmpty()) {
            android.util.Log.w(TAG, "handleSwap called on empty DuhaKeahlianList")
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

        DuhaKeahlianList = list
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
        if (jadwal.id == 0) {
            val jenisSholat = jadwal.jenis_sholat
            val prayerType = prayerTypesList.find {
                it.nama_jenis.equals(jenisSholat, ignoreCase = true)
            }
            if (prayerType == null) {
                Toast.makeText(this, "Tidak dapat menemukan tipe sholat '$jenisSholat'", Toast.LENGTH_SHORT).show()
                return
            }
            AlertDialog.Builder(this)
                .setTitle("Hapus $jenisSholat")
                .setMessage("Apakah Anda yakin ingin menghapus tipe sholat '$jenisSholat'? Semua jadwal dan presensi terkait akan dihapus.")
                .setPositiveButton("Hapus") { _, _ -> deletePrayerTypeById(prayerType.id) }
                .setNegativeButton("Batal", null)
                .show()
            return
        }

        val nama = "Salat ${jadwal.jenis_sholat}"
        val allSchedules = jadwalList.filter {
            it.jenis_sholat.equals(jadwal.jenis_sholat, ignoreCase = true)
        }
        val count = allSchedules.size
        val detail = buildString {
            append("Semua jadwal ${jadwal.jenis_sholat} akan dihapus ($count jadwal)")
            append("\n\nTermasuk:")
            allSchedules.forEach { s ->
                val info = mutableListOf<String>()
                s.hari?.let { info.add("Hari: $it") }
                s.jurusan?.let { info.add("Jurusan: $it") }
                info.add("Waktu: ${s.jam_mulai} - ${s.jam_selesai}")
                append("\n• ${info.joinToString(", ")}")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Hapus $nama")
            .setMessage("Apakah Anda yakin ingin menghapus $nama?\n$detail")
            .setPositiveButton("Hapus") { _, _ -> deleteAllSchedulesForType(jadwal.jenis_sholat) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAllSchedulesForType(jenisSholat: String) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        val schedulesToDelete = jadwalList.filter {
            it.jenis_sholat.equals(jenisSholat, ignoreCase = true) && it.id > 0
        }

        if (schedulesToDelete.isEmpty()) return

        jadwalList = jadwalList.filter {
            !(it.jenis_sholat.equals(jenisSholat, ignoreCase = true) && it.id > 0)
        }
        updateJadwalUI()

        lifecycleScope.launch {
            var successCount = 0
            var failCount = 0
            val errors = mutableListOf<String>()

            for (jadwal in schedulesToDelete) {
                repository.deleteJadwalSholat(token, jadwal.id).fold(
                    onSuccess = { successCount++ },
                    onFailure = { e ->
                        failCount++
                        errors.add("ID ${jadwal.id}: ${e.message}")
                    }
                )
            }

            if (!isFinishing && !isDestroyed) {
                safeRunOnUiThread {
                    if (failCount == 0) {
                        Toast.makeText(this@JadwalSholatAdminActivity, "$jenisSholat berhasil dihapus ($successCount jadwal)", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorText = errors.joinToString("\n")
                        Toast.makeText(this@JadwalSholatAdminActivity, "$successCount berhasil, $failCount gagal\n$errorText", Toast.LENGTH_LONG).show()
                        loadJadwalList()
                    }
                }
            }
        }
    }

    private fun deletePrayerTypeById(prayerTypeId: Int) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        val deletedType = prayerTypesList.find { it.id == prayerTypeId }

        jadwalList = jadwalList.filter {
            val matchingType = prayerTypesList.find { pt -> pt.id == prayerTypeId }
            matchingType == null || !it.jenis_sholat.equals(matchingType.nama_jenis, ignoreCase = true)
        }
        prayerTypesList = prayerTypesList.filter { it.id != prayerTypeId }
        updateJadwalUI()

        lifecycleScope.launch {
            repository.deletePrayerType(token, prayerTypeId).fold(
                onSuccess = {
                    if (!isFinishing && !isDestroyed) {
                        safeRunOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Tipe sholat berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFailure = { e ->
                    if (!isFinishing && !isDestroyed) {
                        safeRunOnUiThread {
                            Toast.makeText(this@JadwalSholatAdminActivity, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                            loadJadwalList()
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
            val matchingType = prayerTypesList.find { it.nama_jenis.equals(jenisSholat, ignoreCase = true) }
            val pt = matchingType?.let { t -> prayerTimesList.find { it.id_jenis_sholat == t.id } }
            if (pt != null) {
                showEditDialog(pt.id, jenisSholat)
            } else {
                Toast.makeText(this, "Jadwal $jenisSholat tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkDuplicateJurusan(selectedJurusan: String, selectedHari: String, currentJadwalId: Int): String? {
        val existing = jadwalList.find {
            it.jenis_sholat.equals("Duha", ignoreCase = true) &&
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
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        
        val validationContainer = dialogView.findViewById<LinearLayout>(R.id.validationStatusContainer)
        val ivValidationIcon = dialogView.findViewById<ImageView>(R.id.ivValidationIcon)
        val tvValidationStatus = dialogView.findViewById<TextView>(R.id.tvValidationStatus)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        
        var originalJamMulai = ""
        var originalJamSelesai = ""
        var originalJurusan = ""

        tvTitle.text = "Edit $namaSholat"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Jurusan dropdown
        val jurusans = listOf("Semua Jurusan") + JurusanHelper.getAllJurusan(this).map { it.nama }
        actvJurusan.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jurusans))

        fun updateValidationStatus() {
            val selectedJurusan = actvJurusan.text.toString().trim()
            if (selectedJurusan.isEmpty() || namaSholat.equals("Duha", ignoreCase = true)) {
                validationContainer.visibility = View.GONE
                return
            }
            val duplicateDay = checkDuplicateJurusan(selectedJurusan, "", currentJadwalId)
            validationContainer.visibility = View.VISIBLE
            if (duplicateDay != null) {
                ivValidationIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_warning))
                tvValidationStatus.text = "Jurusan $selectedJurusan sudah ada!"
                tvValidationStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
            } else {
                ivValidationIcon.setImageResource(android.R.drawable.ic_menu_send)
                ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_success))
                tvValidationStatus.text = "Jurusan tersedia"
                tvValidationStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            }
        }

        actvJurusan.setOnItemClickListener { _, _, _, _ -> updateValidationStatus() }

        // Load existing data
        lifecycleScope.launch {
            repository.getJadwalSholatById(token, currentJadwalId).fold(
                onSuccess = { jadwal ->
                    safeRunOnUiThread {
                        etJamMulai.setText(jadwal.jam_mulai)
                        etJamSelesai.setText(jadwal.jam_selesai)
                        
                        val jurusan = jadwal.jurusan ?: ""
                        actvJurusan.setText(jurusan, false)
                        
                        originalJamMulai = jadwal.jam_mulai
                        originalJamSelesai = jadwal.jam_selesai
                        originalJurusan = jurusan

                        updateValidationStatus()
                    }
                },
                onFailure = { error ->
                    safeRunOnUiThread {
                        when (namaSholat.lowercase()) {
                            "Duha" -> { etJamMulai.setText("06:30"); etJamSelesai.setText("09:00") }
                            "Zuhur" -> { etJamMulai.setText("11:30"); etJamSelesai.setText("13:00") }
                            "jumat" -> { etJamMulai.setText("11:00"); etJamSelesai.setText("13:00") }
                        }
                    }
                }
            )
        }

        etJamMulai.setOnClickListener { showTimePicker(etJamMulai) }
        etJamSelesai.setOnClickListener { showTimePicker(etJamSelesai) }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val updatedJamMulai = etJamMulai.text.toString().trim()
            val updatedJamSelesai = etJamSelesai.text.toString().trim()
            val updatedJurusan = actvJurusan.text.toString().trim()

            if (updatedJamMulai.isEmpty() || updatedJamSelesai.isEmpty()) {
                Toast.makeText(this, "Harap isi waktu mulai dan selesai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidTimeFormat(updatedJamMulai) || !isValidTimeFormat(updatedJamSelesai)) {
                Toast.makeText(this, "Format waktu tidak valid (HH:mm)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isEndTimeAfterStart(updatedJamMulai, updatedJamSelesai)) {
                Toast.makeText(this, "Waktu selesai harus lebih besar dari waktu mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "MENYIMPAN..."

            // Find all schedules for this prayer type and update them
            val schedulesToUpdate = jadwalList.filter {
                it.jenis_sholat.equals(namaSholat, ignoreCase = true) && it.id > 0
            }

            showConfirmationDialog(
                originalHari = "",
                newHari = "",
                originalJurusan = originalJurusan,
                newJurusan = updatedJurusan,
                originalWaktu = "$originalJamMulai - $originalJamSelesai",
                newWaktu = "$updatedJamMulai - $updatedJamSelesai",
                duplicateDay = null,
                onConfirm = {
                    lifecycleScope.launch {
                        var successCount = 0
                        var failCount = 0

                        for (jadwal in schedulesToUpdate) {
                            val request = JadwalSholatUpdateRequest(
                                waktu_mulai = updatedJamMulai,
                                waktu_selesai = updatedJamSelesai
                            )
                            repository.updateJadwalSholat(token, jadwal.id, request).fold(
                                onSuccess = { successCount++ },
                                onFailure = { failCount++ }
                            )
                        }

                        if (!isFinishing && !isDestroyed) {
                            safeRunOnUiThread {
                                showLoading(false)
                                if (failCount == 0) {
                                    Toast.makeText(this@JadwalSholatAdminActivity, "$namaSholat berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@JadwalSholatAdminActivity, "$successCount berhasil, $failCount gagal", Toast.LENGTH_LONG).show()
                                }
                                dialog.dismiss()
                                loadJadwalList()
                            }
                        }
                    }
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
        val rgTipeJadwal = dialogView.findViewById<RadioGroup>(R.id.rgTipeJadwal)
        val rbBerulang = dialogView.findViewById<RadioButton>(R.id.rbBerulang)
        val rbTanggalKhusus = dialogView.findViewById<RadioButton>(R.id.rbTanggalKhusus)
        val sectionHari = dialogView.findViewById<LinearLayout>(R.id.sectionHari)
        val sectionTanggalKhusus = dialogView.findViewById<LinearLayout>(R.id.sectionTanggalKhusus)
        val etTanggalKhusus = dialogView.findViewById<TextInputEditText>(R.id.etTanggalKhusus)
        val chipGroupJurusan = dialogView.findViewById<ChipGroup>(R.id.chipGroupJurusan)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatal)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        val dayButtons = mapOf(
            "Senin" to dialogView.findViewById<MaterialButton>(R.id.btnSenin),
            "Selasa" to dialogView.findViewById<MaterialButton>(R.id.btnSelasa),
            "Rabu" to dialogView.findViewById<MaterialButton>(R.id.btnRabu),
            "Kamis" to dialogView.findViewById<MaterialButton>(R.id.btnKamis),
            "Jumat" to dialogView.findViewById<MaterialButton>(R.id.btnJumat),
            "Sabtu" to dialogView.findViewById<MaterialButton>(R.id.btnSabtu)
        )
        val selectedDays = mutableSetOf<String>()

        fun toggleDayButton(day: String) {
            val btn = dayButtons[day] ?: return
            if (selectedDays.contains(day)) {
                selectedDays.remove(day)
                btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                btn.setTextColor(ContextCompat.getColor(this, R.color.on_background))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.divider)
            } else {
                selectedDays.add(day)
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_theme))
                btn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.blue_theme)
            }
        }

        val jurusanData = mutableListOf<JurusanItem>()
        val selectedJurusanNames = mutableSetOf<String>()

        fun addJurusanChip(nama: String) {
            val chip = Chip(this).apply {
                text = nama
                isCheckable = true
                isChecked = false
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedJurusanNames.add(nama) else selectedJurusanNames.remove(nama)
                }
            }
            chipGroupJurusan.addView(chip)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        etWaktuMulai.setOnClickListener { showTimePickerEx { time -> etWaktuMulai.setText(time) } }
        etWaktuSelesai.setOnClickListener { showTimePickerEx { time -> etWaktuSelesai.setText(time) } }

        rgTipeJadwal.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbBerulang) {
                sectionHari.visibility = View.VISIBLE
                sectionTanggalKhusus.visibility = View.GONE
            } else {
                sectionHari.visibility = View.GONE
                sectionTanggalKhusus.visibility = View.VISIBLE
            }
        }

        dayButtons.forEach { (day, btn) ->
            btn.setOnClickListener { toggleDayButton(day) }
        }

        etTanggalKhusus.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pilih Tanggal Khusus")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                etTanggalKhusus.setText(sdf.format(java.util.Date(millis)))
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }

        val token = getAuthToken()
        lifecycleScope.launch {
            val jurusanDeferred = async { repository.getJurusanLookup(token) }

            jurusanDeferred.await().onSuccess { list ->
                jurusanData.clear()
                jurusanData.addAll(list)
                val sorted = list.map { it.nama }.sorted()
                runOnUiThread {
                    sorted.forEach { nama -> addJurusanChip(nama) }
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            tvError.visibility = View.GONE
            val namaSholat = etNamaSholat.text.toString().trim()
            val waktuMulai = etWaktuMulai.text.toString().trim()
            val waktuSelesai = etWaktuSelesai.text.toString().trim()
            val isTanggalKhusus = rbTanggalKhusus.isChecked
            val tanggalKhusus = etTanggalKhusus.text.toString().trim()

            val errors = mutableListOf<String>()
            if (namaSholat.isEmpty()) errors.add("Nama sholat")
            if (waktuMulai.isEmpty()) errors.add("Jam mulai")
            if (waktuSelesai.isEmpty()) errors.add("Jam selesai")
            if (isTanggalKhusus && tanggalKhusus.isEmpty()) errors.add("Tanggal khusus")
            if (!isTanggalKhusus && selectedDays.isEmpty()) errors.add("Minimal satu hari")

            if (errors.isNotEmpty()) {
                tvError.text = "Harap isi: ${errors.joinToString(", ")}"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            btnSimpan.text = "MEMPROSES..."

            val daysToCreate = if (isTanggalKhusus) emptyList() else selectedDays.toList()

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

                    val jurusanIds = if (selectedJurusanNames.isEmpty()) {
                        null
                    } else {
                        jurusanData
                            .filter { selectedJurusanNames.contains(it.nama) }
                            .map { it.id }
                            .ifEmpty { null }
                    }

                    var lastError: Throwable? = null
                    if (isTanggalKhusus) {
                        val req = JadwalSholatCreateRequest(
                            hari = tanggalKhusus ?: "",
                            id_waktu = idWaktu,
                            jurusan_ids = jurusanIds
                        )
                        val scheduleResult = repository.createJadwalSholat(token, req)
                        if (scheduleResult.isFailure) {
                            lastError = scheduleResult.exceptionOrNull()
                        }
                    } else {
                        for (d in daysToCreate) {
                            val req = JadwalSholatCreateRequest(
                                hari = d,
                                id_waktu = idWaktu,
                                jurusan_ids = jurusanIds
                            )
                            val scheduleResult = repository.createJadwalSholat(token, req)
                            if (scheduleResult.isFailure) {
                                lastError = scheduleResult.exceptionOrNull()
                            }
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
                            tvError.text = msg
                            tvError.visibility = View.VISIBLE
                            btnSimpan.isEnabled = true
                            btnSimpan.text = "SIMPAN JADWAL"
                        }
                    )
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showTimePickerEx(onTimeSet: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            onTimeSet(String.format("%02d:%02d", hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
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
