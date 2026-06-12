package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.adapter.SiswaAdapter
import com.xirpl2.SASMobile.model.NotifyWaliKelasRequest
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
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
    private lateinit var paginationContainer: LinearLayout
    private lateinit var tvPagination: TextView
    private lateinit var btnPrevPage: MaterialButton
    private lateinit var btnNextPage: MaterialButton
    private lateinit var cbSelectAll: CheckBox
    private lateinit var notifyBar: android.widget.LinearLayout
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnNotifyWali: MaterialButton

    private lateinit var adapter: SiswaAdapter

    private var searchQuery: String = ""
    private var selectedJurusanId: Int? = null
    private var selectedWaliStaffId: Int? = null
    private var totalItemsCount = 0
    private val allStudents = mutableListOf<SiswaItem>()
    private val apiPageSize = 50
    private var generation = 0

    // Client-side pagination state
    private var currentPage = 1
    private val displayPageSize = 20
    private val displayTotalPages: Int get() = if (allStudents.isEmpty()) 1 else Math.ceil(allStudents.size.toDouble() / displayPageSize).toInt()

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
        paginationContainer = findViewById(R.id.paginationRow)
        btnPrevPage = findViewById(R.id.btnPrevPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        notifyBar = findViewById(R.id.notifyBar)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        btnNotifyWali = findViewById(R.id.btnNotifyWali)

        swipeRefresh.setOnRefreshListener {
            loadUnregisteredStudents(reset = true)
        }

        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                showCurrentPage()
                updatePagination()
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < displayTotalPages) {
                currentPage++
                showCurrentPage()
                updatePagination()
            }
        }

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val pageItems = getCurrentPageItems()
            if (pageItems.isNotEmpty()) {
                adapter.selectAll(isChecked)
            }
        }

        btnNotifyWali.setOnClickListener {
            sendNotifyWaliKelas()
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

        adapter.setOnSelectionChangedListener { count ->
            updateSelectionUI(count)
        }

        rvSiswaBaru.layoutManager = LinearLayoutManager(this)
        rvSiswaBaru.adapter = adapter
        rvSiswaBaru.isNestedScrollingEnabled = false
    }

    private fun getCurrentPageItems(): List<SiswaItem> {
        val start = (currentPage - 1) * displayPageSize
        val end = minOf(start + displayPageSize, allStudents.size)
        return if (start < allStudents.size) allStudents.subList(start, end) else emptyList()
    }

    private fun showCurrentPage() {
        val pageItems = getCurrentPageItems()
        adapter.submitList(pageItems)
        updateSelectionUI(adapter.getSelectedCount())
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            notifyBar.visibility = View.VISIBLE
            tvSelectedCount.text = getString(R.string.terpilih_x_siswa, count)
        } else {
            notifyBar.visibility = View.GONE
        }
        cbSelectAll.setOnCheckedChangeListener(null)
        cbSelectAll.isChecked = count > 0 && count == adapter.currentList.size
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val pageItems = getCurrentPageItems()
            if (pageItems.isNotEmpty()) {
                adapter.selectAll(isChecked)
            }
        }
    }

    private fun updatePagination() {
        val totalPages = displayTotalPages
        paginationContainer.removeAllViews()
        if (totalPages <= 1) {
            paginationContainer.visibility = View.GONE
            return
        }
        paginationContainer.visibility = View.VISIBLE

        if (currentPage > 1) {
            val prev = createPageButton("\u2039") {
                currentPage--
                showCurrentPage()
                updatePagination()
            }
            paginationContainer.addView(prev)
        }

        val maxVisible = 5
        var startPage = maxOf(1, currentPage - maxVisible / 2)
        val endPage = minOf(totalPages, startPage + maxVisible - 1)
        startPage = maxOf(1, endPage - maxVisible + 1)

        if (startPage > 1) {
            paginationContainer.addView(createPageButton("1") {
                currentPage = 1
                showCurrentPage()
                updatePagination()
            })
            if (startPage > 2) {
                val dots = TextView(this).apply {
                    text = "\u2026"
                    setPadding(8, 0, 8, 0)
                    gravity = android.view.Gravity.CENTER
                    textSize = 14f
                    setTextColor(getColor(R.color.text_secondary))
                }
                paginationContainer.addView(dots)
            }
        }

        for (i in startPage..endPage) {
            val btn = createPageButton(i.toString()) {
                currentPage = i
                showCurrentPage()
                updatePagination()
            }
            if (i == currentPage) {
                btn.setBackgroundResource(R.drawable.bg_pagination_active)
                btn.setTextColor(getColor(android.R.color.white))
            }
            paginationContainer.addView(btn)
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                val dots = TextView(this).apply {
                    text = "\u2026"
                    setPadding(8, 0, 8, 0)
                    gravity = android.view.Gravity.CENTER
                    textSize = 14f
                    setTextColor(getColor(R.color.text_secondary))
                }
                paginationContainer.addView(dots)
            }
            paginationContainer.addView(createPageButton(totalPages.toString()) {
                currentPage = totalPages
                showCurrentPage()
                updatePagination()
            })
        }

        if (currentPage < totalPages) {
            val next = createPageButton("\u203A") {
                currentPage++
                showCurrentPage()
                updatePagination()
            }
            paginationContainer.addView(next)
        }
    }

    private fun createPageButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(getColor(R.color.blue_theme))
            setPadding(24, 12, 24, 12)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pagination_button)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4, 0, 4, 0) }
            layoutParams = params
            setOnClickListener { onClick() }
        }
    }

    private fun initForcedClass() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        isWaliKelas = role.contains("wali") || role == "guru"
        if (isWaliKelas) {
            forcedClass = session.getString("user_kelas", "")?.takeIf { it.isNotBlank() }
                ?: session.getString("kelas", "")?.takeIf { it.isNotBlank() }
            cbSelectAll.visibility = View.GONE
            notifyBar.visibility = View.GONE
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
                        acJurusan.setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_UP) (v as AutoCompleteTextView).showDropDown()
                            false
                        }
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
                        acWaliKelas.setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_UP) (v as AutoCompleteTextView).showDropDown()
                            false
                        }
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
            generation++
        }

        showLoading(true)
        loadingJob?.cancel()

        val currentGen = generation
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
                if (!isActive) return@launch
            }

            showLoading(false)

            if (!isActive || currentGen != generation) return@launch

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
            currentPage = 1

            if (allStudents.isNotEmpty()) {
                showCurrentPage()
            } else {
                adapter.submitList(emptyList())
            }
            tvCountInfo.text = "Menampilkan ${allStudents.size} dari $totalItemsCount data"
            val empty = allStudents.isEmpty()
            tableContainer.visibility = if (empty) View.GONE else View.VISIBLE
            layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
            paginationRow.visibility = if (empty || displayTotalPages <= 1) View.GONE else View.VISIBLE
            updatePagination()
        }
    }

    private fun sendNotifyWaliKelas() {
        val selected = adapter.getSelectedItems()
        if (selected.isEmpty()) return

        val token = getAuthToken()
        if (token.isEmpty()) return

        val nisList = selected.map { it.nis }
        btnNotifyWali.isEnabled = false
        btnNotifyWali.text = "Mengirim..."

        lifecycleScope.launch {
            repository.notifyWaliKelas(token, NotifyWaliKelasRequest(nisList)).fold(
                onSuccess = { msg ->
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, msg, Toast.LENGTH_SHORT).show()
                    btnNotifyWali.isEnabled = true
                    btnNotifyWali.text = getString(R.string.kirim_notifikasi)
                    adapter.selectAll(false)
                },
                onFailure = { e ->
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    btnNotifyWali.isEnabled = true
                    btnNotifyWali.text = getString(R.string.kirim_notifikasi)
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
