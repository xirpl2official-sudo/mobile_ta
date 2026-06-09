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
import com.xirpl2.SASMobile.repository.BerandaRepository
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
    private lateinit var tvCountInfo: TextView
    private lateinit var tableContainer: android.widget.LinearLayout
    private lateinit var paginationRow: LinearLayout
    private lateinit var tvPagination: TextView
    private lateinit var btnPrevPage: com.google.android.material.button.MaterialButton
    private lateinit var btnNextPage: com.google.android.material.button.MaterialButton

    private lateinit var adapter: SiswaAdapter

    private var searchQuery: String = ""
    private var selectedJurusanId: Int? = null
    private var selectedWaliStaffId: Int? = null
    private var totalItemsCount = 0
    private val allStudents = mutableListOf<SiswaItem>()
    private val apiPageSize = 50

    private var forcedClass: String? = null
    private var forcedClassId: Int? = null
    private var isWaliKelas: Boolean = false

    private val repository = BerandaRepository()
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
        searchHandler.removeCallbacksAndMessages(null)
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
        tvCountInfo = findViewById(R.id.tvCountInfo)
        tableContainer = findViewById(R.id.tableContainer)
        paginationRow = findViewById(R.id.paginationRow)
        tvPagination = findViewById(R.id.tvPagination)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)

        swipeRefresh.setOnRefreshListener {
            loadUnregisteredStudents(reset = true)
        }

        btnPrevPage.setOnClickListener {
            adapter.prevPage()
            updatePagination()
        }

        btnNextPage.setOnClickListener {
            adapter.nextPage()
            updatePagination()
        }

        findViewById<View>(R.id.iconMenu).setOnClickListener { openSidebar() }
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(
            onDetailClick = { siswa ->
                val dialog = SiswaDetailDialogFragment.newInstance(siswa)
                dialog.show(supportFragmentManager, "SiswaDetail")
            },
            isReadOnly = true,
            isUnregistered = true
        )

        rvSiswaBaru.layoutManager = LinearLayoutManager(this)
        rvSiswaBaru.adapter = adapter
        rvSiswaBaru.isNestedScrollingEnabled = false
    }

    private fun updatePagination() {
        val current = adapter.getCurrentPage()
        val total = adapter.getTotalPages()
        tvPagination.text = getString(R.string.halaman_x_dari_y, current, total)
        btnPrevPage.isEnabled = current > 1
        btnNextPage.isEnabled = current < total
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

        if (isWaliKelas) {
            acJurusan.visibility = View.GONE
            acWaliKelas.visibility = View.GONE
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val newQuery = s?.toString()?.trim() ?: ""
                    if (newQuery != searchQuery) {
                        searchQuery = newQuery
                        loadUnregisteredStudents(reset = true)
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs)
            }
        })

        if (!isWaliKelas) {
            lifecycleScope.launch {
                repository.getJurusanLookup(token).fold(
                    onSuccess = { list ->
                        val options = listOf("Semua Jurusan") + list.map { it.nama }
                        val spinnerAdapter = ArrayAdapter(this@SiswaBelumTerdaftarAdminActivity, android.R.layout.simple_dropdown_item_1line, options)
                        acJurusan.setAdapter(spinnerAdapter)
                        acJurusan.setText(options[0], false)
                        acJurusan.setOnItemClickListener { _, _, position, _ ->
                            selectedJurusanId = if (position == 0) null else list[position - 1].id
                            loadUnregisteredStudents(reset = true)
                        }
                    },
                    onFailure = { }
                )
            }

            lifecycleScope.launch {
                repository.getStaffGuruLookup(token).fold(
                    onSuccess = { list ->
                        val options = listOf("Semua Wali") + list.map { it.nama }
                        val spinnerAdapter = ArrayAdapter(this@SiswaBelumTerdaftarAdminActivity, android.R.layout.simple_dropdown_item_1line, options)
                        acWaliKelas.setAdapter(spinnerAdapter)
                        acWaliKelas.setText(options[0], false)
                        acWaliKelas.setOnItemClickListener { _, _, position, _ ->
                            selectedWaliStaffId = if (position == 0) null else list[position - 1].id_staff
                            loadUnregisteredStudents(reset = true)
                        }
                    },
                    onFailure = { }
                )
            }
        }
    }

    private fun loadUnregisteredStudents(reset: Boolean = true) {
        val token = getAuthToken()
        if (token.isEmpty()) {
            showLoading(false)
            layoutEmpty.visibility = View.VISIBLE
            return
        }

        if (reset) {
            allStudents.clear()
        }

        showLoading(true)
        loadingJob?.cancel()

        loadingJob = lifecycleScope.launch {
            if (forcedClass != null && forcedClassId == null) {
                repository.getKelasLookup(token).fold(
                    onSuccess = { kelasList ->
                        val match = kelasList.find {
                            it.label.equals(forcedClass, ignoreCase = true) ||
                            "${it.tingkatan} ${it.jurusan} ${it.part}".equals(forcedClass, ignoreCase = true) ||
                            "${it.tingkatan}${it.jurusan}${it.part}".equals(forcedClass, ignoreCase = true)
                        }
                        forcedClassId = match?.id_kelas
                    },
                    onFailure = { }
                )
            }

            val tempList = mutableListOf<SiswaItem>()
            var page = 1
            var loadedAll = false
            var lastError: Throwable? = null

            while (!loadedAll) {
                repository.getUnregisteredStudents(
                    token = token,
                    page = page,
                    pageSize = apiPageSize,
                    search = if (searchQuery.isNotEmpty()) searchQuery else null,
                    jurusan = selectedJurusanId,
                    waliKelas = selectedWaliStaffId,
                    idKelas = forcedClassId
                ).fold(
                    onSuccess = { body ->
                        val students = body.data ?: emptyList()
                        totalItemsCount = body.pagination?.total_items ?: students.size
                        tempList.addAll(students)
                        val totalPages = body.pagination?.total_pages ?: 1
                        loadedAll = page >= totalPages || students.isEmpty()
                        page++
                    },
                    onFailure = { e ->
                        loadedAll = true
                        lastError = e
                    }
                )
            }

            showLoading(false)

            if (lastError != null && tempList.isEmpty()) {
                val e = lastError!!
                val msg = when {
                    e.message?.contains("HTTP Error: 401") == true -> "Sesi habis, silakan login ulang"
                    e.message?.contains("HTTP Error: 500") == true -> "Server mengalami kesalahan"
                    e.message?.contains("Unable to resolve host") == true || e.message?.contains("Failed to connect") == true -> "Tidak dapat terhubung ke server"
                    else -> "Kesalahan: ${e.message}"
                }
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, msg, Toast.LENGTH_LONG).show()
                return@launch
            }

            allStudents.clear()
            allStudents.addAll(tempList)

            if (allStudents.isNotEmpty()) {
                adapter.setFullList(allStudents.toList())
            }
            tvCountInfo.text = "Menampilkan ${allStudents.size} dari $totalItemsCount data"
            val empty = allStudents.isEmpty()
            tableContainer.visibility = if (empty) View.GONE else View.VISIBLE
            layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            paginationRow.visibility = if (empty || adapter.getTotalPages() <= 1) View.GONE else View.VISIBLE
            updatePagination()
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
