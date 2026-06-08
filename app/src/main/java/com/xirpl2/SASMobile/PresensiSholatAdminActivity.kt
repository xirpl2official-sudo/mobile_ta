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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var filterTanggal: TextView
    private lateinit var filterKelas: TextView
    private lateinit var filterJurusan: TextView
    private lateinit var filterJenisSholat: TextView
    private lateinit var tvCountInfo: TextView
    private lateinit var recyclerPresensi: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView

    
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var presensiAdapter: PresensiAdapter
    private lateinit var tableHorizontalScrollView: View


    
    
    private var selectedKelas: String = "Semua Kelas"
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedJenisSholat: String = "Semua Sholat"
    private var selectedTanggal: String = "Semua Tanggal"
    private var searchQuery: String = ""

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
    private var forcedClass: String? = null
    private var isWaliKelas: Boolean = false
    
    
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
    private val kelasOptions: List<String> = listOf("Semua Kelas", "10", "11", "12")
    private val jenisSholatOptions: List<String> = listOf("Semua Shalat", "Duha", "Dzuhur", "Jumat")
    private val tanggalOptions: List<String> = listOf("Semua Tanggal", "Hari Ini", "Minggu Ini", "Bulan Ini")

    private fun getDateRange(): Pair<String?, String?> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        return when (selectedTanggal) {
            "Hari Ini" -> {
                val today = sdf.format(cal.time)
                today to today
            }
            "Minggu Ini" -> {
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = sdf.format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
                val end = sdf.format(cal.time)
                start to end
            }
            "Bulan Ini" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(cal.time)
                cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                val end = sdf.format(cal.time)
                start to end
            }
            else -> null to null
        }
    }

    
    private fun getJenisSholatApiValue(displayValue: String): String? {
        return when (displayValue) {
            "Duha" -> "Duha"
            "Dzuhur" -> "Dzuhur"
            "Jumat" -> "Jumat"
            else -> null
        }
    }

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.PRESENSI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_presensi_sholat_admin)
            setupStatusBar()

            val topBarContent = findViewById<View>(R.id.topBarContent)
            if (topBarContent != null) {
                applyEdgeToEdge(topBarContent)
            }

            initViews()
            setupDrawerAndSidebar()
            setupMenuIcon()
            setupRecyclerView()
            setupSearch()
            initForcedClass()
            setupFilters()
            setupInputIzinButton()

            
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing) {
                    try {
                        loadData()
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error in delayed loadData: ${e.message}", e)
                    }
                }
            }, 100)

            // Initialize swipe-to-refresh after content is set up
            swipeRefresh = findViewById(R.id.swipeRefresh)
            swipeRefresh.setOnRefreshListener { refreshData() }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Terjadi kesalahan saat memuat halaman", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupInputIzinButton() {
        val btnInputIzin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInputIzin)
        if (btnInputIzin != null) {
            btnInputIzin.visibility = View.GONE
        }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle) ?: TextView(this)
        etSearch = findViewById(R.id.etSearch) ?: EditText(this)
        filterTanggal = findViewById(R.id.filterTanggal) ?: TextView(this)
        filterKelas = findViewById(R.id.filterKelas) ?: TextView(this)
        filterJurusan = findViewById(R.id.filterJurusan) ?: TextView(this)
        filterJenisSholat = findViewById(R.id.filterJenisSholat) ?: TextView(this)
        tvCountInfo = findViewById(R.id.tvCountInfo) ?: TextView(this)
        recyclerPresensi = findViewById(R.id.recyclerPresensi) ?: RecyclerView(this)
        progressLoading = findViewById(R.id.progressLoading) ?: ProgressBar(this)
        tvEmptyState = findViewById(R.id.tvEmptyState) ?: TextView(this)
        tableHorizontalScrollView = findViewById(R.id.tableHorizontalScrollView) ?: View(this)
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

    private fun initForcedClass() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        isWaliKelas = role.contains("wali")
        if (isWaliKelas) {
            forcedClass = session.getString("user_kelas", "")?.takeIf { it.isNotBlank() }
            if (forcedClass != null) {
                // Auto-set the kelas filter to the wali_kelas's assigned class
                selectedKelas = forcedClass!!
                filterKelas.text = forcedClass!!
            }
        }
    }

    private fun setupFilters() {
        if (isWaliKelas) {
            filterKelas.visibility = View.GONE
            filterJurusan.visibility = View.GONE
        }

        filterTanggal.setOnClickListener {
            showFilterDialog("Pilih Tanggal", tanggalOptions, selectedTanggal) { selected ->
                selectedTanggal = selected
                filterTanggal.text = selected
                refreshData()
            }
        }

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
        if (isFinishing || isDestroyed) return

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
        val (startDate, endDate) = getDateRange()

        lifecycleScope.launch {
            repository.getHistoryStaff(
                token = token,
                kelas = kelasApi,
                jurusan = jurusanApi,
                jenisSholat = jenisSholatApi,
                search = searchApi,
                startDate = startDate,
                endDate = endDate,
                page = currentPage,
                limit = limit
            ).fold(
                onSuccess = { data ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    if (isFinishing || isDestroyed) return@fold
                    isLoading = false
                    showLoading(false)
                    val items = data.absensi
                    val pagination = data.pagination

                    if (currentPage == 1) {
                        dataList.clear()
                    }
                    dataList.addAll(items ?: emptyList())
                    presensiAdapter.updateData(dataList)

                    isLastPage = pagination?.let {
                        it.page >= it.totalPages
                    } ?: ((items?.size ?: 0) < limit)

                    val totalItems = pagination?.totalItems ?: dataList.size
                    tvCountInfo.text = "Total: $totalItems data"

                    if (dataList.isEmpty()) {
                        showEmptyState("Tidak ada data presensi")
                    } else {
                        tvEmptyState.visibility = View.GONE
                        recyclerPresensi.visibility = View.VISIBLE
                        tableHorizontalScrollView.visibility = View.VISIBLE
                    }
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    if (isFinishing || isDestroyed) return@fold
                    isLoading = false
                    showLoading(false)
                    if (currentPage == 1) {
                        showEmptyState("Gagal memuat data: ${error.message}")
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        searchHandler.removeCallbacksAndMessages(null)
        if (::recyclerPresensi.isInitialized) {
            recyclerPresensi.adapter = null
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (::recyclerPresensi.isInitialized) {
                recyclerPresensi.clearOnScrollListeners()
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

    private fun showLoading(show: Boolean) {
        safeUIUpdate {
            progressLoading.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                recyclerPresensi.visibility = View.GONE
                findViewById<View>(R.id.emptyStateContainer)?.visibility = View.GONE
                tableHorizontalScrollView.visibility = View.GONE
            } else {
                tableHorizontalScrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun showEmptyState(message: String) {
        safeUIUpdate {
            tvEmptyState.text = message
            findViewById<View>(R.id.emptyStateContainer)?.visibility = View.VISIBLE
            recyclerPresensi.visibility = View.GONE
            tableHorizontalScrollView.visibility = View.GONE
        }
    }
}

