package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.adapter.PengajuanIzinAdminAdapter
import com.xirpl2.SASMobile.model.PengajuanIzin
import com.xirpl2.SASMobile.repository.PengajuanIzinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PengajuanIzinAdminActivity : BaseAdminActivity() {

    private lateinit var rvPengajuanIzin: RecyclerView
    private lateinit var adapter: PengajuanIzinAdminAdapter
    private lateinit var spinnerFilterStatus: Spinner
    private lateinit var spinnerSort: Spinner
    private lateinit var searchView: SearchView
    private lateinit var tvInfoBanner: TextView
    private lateinit var infoBanner: View
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: View
    private lateinit var btnLoadMore: Button
    private lateinit var btnClose: ImageView
    private lateinit var fabHelp: View

    private val repository = PengajuanIzinRepository()
    
    // State Management
    private var currentStatusFilter: String? = null
    private var currentSort: String = "terbaru"
    private var searchQuery: String = ""
    private var currentPage = 1
    private val limit = 10
    private var totalItems = 0
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var dataJob: Job? = null
    
    private val allData = mutableListOf<PengajuanIzin>()

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.PENGAJUAN_IZIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_izin_admin)
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

        loadData(reset = true)
    }

    private fun initializeViews() {
        rvPengajuanIzin = findViewById(R.id.rvPengajuanIzin)
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus)
        spinnerSort = findViewById(R.id.spinnerSort)
        searchView = findViewById(R.id.searchView)
        tvInfoBanner = findViewById(R.id.tvInfoBanner)
        infoBanner = findViewById(R.id.infoBanner)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        btnLoadMore = findViewById(R.id.btnLoadMore)
        btnClose = findViewById(R.id.btnClose)
        fabHelp = findViewById(R.id.fabHelp)
    }

    private fun setupRecyclerView() {
        adapter = PengajuanIzinAdminAdapter(
            onApprove = { item -> showConfirmApprove(item) },
            onReject = { item -> showConfirmReject(item) },
            onDetail = { item -> showDetail(item) }
        )
        rvPengajuanIzin.layoutManager = LinearLayoutManager(this)
        rvPengajuanIzin.adapter = adapter
    }

    private fun setupFilters() {
        spinnerFilterStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString().lowercase()
                val mappedStatus = when (selected) {
                    "semua" -> null
                    "pending" -> "pending"
                    "approved" -> "disetujui"
                    "rejected" -> "ditolak"
                    else -> null
                }
                if (currentStatusFilter != mappedStatus) {
                    currentStatusFilter = mappedStatus
                    loadData(reset = true)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString().lowercase().replace(" ", "_")
                if (currentSort != selected) {
                    currentSort = selected
                    loadData(reset = true)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        btnClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnLoadMore.setOnClickListener {
            currentPage++
            loadData(reset = false)
        }

        fabHelp.setOnClickListener {
            showHelpDialog()
        }
        
        findViewById<Button>(R.id.btnResetFilter)?.setOnClickListener {
            resetFilters()
        }
    }

    private fun resetFilters() {
        searchView.setQuery("", false)
        spinnerFilterStatus.setSelection(0)
        spinnerSort.setSelection(0)
        currentStatusFilter = null
        currentSort = "terbaru"
        searchQuery = ""
        loadData(reset = true)
    }

    private fun updateResetButtonVisibility() {
        val btnResetFilter = findViewById<Button>(R.id.btnResetFilter)
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
            repository.getPengajuanIzinList(
                token,
                currentStatusFilter,
                currentPage,
                limit
            ).fold(
                onSuccess = { response ->
                    val newData = response.data
                    allData.addAll(newData)
                    totalItems = response.pagination?.totalItems ?: allData.size

                    val filteredData = if (searchQuery.isNotEmpty()) {
                        val query = searchQuery.lowercase()
                        allData.filter { item ->
                            val nama = item.siswa?.nama_siswa?.lowercase() ?: ""
                            val nis = item.siswa?.nis?.lowercase() ?: ""
                            val kelas = item.siswa?.kelas?.lowercase() ?: ""
                            val keterangan = item.keterangan?.lowercase() ?: ""
                            val jenis = item.jenisIzin?.lowercase() ?: ""
                            nama.contains(query) || nis.contains(query) ||
                                kelas.contains(query) || keterangan.contains(query) ||
                                jenis.contains(query)
                        }
                    } else {
                        allData
                    }

                    adapter.submitList(filteredData.toList())
                    updateInfoBanner(filteredData.size)

                    emptyState.visibility = if (filteredData.isEmpty()) View.VISIBLE else View.GONE
                    btnLoadMore.visibility = if (allData.size < totalItems && searchQuery.isEmpty()) View.VISIBLE else View.GONE
                    btnLoadMore.isEnabled = true
                    btnLoadMore.text = "Muat Lebih Banyak"

                    setLoading(false)
                },
                onFailure = { error ->
                    Toast.makeText(this@PengajuanIzinAdminActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
            tvInfoBanner.text = "Menampilkan $filteredCount dari $totalItems pengajuan (${parts.joinToString(", ")})"
        } else {
            infoBanner.visibility = if (totalCount > 0) View.VISIBLE else View.GONE
            tvInfoBanner.text = "Menampilkan $totalCount pengajuan"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            emptyState.visibility = View.GONE
            rvPengajuanIzin.visibility = View.GONE
        } else {
            rvPengajuanIzin.visibility = View.VISIBLE
        }
    }

    private fun showConfirmApprove(item: PengajuanIzin) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Setuju")
            .setMessage("Yakin ingin menyetujui pengajuan ini dari ${item.siswa?.nama_siswa}?")
            .setPositiveButton("Ya, Lanjutkan") { _, _ ->
                updateStatus(item.id_pengajuan, "disetujui", null)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showConfirmReject(item: PengajuanIzin) {
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
                    updateStatus(item.id_pengajuan, "ditolak", reason)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateStatus(id: Int, status: String, catatan: String?) {
        val token = getAuthToken()
        lifecycleScope.launch {
            repository.updatePengajuanIzinStatus(token, id, status, catatan).fold(
                onSuccess = { message ->
                    Toast.makeText(this@PengajuanIzinAdminActivity, "Berhasil: $message", Toast.LENGTH_SHORT).show()
                    loadData(reset = true)
                },
                onFailure = { error ->
                    Toast.makeText(this@PengajuanIzinAdminActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showDetail(item: PengajuanIzin) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            // Fetch detail first
            val detailResult = repository.getPengajuanIzinDetail(token, item.id_pengajuan)
            val fullItem = detailResult.getOrNull() ?: item
            
            // If buktiFoto is still null, try fetching it explicitly from the separate endpoint
            var finalItem = fullItem
            if (fullItem.buktiFoto.isNullOrEmpty()) {
                val buktiResult = repository.getBuktiFoto(token, item.id_pengajuan)
                buktiResult.onSuccess { url ->
                    if (!url.isNullOrEmpty()) {
                        finalItem = fullItem.copy(buktiFoto = url)
                    }
                }
            }
            
            displayDetailDialog(finalItem)
            
            if (detailResult.isFailure) {
                Toast.makeText(this@PengajuanIzinAdminActivity, "Gagal memuat detail lengkap: ${detailResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayDetailDialog(item: PengajuanIzin) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detail_pengajuan, null)
        
        dialogView.findViewById<TextView>(R.id.tvDetailSiswa).text = "Siswa: ${item.siswa?.nama_siswa}"
        dialogView.findViewById<TextView>(R.id.tvDetailNIS).text = "NIS: ${item.siswa?.nis}"
        dialogView.findViewById<TextView>(R.id.tvDetailJenis).text = "Jenis: ${item.jenisIzin}"
        dialogView.findViewById<TextView>(R.id.tvDetailPeriode).text = "Periode: ${item.tanggalAwal} - ${item.tanggalAkhir}"
        dialogView.findViewById<TextView>(R.id.tvDetailAlasan).text = item.keterangan
        dialogView.findViewById<TextView>(R.id.tvDetailStatus).text = "Status: ${item.status}"
        
        val sectionLampiran = dialogView.findViewById<LinearLayout>(R.id.sectionLampiran)
        if (!item.buktiFoto.isNullOrEmpty()) {
            sectionLampiran.visibility = View.VISIBLE
            val tvFileName = dialogView.findViewById<TextView>(R.id.tvFileName)
            val ivFileThumbnail = dialogView.findViewById<ImageView>(R.id.ivFileThumbnail)
            val btnViewPreview = dialogView.findViewById<ImageButton>(R.id.btnViewPreview)
            val btnDownloadFile = dialogView.findViewById<ImageButton>(R.id.btnDownloadFile)

            // Extract filename from URL
            val fileName = item.buktiFoto.substringAfterLast("/")
            tvFileName.text = fileName

            // Basic type detection for icon
            val extension = fileName.substringAfterLast(".", "").lowercase()
            if (extension == "jpg" || extension == "jpeg" || extension == "png" || extension == "webp") {
                ivFileThumbnail.setImageResource(R.drawable.ic_profile) // Placeholder for image
                ivFileThumbnail.setColorFilter(getColor(R.color.blue_500))
            } else if (extension == "pdf") {
                ivFileThumbnail.setImageResource(R.drawable.ic_description)
                ivFileThumbnail.setColorFilter(getColor(R.color.red))
            } else {
                ivFileThumbnail.setImageResource(R.drawable.ic_description)
                ivFileThumbnail.setColorFilter(getColor(R.color.slate_500))
            }

            btnViewPreview.setOnClickListener {
                openFile(item.buktiFoto)
            }

            btnDownloadFile.setOnClickListener {
                openFile(item.buktiFoto)
                Toast.makeText(this, "Membuka lampiran...", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun openFile(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        searchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bantuan")
            .setMessage("Gunakan kolom pencarian untuk mencari siswa.\nFilter status untuk mempermudah verifikasi.\nTekan 'Selengkapnya' pada alasan untuk membaca detail.")
            .setPositiveButton("Mengerti", null)
            .show()
    }
}
