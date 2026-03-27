package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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

class DataSiswaAdminActivity : BaseAdminActivity() {

    private val TAG = "DataSiswaAdminActivity"
    
    private val repository = BerandaRepository()
    
    // RecyclerView and Adapter
    private lateinit var recyclerSiswa: RecyclerView
    private lateinit var siswaAdapter: SiswaAdapter
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCountInfo: TextView
    
    // Data siswa
    private val allStudentList = mutableListOf<SiswaItem>()
    
    // Pagination state
    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private var isLoading = false
    private var isLastPage = false
    private val pageSize = 100
    
    // Filter states
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedKelas: String = "Semua Kelas"
    private var selectedGender: String = "Semua JK"
    private var searchQuery: String = ""
    
    // Debounce handler for search
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 300L
    
    // Current loading job
    private var loadingJob: Job? = null
    
    // Available options for filters - Using Roman numerals for kelas
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "X", "XI", "XII")
    private val genderOptions: List<String> = listOf("Semua JK", "Laki-laki", "Perempuan")
    
    // Gender display to API value mapping
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

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        initViews()
        
        // Setup Drawer and Sidebar (from BaseAdminActivity)
        setupDrawerAndSidebar()
        
        // Setup Menu Icon (from BaseAdminActivity)
        setupMenuIcon()
        
        // Setup Buttons
        setupButtons()
        
        // Setup Search with debounce
        setupSearch()
        
        // Setup Filters
        setupFilters()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load initial data from API
        loadStudentData(reset = true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any pending search
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        // Cancel any ongoing loading
        loadingJob?.cancel()
    }
    
    /**
     * Initialize view references
     */
    private fun initViews() {
        recyclerSiswa = findViewById(R.id.recyclerSiswa)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvCountInfo = findViewById(R.id.tvCountInfo)
    }
    
    /**
     * Setup RecyclerView with adapter and scroll listener
     */
    private fun setupRecyclerView() {
        // Check role
        val role = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("user_role", "")
        val isReadOnly = role == "wali_kelas" || role == "guru"

        siswaAdapter = SiswaAdapter(
            onEditClick = { siswa ->
                if (!isReadOnly) showEditSiswaDialog(siswa)
            },
            onDeleteClick = { siswa ->
                if (!isReadOnly) showDeleteConfirmationDialog(siswa)
            },
            isReadOnly = isReadOnly
        )
        
        recyclerSiswa.apply {
            layoutManager = LinearLayoutManager(this@DataSiswaAdminActivity)
            adapter = siswaAdapter
            
            // Add scroll listener for infinite scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // Check if we need to load more
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
    
    /**
     * Setup search functionality with debounce
     */
    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Cancel previous search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                
                // Create new search with debounce
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
    
    /**
     * Setup filter buttons
     */
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
    
    /**
     * Show filter dialog
     */
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
    
    /**
     * Load student data from API
     */
    private fun loadStudentData(reset: Boolean = false) {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            Log.w(TAG, "No auth token, cannot load student data")
            Toast.makeText(this, "Token tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Cancel previous job if any
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
                        
                        // Handle empty state
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
                        
                        Log.d(TAG, "Loaded page $currentPage: ${newStudents.size} students, total: ${allStudentList.size}/$totalItems")
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        Log.e(TAG, "Error loading student data: ${error.message}")
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
        val btnTambah = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTambah)
        
        // Check role
        val role = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("user_role", "")
        if (role == "wali_kelas" || role == "guru") {
            btnTambah.visibility = View.GONE
        } else {
            btnTambah.visibility = View.VISIBLE
            btnTambah.setOnClickListener {
                showTambahSiswaDialog()
            }
        }
    }
    
    /**
     * Show dialog to add new student
     */
    private fun showTambahSiswaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_siswa, null)
        
        val etNis = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNis)
        val etNama = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNama)
        val rgJenisKelamin = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgJenisKelamin)
        val rbLakiLaki = dialogView.findViewById<android.widget.RadioButton>(R.id.rbLakiLaki)
        val etKelas = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etKelas)
        val actvJurusan = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvJurusan)
        val btnBatal = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatal)
        val btnSimpan = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSimpan)
        val btnClose = dialogView.findViewById<android.widget.ImageView>(R.id.btnClose)
        
        // Setup jurusan dropdown
        val jurusanAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            fixedJurusanList
        )
        actvJurusan.setAdapter(jurusanAdapter)
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        btnBatal.setOnClickListener {
            dialog.dismiss()
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        btnSimpan.setOnClickListener {
            val nis = etNis.text?.toString()?.trim() ?: ""
            val nama = etNama.text?.toString()?.trim() ?: ""
            val jenisKelamin = if (rbLakiLaki.isChecked) "L" else "P"
            val kelas = etKelas.text?.toString()?.trim()?.uppercase() ?: ""
            val jurusan = actvJurusan.text?.toString()?.trim()?.uppercase() ?: ""
            
            // Validation
            if (nis.isEmpty()) {
                etNis.error = "NIS tidak boleh kosong"
                return@setOnClickListener
            }
            if (nama.isEmpty()) {
                etNama.error = "Nama tidak boleh kosong"
                return@setOnClickListener
            }
            if (rgJenisKelamin.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
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
            
            // Create request
            val request = com.xirpl2.SASMobile.model.CreateSiswaRequest(
                nis = nis,
                nama_siswa = nama,
                jenis_kelamin = jenisKelamin,
                kelas = kelas,
                jurusan = jurusan
            )
            
            // Disable button to prevent double submit
            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."
            
            // Call API
            lifecycleScope.launch {
                repository.createSiswa(getAuthToken(), request).fold(
                    onSuccess = { siswa ->
                        runOnUiThread {
                            Toast.makeText(this@DataSiswaAdminActivity, "Siswa berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            // Reload data to show new student
                            loadStudentData(reset = true)
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            Toast.makeText(this@DataSiswaAdminActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                            btnSimpan.isEnabled = true
                            btnSimpan.text = "Simpan"
                        }
                    }
                )
            }
        }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    /**
     * Show dialog to edit existing student
     */
    private fun showEditSiswaDialog(siswa: SiswaItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tambah_siswa, null)
        
        // Update title
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
        
        // Pre-fill with existing data
        etNis.setText(siswa.nis)
        etNis.isEnabled = false // NIS cannot be changed
        etNama.setText(siswa.nama_siswa)
        if (siswa.jenis_kelamin == "L") {
            rbLakiLaki.isChecked = true
        } else {
            rbPerempuan.isChecked = true
        }
        etKelas.setText(siswa.kelas)
        actvJurusan.setText(siswa.jurusan)
        
        // Setup jurusan dropdown
        val jurusanAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            fixedJurusanList
        )
        actvJurusan.setAdapter(jurusanAdapter)
        
        // Change button text
        btnSimpan.text = "Update"
        
        // Create dialog
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
            
            // Validation
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
            
            // Create request
            val request = com.xirpl2.SASMobile.model.UpdateSiswaRequest(
                nama_siswa = nama,
                jenis_kelamin = jenisKelamin,
                kelas = kelas,
                jurusan = jurusan
            )
            
            // Disable button to prevent double submit
            btnSimpan.isEnabled = false
            btnSimpan.text = "Mengupdate..."
            
            // Call API
            lifecycleScope.launch {
                repository.updateSiswa(getAuthToken(), siswa.nis, request).fold(
                    onSuccess = { updatedSiswa ->
                        runOnUiThread {
                            Toast.makeText(this@DataSiswaAdminActivity, "Siswa berhasil diupdate!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            // Reload data to show updated student
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
    
    /**
     * Show confirmation dialog to delete student
     */
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
    
    /**
     * Delete student from API
     */
    private fun deleteSiswa(siswa: SiswaItem) {
        lifecycleScope.launch {
            repository.deleteSiswa(getAuthToken(), siswa.nis).fold(
                onSuccess = { message ->
                    runOnUiThread {
                        Toast.makeText(this@DataSiswaAdminActivity, "Siswa berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        // Reload data
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
}
