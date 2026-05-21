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
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.adapter.SiswaAdapter
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private lateinit var tvCountInfo: TextView
    
    
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
    private var searchQuery: String = ""
    
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 300L
    
    
    private var loadingJob: Job? = null
    
    
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "X", "XI", "XII")
    private val genderOptions: List<String> = listOf("Semua JK", "Laki-laki", "Perempuan")
    
    
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
        
        
        setupFilters()
        
        
        setupRecyclerView()
        
        
        loadStudentData(reset = true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        
        loadingJob?.cancel()
    }
    
    private fun initViews() {
        recyclerSiswa = findViewById(R.id.recyclerSiswa)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvCountInfo = findViewById(R.id.tvCountInfo)
    }
    
    private fun setupRecyclerView() {
        
        val role = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("user_role", "")?.lowercase() ?: ""
        val isReadOnly = role.contains("wali") || role == "guru"

        siswaAdapter = SiswaAdapter(
            onEditClick = { siswa ->
                if (!isReadOnly) showEditSiswaDialog(siswa)
            },
            onDeleteClick = { siswa ->
                if (!isReadOnly) showDeleteConfirmationDialog(siswa)
            },
            onDetailClick = { siswa ->
                showStudentDetailDialog(siswa)
            },
            isReadOnly = isReadOnly
        )
        
        recyclerSiswa.apply {
            layoutManager = LinearLayoutManager(this@DataSiswaAdminActivity)
            adapter = siswaAdapter
            
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                            && firstVisibleItemPosition >= 0) {
                            loadMoreData()
                        }
                    }
                }
            })
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
        filterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                filterJurusan.text = selected
                loadStudentData(reset = true)
            }
        }
        
        val filterKelas = findViewById<TextView>(R.id.filterKelas)
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
            siswaAdapter.submitList(emptyList())
            isLastPage = false
            progressLoading.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            recyclerSiswa.visibility = View.GONE
            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
        } else {
            siswaAdapter.setLoadingMore(true)
        }
        
        isLoading = true
        
        loadingJob = lifecycleScope.launch {
            repository.getSiswaList(
                token = token,
                page = currentPage,
                pageSize = pageSize,
                jurusan = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan,
                kelas = if (selectedKelas == "Semua Kelas") null else selectedKelas,
                jk = getGenderApiValue(selectedGender),
                search = if (searchQuery.isNotEmpty()) searchQuery else null
            ).fold(
                onSuccess = { response ->
                    runOnUiThread {
                        progressLoading.visibility = View.GONE
                        siswaAdapter.setLoadingMore(false)
                        
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
                        
                        siswaAdapter.submitList(allStudentList.toList())
                        
                        
                        if (allStudentList.isEmpty()) {
                            tvEmptyState.text = "Tidak ada data siswa"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerSiswa.visibility = View.GONE
                            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
                        } else {
                            tvEmptyState.visibility = View.GONE
                            recyclerSiswa.visibility = View.VISIBLE
                            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.VISIBLE
                        }
                        
                        updateCountInfo()
                        isLoading = false
                        
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(this@DataSiswaAdminActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                        
                        progressLoading.visibility = View.GONE
                        siswaAdapter.setLoadingMore(false)
                        
                        if (allStudentList.isEmpty()) {
                            tvEmptyState.text = "Gagal memuat data siswa"
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerSiswa.visibility = View.GONE
                            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
                        } else {
                            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.VISIBLE
                        }
                        
                        isLoading = false
                    }
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
    }

    private fun setupButtons() {
        val btnMore = findViewById<android.view.View>(R.id.btnMore)

        btnMore?.setOnClickListener { view ->
            showOverflowMenu(view)
        }
    }

    private fun showOverflowMenu(view: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        
        popup.menu.add(0, 1, 0, "Tambah Siswa")
        popup.menu.add(0, 2, 1, "Import Data")
        popup.menu.add(0, 3, 2, "Export Data")
        
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
        } catch (e: Exception) {
            // ignore
        }

        val role = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("user_role", "")
        val isReadOnly = role == "wali_kelas" || role == "guru"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    if (isReadOnly) {
                        Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(android.content.Intent(this, TambahSiswaActivity::class.java))
                    }
                    true
                }
                2 -> {
                    if (isReadOnly) {
                        Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                    } else {
                        showImportSiswaDialog()
                    }
                    true
                }
                3 -> {
                    showExportSiswaDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showExportSiswaDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Export Data Siswa")
            .setMessage("Filter aktif saat ini:\n" +
                    "• Kelas: $selectedKelas\n" +
                    "• Jurusan: $selectedJurusan\n" +
                    "• Jenis Kelamin: $selectedGender\n\n" +
                    "Data akan diekspor ke format CSV (.csv)")
            .setPositiveButton("Export") { dialog, _ ->
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

        lifecycleScope.launch {
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

                runOnUiThread {
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
                runOnUiThread {
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/SASMobile")
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
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
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            file.absolutePath
        }
    }

    private fun showImportSiswaDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Import Data Siswa")
            .setMessage("Silakan unggah file CSV berisi data siswa.\n\n" +
                    "Header kolom yang WAJIB ada:\n" +
                    "• nis (Nomor Induk Siswa)\n" +
                    "• nama_siswa\n" +
                    "• jk (L / P)\n" +
                    "• tingkatan (X / XI / XII)\n" +
                    "• jurusan (Singkatan Jurusan, misal: RPL)\n" +
                    "• part (Rombel, misal: 1 / 2)\n\n" +
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

        lifecycleScope.launch {
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    runOnUiThread {
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
                runOnUiThread {
                    progressDialog.dismiss()
                    result.fold(
                        onSuccess = { response ->
                            AlertDialog.Builder(this@DataSiswaAdminActivity)
                                .setTitle("Import Selesai")
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
                runOnUiThread {
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
            if (kelas.isEmpty() || kelas !in listOf("X", "XI", "XII")) {
                etKelas.error = "Kelas harus X, XI, atau XII"
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
                            btnSimpan.text = "Update"
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

    private fun showStudentDetailDialog(siswa: SiswaItem) {
        val dialog = SiswaDetailDialogFragment.newInstance(siswa)
        dialog.show(supportFragmentManager, "SiswaDetailDialog")
    }
}
