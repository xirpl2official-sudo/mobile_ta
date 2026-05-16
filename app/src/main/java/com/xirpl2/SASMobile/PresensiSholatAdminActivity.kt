package com.xirpl2.SASMobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.adapter.PresensiAdapter
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class PresensiSholatAdminActivity : BaseAdminActivity() {

    private val TAG = "PresensiSholatAdmin"
    private val repository = BerandaRepository()

    
    private lateinit var tvTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var filterKelas: TextView
    private lateinit var filterJurusan: TextView
    private lateinit var filterJenisSholat: TextView
    private lateinit var tvCountInfo: TextView
    private lateinit var recyclerPresensi: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView

    
    private lateinit var presensiAdapter: PresensiAdapter


    
    
    private var selectedKelas: String = "Semua Kelas"
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedJenisSholat: String = "Semua Sholat"
    private var searchQuery: String = ""
    
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 500L

    
    private var currentPage = 1
    private val limit = 20
    private var isLoading = false
    private var isLastPage = false
    private val dataList = mutableListOf<AbsensiStaffItem>()

    
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "X", "XI", "XII")
    private val jenisSholatOptions: List<String> = listOf("Semua Sholat", "Dhuha", "Dzuhur", "Jumat")

    
    private fun getJenisSholatApiValue(displayValue: String): String? {
        return when (displayValue) {
            "Dhuha" -> "Dhuha"
            "Dzuhur" -> "Dzuhur"
            "Jumat" -> "Jumat"
            else -> null
        }
    }

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.PRESENSI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presensi_sholat_admin)
        setupStatusBar()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupInputIzinButton()

        
        loadData()
    }

    private fun setupInputIzinButton() {
        val btnInputIzin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInputIzin)
        val btnTambah = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTambah)
        
        
        val role = getSharedPreferences("UserData", MODE_PRIVATE).getString("user_role", "")?.lowercase() ?: ""
        
        when {
            role == "wali_kelas" || role == "wali kelas" -> {
                btnInputIzin.visibility = View.VISIBLE
                btnTambah.visibility = View.GONE
            }
            role == "admin" -> {
                btnInputIzin.visibility = View.GONE
                btnTambah.visibility = View.VISIBLE
            }
            else -> { 
                btnInputIzin.visibility = View.GONE
                btnTambah.visibility = View.GONE
            }
        }
        
        btnInputIzin.setOnClickListener {
            val dialog = InputIzinDialogFragment.newInstance {
                
                refreshData()
            }
            dialog.show(supportFragmentManager, "InputIzinDialog")
        }

        btnTambah.setOnClickListener {
            val dialog = TambahAbsensiDialogFragment.newInstance {
                
                refreshData()
            }
            dialog.show(supportFragmentManager, "TambahAbsensiDialog")
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etSearch = findViewById(R.id.etSearch)
        filterKelas = findViewById(R.id.filterKelas)
        filterJurusan = findViewById(R.id.filterJurusan)
        filterJenisSholat = findViewById(R.id.filterJenisSholat)
        tvCountInfo = findViewById(R.id.tvCountInfo)
        recyclerPresensi = findViewById(R.id.recyclerPresensi)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupRecyclerView() {
        presensiAdapter = PresensiAdapter()
        val layoutManager = LinearLayoutManager(this@PresensiSholatAdminActivity)
        recyclerPresensi.apply {
            this.layoutManager = layoutManager
            adapter = presensiAdapter
            
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= limit
                        ) {
                            loadMoreData()
                        }
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                
                
                searchRunnable = Runnable {
                    val newQuery = s?.toString()?.trim() ?: ""
                    if (newQuery != searchQuery) {
                        searchQuery = newQuery
                        
                        refreshData()
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, searchDebounceMs)
            }
        })
    }

    private fun refreshData() {
        currentPage = 1
        isLastPage = false
        dataList.clear()
        presensiAdapter.clearData() 
        loadData()
    }

    private fun loadMoreData() {
        currentPage++
        loadData()
    }

    private fun setupFilters() {
        filterKelas.setOnClickListener {
            showFilterDialog("Pilih Kelas", kelasOptions, selectedKelas) { selected ->
                selectedKelas = selected
                filterKelas.text = selected
                refreshData()
            }
        }
        
        filterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                filterJurusan.text = selected
                refreshData()
            }
        }
        
        filterJenisSholat.setOnClickListener {
            showFilterDialog("Pilih Jenis Sholat", jenisSholatOptions, selectedJenisSholat) { selected ->
                selectedJenisSholat = selected
                filterJenisSholat.text = selected
                refreshData()
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

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            showEmptyState("Silakan login terlebih dahulu")
            return
        }

        if (isLoading) return
        isLoading = true
        
        if (currentPage == 1) {
            showLoading(true)
        } else {
            
        }

        
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val jenisSholatApi = getJenisSholatApiValue(selectedJenisSholat)
        val searchApi = if (searchQuery.isBlank()) null else searchQuery
        
        
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        lifecycleScope.launch {
            repository.getHistoryStaff(
                token = token,
                kelas = kelasApi,
                jurusan = jurusanApi,
                jenisSholat = jenisSholatApi,
                search = searchApi,
                tanggal = todayDate,
                page = currentPage,
                limit = limit
            ).fold(
                onSuccess = { data ->
                    val items = data.absensi
                    val pagination = data.pagination

                    if (currentPage == 1) {
                        dataList.clear()
                    }
                    dataList.addAll(items)
                    presensiAdapter.updateData(dataList)

                    isLastPage = pagination?.let {
                        it.page >= it.total_pages
                    } ?: (items.size < limit)

                    val totalItems = pagination?.total_items ?: dataList.size
                    tvCountInfo.text = "Total: $totalItems data"

                    if (dataList.isEmpty()) {
                        showEmptyState("Tidak ada data presensi")
                    } else {
                        tvEmptyState.visibility = View.GONE
                        recyclerPresensi.visibility = View.VISIBLE
                    }
                },
                onFailure = { error ->
                    isLoading = false
                    showLoading(false)
                    if (currentPage == 1) {
                        showEmptyState("Gagal memuat data: ${error.message}")
                    }
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerPresensi.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.GONE
        } else {
            findViewById<View>(R.id.tableHorizontalScrollView).visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        recyclerPresensi.visibility = View.GONE
    }
}

