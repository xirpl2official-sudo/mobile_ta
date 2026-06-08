package com.xirpl2.SASMobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.adapter.FemaleRestrictionApprovalAdapter
import com.xirpl2.SASMobile.model.ApprovalRequest
import com.xirpl2.SASMobile.repository.FemaleRestrictionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FemaleRestrictionApprovalActivity : BaseAdminActivity() {

    private lateinit var rvApprovalRequests: RecyclerView
    private lateinit var adapter: FemaleRestrictionApprovalAdapter
    private lateinit var spinnerFilterStatus: TextView
    private lateinit var spinnerSort: TextView
    private lateinit var searchView: SearchView
    private lateinit var tvInfoBanner: TextView
    private lateinit var infoBanner: View
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var btnLoadMore: com.google.android.material.button.MaterialButton
    private lateinit var fabHelp: View

    private val repository = FemaleRestrictionRepository()

    private var currentStatusFilter: String? = null
    private var currentSort: String = "terbaru"
    private var searchQuery: String = ""
    private var currentPage = 1
    private val limit = 10
    private var totalItems = 0

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var dataJob: Job? = null

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val allData = mutableListOf<ApprovalRequest>()

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.PENGAJUAN_IZIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_female_restriction_approval)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupActions()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadData(reset = true) }

        loadData(reset = true)
    }

    private fun initializeViews() {
        rvApprovalRequests = findViewById(R.id.rvApprovalRequests)
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus)
        spinnerSort = findViewById(R.id.spinnerSort)
        searchView = findViewById(R.id.searchView)
        tvInfoBanner = findViewById(R.id.tvInfoBanner)
        infoBanner = findViewById(R.id.infoBanner)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        fabHelp = findViewById(R.id.fabHelp)
    }

    private fun setupRecyclerView() {
        adapter = FemaleRestrictionApprovalAdapter(
            onApprove = { item -> showConfirmApprove(item) },
            onReject = { item -> showConfirmReject(item) },
            onDetail = { item -> showDetail(item) }
        )
        rvApprovalRequests.layoutManager = LinearLayoutManager(this)
        rvApprovalRequests.adapter = adapter
    }

    private fun setupFilters() {
        val statusOptions = arrayOf("Semua Status", "Pending", "Disetujui", "Ditolak")
        spinnerFilterStatus.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Filter Status")
                .setSingleChoiceItems(statusOptions, -1) { dialog, which ->
                    val selected = statusOptions[which].lowercase()
                    val mappedStatus = when (selected) {
                        "semua status" -> null
                        "pending" -> "pending"
                        "disetujui" -> "disetujui"
                        "ditolak" -> "ditolak"
                        else -> null
                    }
                    spinnerFilterStatus.text = statusOptions[which]
                    if (currentStatusFilter != mappedStatus) {
                        currentStatusFilter = mappedStatus
                        loadData(reset = true)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        val sortOptions = arrayOf("Terbaru", "Terlama")
        spinnerSort.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Urutkan")
                .setSingleChoiceItems(sortOptions, -1) { dialog, which ->
                    val selected = sortOptions[which].lowercase()
                    spinnerSort.text = sortOptions[which]
                    if (currentSort != selected) {
                        currentSort = selected
                        loadData(reset = true)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                loadData(reset = true)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = newText ?: ""
                    loadData(reset = true)
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
                return true
            }
        })
    }

    private fun setupActions() {
        btnLoadMore.setOnClickListener {
            currentPage++
            loadData(reset = false)
        }

        fabHelp.setOnClickListener {
            showHelpDialog()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnResetFilter)?.setOnClickListener {
            resetFilters()
        }
    }

    private fun resetFilters() {
        searchView.setQuery("", false)
        spinnerFilterStatus.text = "Semua Status"
        spinnerSort.text = "Terbaru"
        currentStatusFilter = null
        currentSort = "terbaru"
        searchQuery = ""
        loadData(reset = true)
    }

    private fun updateResetButtonVisibility() {
        val btnResetFilter = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnResetFilter)
        val isFilterChanged = currentStatusFilter != null || currentSort != "terbaru" || searchQuery.isNotEmpty()
        btnResetFilter?.visibility = if (isFilterChanged) View.VISIBLE else View.GONE
    }

    private fun loadData(reset: Boolean) {
        updateResetButtonVisibility()
        val token = getAuthToken()
        if (token.isEmpty()) return

        if (reset) {
            currentPage = 1
            allData.clear()
            setLoading(true)
        } else {
            btnLoadMore.isEnabled = false
            btnLoadMore.text = "Memuat..."
        }

        dataJob?.cancel()
        dataJob = lifecycleScope.launch {
            repository.getPendingApprovals(token).fold(
                onSuccess = { rawData ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false

                    val sortedData = when (currentSort) {
                        "terlama" -> rawData.reversed()
                        else -> rawData
                    }

                    allData.clear()
                    allData.addAll(sortedData)
                    totalItems = sortedData.size

                    val filteredData = if (searchQuery.isNotEmpty()) {
                        val query = searchQuery.lowercase()
                        allData.filter { item ->
                            val nama = item.siswaName?.lowercase() ?: ""
                            val nis = item.siswaNis?.lowercase() ?: ""
                            val kelas = item.siswaKelas?.lowercase() ?: ""
                            nama.contains(query) || nis.contains(query) || kelas.contains(query)
                        }
                    } else {
                        allData
                    }

                    val statusFiltered = if (currentStatusFilter != null) {
                        filteredData.filter { it.status.lowercase() == currentStatusFilter }
                    } else {
                        filteredData
                    }

                    adapter.submitList(statusFiltered.toList())
                    updateInfoBanner(statusFiltered.size)

                    emptyState.visibility = if (statusFiltered.isEmpty()) View.VISIBLE else View.GONE
                    rvApprovalRequests.visibility = if (statusFiltered.isNotEmpty()) View.VISIBLE else View.GONE
                    btnLoadMore.visibility = View.GONE
                    btnLoadMore.isEnabled = true
                    btnLoadMore.text = "Muat Lebih Banyak"

                    setLoading(false)
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    Toast.makeText(this@FemaleRestrictionApprovalActivity, "Kesalahan: ${error.message}", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                }
            )
        }
    }

    private fun updateInfoBanner(filteredCount: Int = allData.size) {
        val totalCount = allData.size
        if (searchQuery.isNotEmpty() || currentStatusFilter != null) {
            infoBanner.visibility = View.VISIBLE
            val parts = mutableListOf<String>()
            if (currentStatusFilter != null) {
                parts.add("Status: ${currentStatusFilter?.uppercase()}")
            }
            if (searchQuery.isNotEmpty()) {
                parts.add("Pencarian: \"$searchQuery\"")
            }
            tvInfoBanner.text = "Menampilkan $filteredCount dari $totalCount pengajuan (${parts.joinToString(", ")})"
        } else {
            infoBanner.visibility = if (totalCount > 0) View.VISIBLE else View.GONE
            tvInfoBanner.text = "Menampilkan $totalCount pengajuan"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            emptyState.visibility = View.GONE
            rvApprovalRequests.visibility = View.GONE
        } else {
            rvApprovalRequests.visibility = View.VISIBLE
        }
    }

    private fun showConfirmApprove(item: ApprovalRequest) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Setuju")
            .setMessage("Yakin ingin menyetujui pengajuan dari ${item.siswaName}?")
            .setPositiveButton("Ya, Lanjutkan") { _, _ ->
                processApproval(item.id, "disetujui", null)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showConfirmReject(item: ApprovalRequest) {
        val input = EditText(this)
        input.hint = "Alasan penolakan (Wajib)..."
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(48, 16, 48, 16)
        input.layoutParams = params
        container.addView(input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Tolak")
            .setMessage("Yakin ingin menolak pengajuan ini? Alasan penolakan wajib diisi.")
            .setView(container)
            .setPositiveButton("Ya, Lanjutkan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Alasan penolakan wajib diisi!", Toast.LENGTH_SHORT).show()
                } else {
                    processApproval(item.id, "ditolak", reason)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun processApproval(id: Int, status: String, catatan: String?) {
        val token = getAuthToken()
        lifecycleScope.launch {
            repository.processApproval(token, id, status, catatan).fold(
                onSuccess = { message ->
                    Toast.makeText(this@FemaleRestrictionApprovalActivity, "Berhasil: $message", Toast.LENGTH_SHORT).show()
                    loadData(reset = true)
                },
                onFailure = { error ->
                    Toast.makeText(this@FemaleRestrictionApprovalActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showDetail(item: ApprovalRequest) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_female_restriction_detail, null)

        dialogView.findViewById<TextView>(R.id.tvDetailName).text = item.siswaName ?: "--"
        dialogView.findViewById<TextView>(R.id.tvDetailNis).text = item.siswaNis ?: "--"
        dialogView.findViewById<TextView>(R.id.tvDetailKelas).text = item.siswaKelas ?: "--"
        dialogView.findViewById<TextView>(R.id.tvDetailRequestDate).text = formatDisplayDate(item.createdAt)
        dialogView.findViewById<TextView>(R.id.tvDetailExpires).text = item.expiresAt?.let { formatDisplayDate(it) } ?: "--"

        val statusLower = item.status.lowercase()
        val statusText = when (statusLower) {
            "pending" -> "Menunggu"
            "disetujui", "approved" -> "Disetujui"
            "ditolak", "rejected" -> "Ditolak"
            else -> item.status
        }
        dialogView.findViewById<TextView>(R.id.tvDetailStatus).text = statusText

        val layoutApprover = dialogView.findViewById<LinearLayout>(R.id.layoutApprover)
        if (!item.approverName.isNullOrEmpty()) {
            layoutApprover.visibility = View.VISIBLE
            dialogView.findViewById<TextView>(R.id.tvDetailApprover).text = item.approverName
        }

        val layoutCatatan = dialogView.findViewById<LinearLayout>(R.id.layoutCatatan)
        if (!item.catatan.isNullOrEmpty()) {
            layoutCatatan.visibility = View.VISIBLE
            dialogView.findViewById<TextView>(R.id.tvDetailCatatan).text = item.catatan
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun formatDisplayDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("T").first().split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                val monthNames = arrayOf(
                    "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )
                "$day ${monthNames[month]} $year"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroy() {
        dataJob?.cancel()
        searchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bantuan")
            .setMessage("Halaman ini menampilkan pengajuan persetujuan halangan dari siswi.\n\nGunakan kolom pencarian untuk mencari berdasarkan nama atau NIS.\nFilter status untuk mempermudah verifikasi.\nTekan tombol 'Setuju' atau 'Tolak' untuk memproses pengajuan.")
            .setPositiveButton("Mengerti", null)
            .show()
    }
}
