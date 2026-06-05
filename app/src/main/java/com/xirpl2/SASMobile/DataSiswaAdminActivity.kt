package com.xirpl2.SASMobile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.adapter.SiswaAdapter
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataSiswaAdminActivity : BaseAdminActivity() {

    private val TAG = "DataSiswaAdminActivity"

    private val repository = BerandaRepository()

    private val pickCsvLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            uploadCsvFile(uri)
        }
    }

    private lateinit var recyclerSiswa: RecyclerView
    private lateinit var siswaAdapter: SiswaAdapter
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var tvCountInfo: TextView
    private lateinit var tvStatTotalSiswa: TextView
    private lateinit var tvStatTotalKelas: TextView
    private lateinit var tvStatTotalJurusan: TextView

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val allStudentList = mutableListOf<SiswaItem>()

    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private var isLoading = false
    private var isLastPage = false
    private val pageSize = 100

    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedKelas: String = "Semua Kelas"
    private var selectedGender: String = "Semua JK"
    private var selectedAgama: String = "Semua Agama"
    private var searchQuery: String = ""

    // Pagination UI
    private lateinit var tvPageInfo: TextView
    private lateinit var btnPrevPage: View
    private lateinit var btnNextPage: View

    // Bulk Action views
    private lateinit var bulkActionCard: View
    private lateinit var tvSelectedCount: TextView
    private lateinit var cbSelectAll: CheckBox
    private lateinit var cbHeaderSelectAll: CheckBox
    private var isSelectionMode = false

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
    private var forcedClass: String? = null
    private var isWaliKelas: Boolean = false

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 300L

    private var loadingJob: Job? = null

    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "10", "11", "12")
    private val genderOptions: List<String> = listOf("Semua JK", "Laki-laki", "Perempuan")
    private val agamaOptions: List<String> = listOf("Semua Agama", "Islam", "Kristen", "Katolik", "Hindu", "Budha", "Khonghucu")

    private fun getGenderApiValue(displayValue: String): String? {
        return when (displayValue) {
            "Laki-laki" -> "L"
            "Perempuan" -> "P"
            else -> null
        }
    }

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.DATA_SISWA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_siswa_admin)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupButtons()
        setupSearch()
        initForcedClass()
        setupFilters()
        setupRecyclerView()
        setupBulkActionLogic()
        setupPagination()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadStudentData(reset = true) }

        loadStudentData(reset = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchHandler.removeCallbacksAndMessages(null)
        loadingJob?.cancel()
    }

    private fun initViews() {
        recyclerSiswa = findViewById(R.id.recyclerSiswa)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        emptyStateContainer = findViewById(R.id.emptyState)
        tvCountInfo = findViewById(R.id.tvCountInfo)
        tvStatTotalSiswa = findViewById(R.id.tvStatTotalSiswa)
        tvStatTotalKelas = findViewById(R.id.tvStatTotalKelas)
        tvStatTotalJurusan = findViewById(R.id.tvStatTotalJurusan)

        bulkActionCard = findViewById(R.id.bulkActionCard)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        cbHeaderSelectAll = findViewById(R.id.cbHeaderSelectAll)

        // Pagination UI
        tvPageInfo = findViewById(R.id.tvPageInfo)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)

        // Stat cards: kelas = 3 (10,11,12), jurusan = fixed list size
        tvStatTotalKelas.text = "3"
        tvStatTotalJurusan.text = fixedJurusanList.size.toString()
    }

    private fun setupBulkActionLogic() {
        siswaAdapter.setOnSelectionChangedListener { count ->
            if (count > 0 && !isSelectionMode) {
                enterSelectionMode()
            } else if (count == 0 && isSelectionMode) {
                exitSelectionMode()
            }
            tvSelectedCount.text = "$count terpilih"
            cbSelectAll.isChecked = count == allStudentList.size && allStudentList.isNotEmpty()
            cbHeaderSelectAll.isChecked = count == allStudentList.size && allStudentList.isNotEmpty()
        }

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            siswaAdapter.selectAll(isChecked)
        }

        cbHeaderSelectAll.setOnCheckedChangeListener { _, isChecked ->
            siswaAdapter.selectAll(isChecked)
        }

        findViewById<View>(R.id.btnCloseBulk)?.setOnClickListener {
            exitSelectionMode()
        }

        // TODO: btnBulkMutasi, btnBulkDelete, btnBulkDetail not yet added to layout
        // findViewById<View>(R.id.btnBulkMutasi)?.setOnClickListener { showBulkMutationDialog() }
        // findViewById<View>(R.id.btnBulkDelete)?.setOnClickListener { showBulkDeleteConfirmation() }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        bulkActionCard.visibility = View.VISIBLE
        siswaAdapter.setSelectionMode(true)
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        bulkActionCard.visibility = View.GONE
        cbHeaderSelectAll.isChecked = false
        siswaAdapter.setSelectionMode(false)
    }

    private fun showBulkMutationDialog() {
        val selectedItems = siswaAdapter.getSelectedItems()
        val view = layoutInflater.inflate(R.layout.dialog_bulk_mutasi, null)

        val spinnerKelas = view.findViewById<android.widget.Spinner>(R.id.spinnerTargetKelas)
        val spinnerJurusan = view.findViewById<android.widget.Spinner>(R.id.spinnerTargetJurusan)
        val spinnerStatus = view.findViewById<android.widget.Spinner>(R.id.spinnerTargetStatus)

        spinnerKelas.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("10", "11", "12"))
        spinnerJurusan.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fixedJurusanList)
        spinnerStatus.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("AKTIF", "PKL", "MUTASI", "KELUAR"))

        MaterialAlertDialogBuilder(this)
            .setTitle("Mutasi Masal (${selectedItems.size} Siswa)")
            .setView(view)
            .setPositiveButton("Mutasi") { _, _ ->
                val targetKelas = spinnerKelas.selectedItem.toString()
                val targetJurusan = spinnerJurusan.selectedItem.toString()
                val targetStatus = spinnerStatus.selectedItem.toString()
                executeBulkMutation(selectedItems, targetKelas, targetJurusan, targetStatus)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeBulkMutation(items: List<SiswaItem>, kelas: String, jurusan: String, status: String) {
        val nises = items.map { it.nis }
        val token = getAuthToken()

        lifecycleScope.launch {
            try {
                val request = com.xirpl2.SASMobile.model.BulkFieldsRequest(
                    student_ids = nises,
                    kelas = kelas,
                    jurusan = jurusan,
                    statusAkademik = status
                )
                val response = RetrofitClient.apiService.updateBulkStudentFields("Bearer $token", request)
                if (response.isSuccessful) {
                    Toast.makeText(this@DataSiswaAdminActivity, "Berhasil memutasi ${items.size} siswa", Toast.LENGTH_SHORT).show()
                    exitSelectionMode()
                    loadStudentData(reset = true)
                }
            } catch (e: Exception) {
                Toast.makeText(this@DataSiswaAdminActivity, "Gagal mutasi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBulkDeleteConfirmation() {
        val selected = siswaAdapter.getSelectedItems()
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus ${selected.size} Siswa?")
            .setMessage("Data siswa yang dihapus tidak dapat dikembalikan.")
            .setPositiveButton("Hapus") { _, _ -> executeBulkDelete(selected) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeBulkDelete(items: List<SiswaItem>) {
        val token = getAuthToken()
        lifecycleScope.launch {
            try {
                var successCount = 0
                for (siswa in items) {
                    val result = repository.deleteSiswa("Bearer $token", siswa.nis)
                    if (result.isSuccess) successCount++
                }
                Toast.makeText(this@DataSiswaAdminActivity, "Berhasil menghapus $successCount/${items.size} siswa", Toast.LENGTH_SHORT).show()
                exitSelectionMode()
                loadStudentData(reset = true)
            } catch (e: Exception) {
                Toast.makeText(this@DataSiswaAdminActivity, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initForcedClass() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        isWaliKelas = role.contains("wali")
        if (isWaliKelas) {
            forcedClass = session.getString("user_kelas", "")?.takeIf { it.isNotBlank() }
            if (forcedClass != null) {
                selectedKelas = forcedClass!!
            }
        }
    }

    private fun setupRecyclerView() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        val isReadOnly = role.contains("wali") || role == "guru"

        siswaAdapter = SiswaAdapter(
            onDetailClick = { siswa ->
                showStudentDetailDialog(siswa)
            },
            onMoreMenuClick = { anchorView, siswa ->
                showRowPopupMenu(anchorView, siswa)
            },
            isReadOnly = isReadOnly
        )

        recyclerSiswa.apply {
            layoutManager = LinearLayoutManager(this@DataSiswaAdminActivity)
            adapter = siswaAdapter
        }
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val newQuery = s?.toString() ?: ""
                    if (newQuery != searchQuery) {
                        searchQuery = newQuery
                        loadStudentData(reset = true)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs)
            }
        })
    }

    private fun setupFilters() {
        val filterJurusan = findViewById<TextView>(R.id.filterJurusan)
        val filterKelas = findViewById<TextView>(R.id.filterKelas)

        // For wali_kelas: hide Jurusan and Kelas filter dropdowns (forcedClass filtering)
        if (isWaliKelas) {
            filterKelas.visibility = View.GONE
            filterJurusan.visibility = View.GONE
        }

        filterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                filterJurusan.text = selected
                loadStudentData(reset = true)
            }
        }

        filterKelas.setOnClickListener {
            showFilterDialog("Pilih Kelas", kelasOptions, selectedKelas) { selected ->
                selectedKelas = selected
                filterKelas.text = selected
                loadStudentData(reset = true)
            }
        }

        val filterGender = findViewById<TextView>(R.id.filterGender)
        filterGender.setOnClickListener {
            showFilterDialog("Pilih Jenis Kelamin", genderOptions, selectedGender) { selected ->
                selectedGender = selected
                filterGender.text = selected
                loadStudentData(reset = true)
            }
        }

        val filterAgama = findViewById<TextView>(R.id.filterAgama)
        filterAgama.setOnClickListener {
            showFilterDialog("Pilih Agama", agamaOptions, selectedAgama) { selected ->
                selectedAgama = selected
                filterAgama.text = selected
                loadStudentData(reset = true)
            }
        }
    }

    private fun showFilterDialog(title: String, options: List<String>, currentSelection: String, onSelect: (String) -> Unit) {
        val selectedIndex = options.indexOf(currentSelection).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                onSelect(options[which])
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadStudentData(reset: Boolean = false) {
        val token = getAuthToken()

        if (token.isEmpty()) {
            Toast.makeText(this, "Token tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        loadingJob?.cancel()

        if (reset) {
            currentPage = 1
            allStudentList.clear()
            siswaAdapter.setFullList(emptyList())
            isLastPage = false
            progressLoading.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
            recyclerSiswa.visibility = View.GONE
            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
        } else {
            // Loading more pages from API — no visual indicator needed
        }

        isLoading = true

        loadingJob = lifecycleScope.launch {
            repository.getSiswaList(
                token = token,
                page = currentPage,
                pageSize = pageSize,
                jurusan = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan,
                tingkatan = if (selectedKelas == "Semua Kelas") null else selectedKelas.toIntOrNull(),
                jk = getGenderApiValue(selectedGender),
                search = if (searchQuery.isNotEmpty()) searchQuery else null
            ).fold(
                onSuccess = { response ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    if (isFinishing || isDestroyed) return@fold
                    progressLoading.visibility = View.GONE

                    val newStudents = response.data ?: emptyList()
                    totalItems = response.pagination?.total_items ?: 0
                    totalPages = response.pagination?.total_pages ?: 1

                    if (reset) {
                        allStudentList.clear()
                    }
                    allStudentList.addAll(newStudents)

                    if (newStudents.size < pageSize) {
                        isLastPage = true
                    }

                    siswaAdapter.setFullList(allStudentList.toList())

                    // Load on-demand (user scrolls), not auto-fetch all pages

                    if (allStudentList.isEmpty()) {
                        tvEmptyState.text = "Tidak ada data siswa"
                        emptyStateContainer.visibility = View.VISIBLE
                        recyclerSiswa.visibility = View.GONE
                        findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                        recyclerSiswa.visibility = View.VISIBLE
                        findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.VISIBLE
                    }

                    updateCountInfo()
                    updatePaginationUI()
                    isLoading = false
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    if (isFinishing || isDestroyed) return@fold
                    Toast.makeText(this@DataSiswaAdminActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()

                    progressLoading.visibility = View.GONE

                    if (allStudentList.isEmpty()) {
                        tvEmptyState.text = "Gagal memuat data siswa"
                        emptyStateContainer.visibility = View.VISIBLE
                        recyclerSiswa.visibility = View.GONE
                        findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
                    } else {
                        findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.VISIBLE
                    }

                    isLoading = false
                }
            )
        }
    }

    private fun loadMoreData() {
        if (!isLoading && !isLastPage) {
            currentPage++
            loadStudentData(reset = false)
        }
    }

    private fun updateCountInfo() {
        tvCountInfo.text = "Menampilkan ${allStudentList.size} dari $totalItems data"
        tvStatTotalSiswa.text = totalItems.toString()
    }

    private fun setupButtons() {
        // Action buttons inside card
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")
        val isReadOnly = role == "wali_kelas" || role == "guru"

        findViewById<View>(R.id.btnTambahSiswa)?.setOnClickListener {
            if (isReadOnly) {
                Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, TambahSiswaActivity::class.java))
            }
        }

        findViewById<View>(R.id.btnImportSiswa)?.setOnClickListener {
            if (isReadOnly) {
                Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
            } else {
                showImportSiswaDialog()
            }
        }

        findViewById<View>(R.id.btnUnduhData)?.setOnClickListener {
            showExportSiswaDialog()
        }

        findViewById<View>(R.id.btnCetakData)?.setOnClickListener {
            Toast.makeText(this, "Fitur cetak akan segera tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExportSiswaDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Ekspor Data Siswa")
            .setMessage("Filter aktif saat ini:\n" +
                    "- Kelas: $selectedKelas\n" +
                    "- Jurusan: $selectedJurusan\n" +
                    "- Jenis Kelamin: $selectedGender\n\n" +
                    "Data akan diekspor ke format CSV (.csv)")
            .setPositiveButton("Ekspor") { dialog, _ ->
                dialog.dismiss()
                exportToCsv()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun exportToCsv() {
        if (allStudentList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Mengekspor Data")
            .setMessage("Sedang menyiapkan ${allStudentList.size} data siswa...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "data_siswa_$timestamp.csv"

                val csvContent = buildString {
                    appendLine("No,NIS,Nama Siswa,Jenis Kelamin,Kelas,Jurusan")
                    allStudentList.forEachIndexed { index, siswa ->
                        val jkDisplay = if (siswa.jenis_kelamin == "L") "Laki-laki" else "Perempuan"
                        appendLine("${index + 1},${escapeCsv(siswa.nis)},${escapeCsv(siswa.nama_siswa)},$jkDisplay,${escapeCsv(siswa.kelas)},${escapeCsv(siswa.jurusan)}")
                    }
                }

                val savedUri = saveCsvFile(fileName, csvContent)

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    progressDialog.dismiss()
                    if (savedUri != null) {
                        Toast.makeText(
                            this@DataSiswaAdminActivity,
                            "Berhasil export ${allStudentList.size} data siswa ke $fileName",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@DataSiswaAdminActivity, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    progressDialog.dismiss()
                    Toast.makeText(this@DataSiswaAdminActivity, "Gagal export: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun saveCsvFile(fileName: String, csvContent: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/SASMobile")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                        outputStream.write(bom)
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                    it.toString()
                }
            } else {
                val dir = File(getExternalFilesDir(null), "Export")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.outputStream().use { outputStream ->
                    val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                    outputStream.write(bom)
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error saving CSV: ${e.message}")
            null
        }
    }

    private fun showImportSiswaDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Impor Data Siswa")
            .setMessage("Silakan unggah file CSV berisi data siswa.\n\n" +
                    "Header kolom yang WAJIB ada:\n" +
                    "- nis (Nomor Induk Siswa)\n" +
                    "- nama_siswa\n" +
                    "- jk (L / P)\n" +
                    "- tingkatan (X / XI / XII)\n" +
                    "- jurusan (Singkatan Jurusan, misal: RPL)\n" +
                    "- part (Rombel, misal: 1 / 2)\n\n" +
                    "Pastikan file berformat .csv dengan pemisah koma.")
            .setPositiveButton("Pilih File CSV") { _, _ ->
                pickCsvLauncher.launch("text/csv")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun uploadCsvFile(uri: android.net.Uri) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Mengimpor Data Siswa")
            .setMessage("Sedang mengunggah dan memproses file CSV...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        if (isFinishing || isDestroyed) return@withContext
                        progressDialog.dismiss()
                        Toast.makeText(this@DataSiswaAdminActivity, "Gagal membuka file", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val tempFile = java.io.File.createTempFile("siswa_import", ".csv", cacheDir)
                tempFile.deleteOnExit()

                tempFile.outputStream().use { output ->
                    inputStream.use { input ->
                        input.copyTo(output)
                    }
                }

                val requestFile = tempFile.asRequestBody("text/csv".toMediaTypeOrNull())

                val filePart = okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    "students_import.csv",
                    requestFile
                )

                val result = repository.importStudents(token, filePart)
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    progressDialog.dismiss()
                    result.fold(
                        onSuccess = { response ->
                            AlertDialog.Builder(this@DataSiswaAdminActivity)
                                .setTitle("Impor Selesai")
                                .setMessage(response.message ?: "Data siswa berhasil diimpor!")
                                .setPositiveButton("OK") { _, _ ->
                                    loadStudentData(reset = true)
                                }
                                .show()
                        },
                        onFailure = { error ->
                            AlertDialog.Builder(this@DataSiswaAdminActivity)
                                .setTitle("Gagal Mengimpor")
                                .setMessage(error.message ?: "Terjadi kesalahan tidak diketahui")
                                .setPositiveButton("Tutup", null)
                                .show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    progressDialog.dismiss()
                    Toast.makeText(this@DataSiswaAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEditSiswaDialog(siswa: SiswaItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_siswa, null)

        val tvTitle = dialogView.findViewById<TextView>(android.R.id.title)
            ?: dialogView.findViewWithTag<TextView>("title")

        val etNis = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNis)
        val etNama = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNama)
        val rgJenisKelamin = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgJenisKelamin)
        val rbLakiLaki = dialogView.findViewById<android.widget.RadioButton>(R.id.rbLakiLaki)
        val rbPerempuan = dialogView.findViewById<android.widget.RadioButton>(R.id.rbPerempuan)
        val etKelas = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etKelas)
        val actvJurusan = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvJurusan)
        val btnBatal = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatal)
        val btnSimpan = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSimpan)
        val btnClose = dialogView.findViewById<android.widget.ImageView>(R.id.btnClose)

        etNis.setText(siswa.nis)
        etNis.isEnabled = false
        etNama.setText(siswa.nama_siswa)
        if (siswa.jenis_kelamin == "L") {
            rbLakiLaki.isChecked = true
        } else {
            rbPerempuan.isChecked = true
        }
        etKelas.setText(siswa.kelas)
        actvJurusan.setText(siswa.jurusan)

        val jurusanAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            fixedJurusanList
        )
        actvJurusan.setAdapter(jurusanAdapter)

        btnSimpan.text = "Update"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnSimpan.setOnClickListener {
            val nama = etNama.text?.toString()?.trim() ?: ""
            val jenisKelamin = if (rbLakiLaki.isChecked) "L" else "P"
            val kelas = etKelas.text?.toString()?.trim()?.uppercase() ?: ""
            val jurusan = actvJurusan.text?.toString()?.trim()?.uppercase() ?: ""

            if (nama.isEmpty()) {
                etNama.error = "Nama tidak boleh kosong"
                return@setOnClickListener
            }
            if (kelas.isEmpty() || kelas !in listOf("10", "11", "12")) {
                etKelas.error = "Kelas harus 10, 11, atau 12"
                return@setOnClickListener
            }
            if (jurusan.isEmpty()) {
                Toast.makeText(this, "Pilih jurusan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = com.xirpl2.SASMobile.model.UpdateSiswaRequest(
                nama_siswa = nama,
                jenis_kelamin = jenisKelamin,
                kelas = kelas,
                jurusan = jurusan
            )

            btnSimpan.isEnabled = false
            btnSimpan.text = "Mengupdate..."

            lifecycleScope.launch {
                repository.updateSiswa(getAuthToken(), siswa.nis, request).fold(
                    onSuccess = { updatedSiswa ->
                        runOnUiThread {
                            Toast.makeText(this@DataSiswaAdminActivity, "Siswa berhasil diupdate!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()

                            loadStudentData(reset = true)
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this@DataSiswaAdminActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                            btnSimpan.isEnabled = true
btnSimpan.text = "Perbarui"
                        }
                    }
                )
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showDeleteConfirmationDialog(siswa: SiswaItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Siswa")
            .setMessage("Apakah Anda yakin ingin menghapus siswa:\n\nNIS: ${siswa.nis}\nNama: ${siswa.nama_siswa}")
            .setPositiveButton("Hapus") { dialog, _ ->
                dialog.dismiss()
                deleteSiswa(siswa)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteSiswa(siswa: SiswaItem) {
        lifecycleScope.launch {
            repository.deleteSiswa(getAuthToken(), siswa.nis).fold(
                onSuccess = { message ->
                    runOnUiThread {
                        Toast.makeText(this@DataSiswaAdminActivity, "Siswa berhasil dihapus!", Toast.LENGTH_SHORT).show()

                        loadStudentData(reset = true)
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(this@DataSiswaAdminActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun showRowPopupMenu(anchor: View, siswa: SiswaItem) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Detail")
        popup.menu.add(0, 2, 1, "Ubah")
        popup.menu.add(0, 3, 2, "Hapus")

        // Force show icons
        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    setForceShowIcon.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) { }

        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")
        val isReadOnly = role == "wali_kelas" || role == "guru"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { showStudentDetailDialog(siswa); true }
                2 -> {
                    if (isReadOnly) {
                        Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                    } else {
                        showEditSiswaDialog(siswa)
                    }
                    true
                }
                3 -> {
                    if (isReadOnly) {
                        Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                    } else {
                        showDeleteConfirmationDialog(siswa)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupPagination() {
        btnPrevPage.setOnClickListener {
            siswaAdapter.prevPage()
            updatePaginationUI()
        }
        btnNextPage.setOnClickListener {
            siswaAdapter.nextPage()
            updatePaginationUI()
        }
    }

    private fun updatePaginationUI() {
        val totalPages = siswaAdapter.getTotalPages()
        val currentPage = siswaAdapter.getCurrentPage()
        tvPageInfo.text = "Halaman $currentPage dari $totalPages"
        btnPrevPage.alpha = if (currentPage <= 1) 0.3f else 1f
        btnNextPage.alpha = if (currentPage >= totalPages) 0.3f else 1f
        // Update count info
        val start = (currentPage - 1) * 20 + 1
        val end = minOf(currentPage * 20, allStudentList.size)
        val shown = end - start + 1
        tvCountInfo.text = "Menampilkan $shown dari $totalItems data"
    }

    private fun showStudentDetailDialog(siswa: SiswaItem) {
        val dialog = SiswaDetailDialogFragment.newInstance(siswa)
        dialog.show(supportFragmentManager, "SiswaDetailDialog")
    }
}
