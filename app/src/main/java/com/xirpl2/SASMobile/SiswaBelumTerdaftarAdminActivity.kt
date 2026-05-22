package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xirpl2.SASMobile.adapter.SiswaAdapter
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SiswaBelumTerdaftarAdminActivity : BaseAdminActivity() {

    private lateinit var rvSiswaBaru: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var acJurusan: AutoCompleteTextView
    private lateinit var acWaliKelas: AutoCompleteTextView
    private lateinit var cbSelectAll: CheckBox
    private lateinit var tvCountInfo: TextView
    
    private lateinit var adapter: SiswaAdapter
    
    private var searchQuery: String = ""
    private var selectedJurusanId: Int? = null
    private var selectedWaliStaffId: Int? = null

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
    private var forcedClass: String? = null
    private var isWaliKelas: Boolean = false
    
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 300L
    private var loadingJob: Job? = null

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.SISWA_BELUM_TERDAFTAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siswa_belum_terdaftar_admin)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupRecyclerView()
        initForcedClass()
        setupFilters()
        setupDrawerAndSidebar()
        setupMenuIcon()
        
        loadUnregisteredStudents()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        loadingJob?.cancel()
    }

    private fun initializeViews() {
        rvSiswaBaru = findViewById(R.id.rvSiswaBaru)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        etSearch = findViewById(R.id.etSearch)
        acJurusan = findViewById(R.id.acJurusan)
        acWaliKelas = findViewById(R.id.acWaliKelas)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        tvCountInfo = findViewById(R.id.tvCountInfo)

        swipeRefresh.setOnRefreshListener {
            loadUnregisteredStudents()
        }

        findViewById<View>(R.id.iconMenu).setOnClickListener { openSidebar() }
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(
            onEditClick = {},
            onDeleteClick = {},
            onDetailClick = { siswa ->
                val dialog = SiswaDetailDialogFragment.newInstance(siswa)
                dialog.show(supportFragmentManager, "SiswaDetail")
            },
            isReadOnly = true
        )
        rvSiswaBaru.layoutManager = LinearLayoutManager(this)
        rvSiswaBaru.adapter = adapter
    }

    private fun initForcedClass() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        isWaliKelas = role.contains("wali")
        if (isWaliKelas) {
            forcedClass = session.getString("user_kelas", "")?.takeIf { it.isNotBlank() }
        }
    }

    private fun setupFilters() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        // For wali_kelas: hide Jurusan and Wali Kelas filter dropdowns (forcedClass filtering)
        if (isWaliKelas) {
            acJurusan.visibility = View.GONE
            acWaliKelas.visibility = View.GONE
        }

        // 1. Setup Search with Debounce
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val newQuery = s?.toString()?.trim() ?: ""
                    if (newQuery != searchQuery) {
                        searchQuery = newQuery
                        loadUnregisteredStudents()
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs)
            }
        })

        // 2. Fetch & Populate Jurusan (only for admin)
        if (!isWaliKelas) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.getJurusanLookup("Bearer $token")
                    if (response.isSuccessful) {
                        val list = response.body()?.data ?: emptyList()
                        val options = listOf("Semua Jurusan") + list.map { it.nama }
                        val spinnerAdapter = ArrayAdapter(this@SiswaBelumTerdaftarAdminActivity, android.R.layout.simple_dropdown_item_1line, options)
                        acJurusan.setAdapter(spinnerAdapter)
                        acJurusan.setText(options[0], false)
                        acJurusan.setOnItemClickListener { _, _, position, _ ->
                            selectedJurusanId = if (position == 0) null else list[position - 1].id
                            loadUnregisteredStudents()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Fetch & Populate Wali Kelas (Staff Guru) (only for admin)
        if (!isWaliKelas) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.apiService.getStaffGuruLookup("Bearer $token")
                    if (response.isSuccessful) {
                        val list = response.body()?.data ?: emptyList()
                        val options = listOf("Semua Wali") + list.map { it.nama }
                        val spinnerAdapter = ArrayAdapter(this@SiswaBelumTerdaftarAdminActivity, android.R.layout.simple_dropdown_item_1line, options)
                        acWaliKelas.setAdapter(spinnerAdapter)
                        acWaliKelas.setText(options[0], false)
                        acWaliKelas.setOnItemClickListener { _, _, position, _ ->
                            selectedWaliStaffId = if (position == 0) null else list[position - 1].id_staff
                            loadUnregisteredStudents()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadUnregisteredStudents() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        showLoading(true)
        loadingJob?.cancel()

        loadingJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnregisteredStudents(
                    token = "Bearer $token",
                    page = 1,
                    pageSize = 100,
                    search = if (searchQuery.isNotEmpty()) searchQuery else null,
                    jurusan = selectedJurusanId?.toString(),
                    waliKelas = selectedWaliStaffId?.toString()
                )

                runOnUiThread {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val allStudents = body?.data ?: emptyList()

                        // ForcedClass: filter to only the wali_kelas's assigned class (client-side)
                        val students = if (forcedClass != null) {
                            allStudents.filter { it.kelas == forcedClass }
                        } else {
                            allStudents
                        }
                        adapter.submitList(students)

                        val total = body?.pagination?.total_items ?: allStudents.size
                        tvCountInfo.text = "Menampilkan ${students.size} dari $total data"

                        layoutEmpty.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(this@SiswaBelumTerdaftarAdminActivity,
                            "Gagal mengambil data: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        if (isLoading) {
            layoutEmpty.visibility = View.GONE
        } else {
            swipeRefresh.isRefreshing = false
        }
    }
}