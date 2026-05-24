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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var btnNotifyBulk: com.google.android.material.button.MaterialButton
    
    private lateinit var adapter: SiswaAdapter
    
    private var searchQuery: String = ""
    private var selectedJurusanId: Int? = null
    private var selectedWaliStaffId: Int? = null

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
    private var forcedClass: String? = null
    private var forcedClassId: Int? = null
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

        swipeRefresh.setOnRefreshListener {
            loadUnregisteredStudents()
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
            onEditClick = {},
            onDeleteClick = {},
            onDetailClick = { siswa ->
                val dialog = SiswaDetailDialogFragment.newInstance(siswa)
                dialog.show(supportFragmentManager, "SiswaDetail")
            },
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
        Toast.makeText(this, "Berhasil mengirim pengingat ke ${selected.size} siswa", Toast.LENGTH_SHORT).show()
        adapter.selectAll(false)
        cbSelectAll.isChecked = false
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
                        loadUnregisteredStudents()
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs)
            }
        })

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
                } catch (e: Exception) { }
            }

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
                } catch (e: Exception) { }
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
                if (forcedClass != null && forcedClassId == null) {
                    try {
                        val kelasResponse = RetrofitClient.apiService.getKelasLookup("Bearer $token")
                        if (kelasResponse.isSuccessful) {
                            val kelasList = kelasResponse.body()?.data ?: emptyList()
                            val match = kelasList.find { "${it.tingkatan}${it.part}" == forcedClass }
                            forcedClassId = match?.id_kelas
                        }
                    } catch (_: Exception) { }
                }

                val response = RetrofitClient.apiService.getUnregisteredStudents(
                    token = "Bearer $token",
                    page = 1,
                    pageSize = 100,
                    search = if (searchQuery.isNotEmpty()) searchQuery else null,
                    jurusan = selectedJurusanId?.toString(),
                    waliKelas = selectedWaliStaffId?.toString(),
                    idKelas = forcedClassId
                )

                runOnUiThread {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val students = body?.data ?: emptyList()
                        adapter.submitList(students)

                        val total = body?.pagination?.total_items ?: students.size
                        tvCountInfo.text = "Menampilkan ${students.size} dari $total data"

                        layoutEmpty.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
