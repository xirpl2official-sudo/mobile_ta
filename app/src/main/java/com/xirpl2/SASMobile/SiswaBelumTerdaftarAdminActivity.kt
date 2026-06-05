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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var btnNotifyBulk: com.google.android.material.button.MaterialButton
    private lateinit var tableHorizontalScrollView: HorizontalScrollView
    private lateinit var btnLoadMore: com.google.android.material.button.MaterialButton
    
    private lateinit var adapter: SiswaAdapter
    
    private var searchQuery: String = ""
    private var selectedJurusanId: Int? = null
    private var selectedWaliStaffId: Int? = null
    private var currentApiPage = 1
    private var hasMorePages = false
    private var totalItemsCount = 0
    private val allStudents = mutableListOf<SiswaItem>()
    private val apiPageSize = 100

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
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
        cbSelectAll = findViewById(R.id.cbSelectAll)
        tvCountInfo = findViewById(R.id.tvCountInfo)
        btnNotifyBulk = findViewById(R.id.btnNotifyBulk)
        tableHorizontalScrollView = findViewById(R.id.tableHorizontalScrollView)
        btnLoadMore = findViewById(R.id.btnLoadMore)

        swipeRefresh.setOnRefreshListener {
            currentApiPage = 1
            allStudents.clear()
            loadUnregisteredStudents(reset = true)
        }

        btnLoadMore.setOnClickListener {
            if (!hasMorePages) return@setOnClickListener
            currentApiPage++
            loadUnregisteredStudents(reset = false)
        }

        btnNotifyBulk.setOnClickListener {
            showNotifyConfirmation()
        }

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAll(isChecked)
        }

        findViewById<View>(R.id.iconMenu).setOnClickListener { openSidebar() }
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(
            onDetailClick = { siswa ->
                val dialog = SiswaDetailDialogFragment.newInstance(siswa)
                dialog.show(supportFragmentManager, "SiswaDetail")
            },
            onMoreMenuClick = { _, _ -> },
            isReadOnly = true
        )
        
        adapter.setSelectionMode(true)
        adapter.setOnSelectionChangedListener { count ->
            btnNotifyBulk.visibility = if (count > 0) View.VISIBLE else View.GONE
            btnNotifyBulk.text = "Pengingat ($count)"
        }

        rvSiswaBaru.layoutManager = LinearLayoutManager(this)
        rvSiswaBaru.adapter = adapter
    }

    private fun showNotifyConfirmation() {
        val count = adapter.getSelectedCount()
        MaterialAlertDialogBuilder(this)
            .setTitle("Kirim Pengingat")
            .setMessage("Kirim notifikasi pengingat pendaftaran kepada $count siswa terpilih?")
            .setPositiveButton("Kirim") { _, _ ->
                executeNotifyBulk()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeNotifyBulk() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return

        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnNotifyBulk.isEnabled = false

        lifecycleScope.launch {
            val request = com.xirpl2.SASMobile.model.NotifyWaliKelasRequest(
                nisList = selected.map { it.nis },
                message = "Silakan segera mendaftarkan perangkat Anda"
            )
            repository.notifyWaliKelas(token, request).fold(
                onSuccess = {
                    progressBar.visibility = View.GONE
                    btnNotifyBulk.isEnabled = true
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, "Berhasil mengirim pengingat ke ${selected.size} siswa", Toast.LENGTH_SHORT).show()
                    adapter.selectAll(false)
                    cbSelectAll.isChecked = false
                },
                onFailure = { e ->
                    progressBar.visibility = View.GONE
                    btnNotifyBulk.isEnabled = true
                    val msg = if (e is java.net.UnknownHostException) "Tidak dapat terhubung ke server" else "Kesalahan: ${e.message}"
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, msg, Toast.LENGTH_SHORT).show()
                }
            )
        }
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
        if (token.isEmpty()) return

        if (reset) {
            currentApiPage = 1
            allStudents.clear()
        }

        showLoading(true)
        loadingJob?.cancel()

        loadingJob = lifecycleScope.launch {
            if (forcedClass != null && forcedClassId == null) {
                repository.getKelasLookup(token).fold(
                    onSuccess = { kelasList ->
                        val match = kelasList.find { "${it.tingkatan}${it.part}" == forcedClass }
                        forcedClassId = match?.id_kelas
                    },
                    onFailure = { }
                )
            }

            repository.getUnregisteredStudents(
                token = token,
                page = currentApiPage,
                pageSize = apiPageSize,
                search = if (searchQuery.isNotEmpty()) searchQuery else null,
                jurusan = selectedJurusanId,
                waliKelas = selectedWaliStaffId,
                idKelas = forcedClassId
            ).fold(
                onSuccess = { body ->
                    showLoading(false)
                    val students = body.data ?: emptyList()
                    totalItemsCount = body.pagination?.total_items ?: students.size
                    val totalPages = body.pagination?.total_pages ?: 1
                    hasMorePages = currentApiPage < totalPages

                    if (reset) {
                        allStudents.clear()
                        allStudents.addAll(students)
                    } else {
                        allStudents.addAll(students)
                    }

                    adapter.setFullList(allStudents.toList())
                    adapter.selectAll(false)
                    cbSelectAll.isChecked = false
                    tvCountInfo.text = "Menampilkan ${allStudents.size} dari $totalItemsCount data"
                    val empty = allStudents.isEmpty()
                    tableHorizontalScrollView.visibility = if (empty) View.GONE else View.VISIBLE
                    layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    btnLoadMore.visibility = if (hasMorePages) View.VISIBLE else View.GONE
                },
                onFailure = { e ->
                    showLoading(false)
                    if (!reset) currentApiPage--
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
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
