package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.os.Environment
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.adapter.LaporanAbsensiAdapter
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LaporanAdminActivity : BaseAdminActivity() {

    private val repository = BerandaRepository()

    // Filter views
    private lateinit var etSearch: EditText
    private lateinit var tvTanggalAwal: TextView
    private lateinit var tvTanggalAkhir: TextView
    private lateinit var tvFilterSholat: TextView
    private lateinit var tvFilterJurusan: TextView
    private lateinit var tvFilterKelas: TextView

    // Chart views
    private lateinit var lineChartTrend: com.github.mikephil.charting.charts.LineChart
    private lateinit var progressDonutKehadiran: ProgressBar
    private lateinit var tvDonutPersen: TextView
    private lateinit var tvLegendHadir: TextView
    private lateinit var tvLegendIzin: TextView
    private lateinit var tvLegendSakit: TextView
    private lateinit var tvLegendAlpha: TextView
    private lateinit var progressBarDhuha: ProgressBar
    private lateinit var tvBarDhuha: TextView
    private lateinit var progressBarDzuhur: ProgressBar
    private lateinit var tvBarDzuhur: TextView
    private lateinit var progressBarJumat: ProgressBar
    private lateinit var tvBarJumat: TextView

    // Table views
    private lateinit var tvDataAbsensiTitle: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var recyclerAbsensi: RecyclerView

    // Pagination
    private lateinit var paginationContainer: LinearLayout

    // Buttons
    private lateinit var btnDownloadLaporan: MaterialButton
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var btnExportPdf: MaterialButton

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var absensiAdapter: LaporanAbsensiAdapter

    private var currentPageItems = listOf<AbsensiStaffItem>()

    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private var isLoading = false
    private val pageSize = 10

    private var searchJob: Job? = null

    // Filter state
    private var searchQuery: String = ""
    private var selectedSholat: String = "Semua Sholat"
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedKelas: String = "Semua Kelas"
    private var tanggalAwal: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -6) }
    private var tanggalAkhir: Calendar = Calendar.getInstance()

    // ForcedClass: for wali_kelas, auto-filter to their assigned class
    private var forcedClass: String? = null
    private var isWaliKelas: Boolean = false

    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions = listOf("Semua Kelas", "10", "11", "12")
    private val sholatOptions = listOf("Semua Sholat", "Dhuha", "Dzuhur", "Jumat")

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.LAPORAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_admin)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        initForcedClass()
        setupFilters()
        setupButtons()
        setupPagination()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadData()
            fetchChartData()
        }

        updateDateDisplay()
        loadData()

        fetchChartData()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        tvTanggalAwal = findViewById(R.id.tvTanggalAwal)
        tvTanggalAkhir = findViewById(R.id.tvTanggalAkhir)
        tvFilterSholat = findViewById(R.id.tvFilterSholat)
        tvFilterJurusan = findViewById(R.id.tvFilterJurusan)
        tvFilterKelas = findViewById(R.id.tvFilterKelas)

        lineChartTrend = findViewById(R.id.lineChartTrend)
        progressDonutKehadiran = findViewById(R.id.progressDonutKehadiran)
        tvDonutPersen = findViewById(R.id.tvDonutPersen)
        tvLegendHadir = findViewById(R.id.tvLegendHadir)
        tvLegendIzin = findViewById(R.id.tvLegendIzin)
        tvLegendSakit = findViewById(R.id.tvLegendSakit)
        tvLegendAlpha = findViewById(R.id.tvLegendAlpha)
        progressBarDhuha = findViewById(R.id.progressBarDhuha)
        tvBarDhuha = findViewById(R.id.tvBarDhuha)
        progressBarDzuhur = findViewById(R.id.progressBarDzuhur)
        tvBarDzuhur = findViewById(R.id.tvBarDzuhur)
        progressBarJumat = findViewById(R.id.progressBarJumat)
        tvBarJumat = findViewById(R.id.tvBarJumat)

        tvDataAbsensiTitle = findViewById(R.id.tvDataAbsensiTitle)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        emptyStateContainer = findViewById(R.id.emptyState)
        recyclerAbsensi = findViewById(R.id.recyclerAbsensi)

        paginationContainer = findViewById(R.id.paginationContainer)

        btnDownloadLaporan = findViewById(R.id.btnDownloadLaporan)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        btnExportPdf = findViewById(R.id.btnExportPdf)
    }

    private fun setupRecyclerView() {
        absensiAdapter = LaporanAbsensiAdapter()
        recyclerAbsensi.apply {
            layoutManager = LinearLayoutManager(this@LaporanAdminActivity)
            adapter = absensiAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPagination() {
        updatePaginationUI()
    }

    private fun initForcedClass() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = session.getString("user_role", "")?.lowercase() ?: ""
        isWaliKelas = role.contains("wali")
        if (isWaliKelas) {
            forcedClass = session.getString("user_kelas", "")?.takeIf { it.isNotBlank() }
            if (forcedClass != null) {
                selectedKelas = forcedClass!!
                tvFilterKelas.text = forcedClass!!
            }
        }
    }

    private fun setupFilters() {
        // For wali_kelas: hide Jurusan and Kelas filter dropdowns (forcedClass filtering)
        if (isWaliKelas) {
            tvFilterJurusan.visibility = View.GONE
            tvFilterKelas.visibility = View.GONE
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(400)
                    searchQuery = s?.toString()?.trim() ?: ""
                    currentPage = 1
                    loadData()
                }
            }
        })

        tvTanggalAwal.setOnClickListener {
            showDatePicker(tanggalAwal) { selectedDate ->
                tanggalAwal = selectedDate
                updateDateDisplay()
                currentPage = 1
                loadData()
                fetchChartData()
            }
        }

        tvTanggalAkhir.setOnClickListener {
            showDatePicker(tanggalAkhir) { selectedDate ->
                tanggalAkhir = selectedDate
                updateDateDisplay()
                currentPage = 1
                loadData()
                fetchChartData()
            }
        }

        val openFilter = {
            showAdvancedFilterBottomSheet()
        }

        tvFilterSholat.setOnClickListener { openFilter() }
        tvFilterJurusan.setOnClickListener { openFilter() }
        tvFilterKelas.setOnClickListener { openFilter() }
    }

    private fun showAdvancedFilterBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_filter_laporan, null)
        
        val chipGroupSholat = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupSholat)
        val chipGroupJurusan = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupJurusan)
        val chipGroupKelas = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupKelas)
        val btnApply = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApplyFilter)

        // Set current selections
        sholatOptions.forEach { opt ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = opt
            chip.isCheckable = true
            chip.id = View.generateViewId()
            if (opt == selectedSholat) {
                chip.isChecked = true
            }
            chipGroupSholat.addView(chip)
        }

        jurusanOptions.forEach { opt ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = opt
            chip.isCheckable = true
            chip.id = View.generateViewId()
            if (opt == selectedJurusan) {
                chip.isChecked = true
            }
            chipGroupJurusan.addView(chip)
        }

        kelasOptions.forEach { opt ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = opt
            chip.isCheckable = true
            chip.id = View.generateViewId()
            if (opt == selectedKelas) {
                chip.isChecked = true
            }
            chipGroupKelas.addView(chip)
        }

        btnApply.setOnClickListener {
            val checkedSholatId = chipGroupSholat.checkedChipId
            val checkedJurusanId = chipGroupJurusan.checkedChipId
            val checkedKelasId = chipGroupKelas.checkedChipId

            if (checkedSholatId != View.NO_ID) {
                selectedSholat = chipGroupSholat.findViewById<com.google.android.material.chip.Chip>(checkedSholatId).text.toString()
                tvFilterSholat.text = selectedSholat
            }
            if (checkedJurusanId != View.NO_ID) {
                selectedJurusan = chipGroupJurusan.findViewById<com.google.android.material.chip.Chip>(checkedJurusanId).text.toString()
                tvFilterJurusan.text = selectedJurusan
            }
            if (checkedKelasId != View.NO_ID) {
                selectedKelas = chipGroupKelas.findViewById<com.google.android.material.chip.Chip>(checkedKelasId).text.toString()
                tvFilterKelas.text = selectedKelas
            }

            currentPage = 1
            loadData()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun setupButtons() {
        btnExportExcel.setOnClickListener { downloadReport("excel") }
        btnExportPdf.setOnClickListener { downloadReport("pdf") }
        btnDownloadLaporan.setOnClickListener { showFormatPicker() }
    }

    // ===== CHART DATA =====

    private fun fetchChartData() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)

        lifecycleScope.launch {
            try {
                val statsResponse = RetrofitClient.apiService.getAttendanceAnalytics("Bearer $token", startDate = startDate, endDate = endDate)
                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()?.data
                    if (stats != null && ::progressDonutKehadiran.isInitialized) {
                        val hadir = stats.total_kehadiran_hari_ini
                        val izin = stats.total_izin_hari_ini
                        val sakit = stats.total_sakit_hari_ini
                        val alpha = stats.total_alpha_hari_ini
                        val total = hadir + izin + sakit + alpha
                        val persen = if (total > 0) ((hadir * 100) / total) else 0

                        progressDonutKehadiran.progress = persen
                        tvDonutPersen.text = "${persen}%"
                        tvLegendHadir.text = hadir.toString()
                        tvLegendIzin.text = izin.toString()
                        tvLegendSakit.text = sakit.toString()
                        tvLegendAlpha.text = alpha.toString()

                        val entries = listOf(
                            Entry(0f, hadir.toFloat()),
                            Entry(1f, izin.toFloat()),
                            Entry(2f, sakit.toFloat()),
                            Entry(3f, alpha.toFloat())
                        )
                        val labels = listOf("Hadir", "Izin", "Sakit", "Alpha")
                        val colors = listOf(
                            ContextCompat.getColor(this@LaporanAdminActivity, R.color.green),
                            ContextCompat.getColor(this@LaporanAdminActivity, R.color.orange_warning),
                            ContextCompat.getColor(this@LaporanAdminActivity, R.color.blue),
                            ContextCompat.getColor(this@LaporanAdminActivity, R.color.red)
                        )

                        val lineDataSet = LineDataSet(entries, "Kehadiran").apply {
                            this.colors = colors
                            setCircleColors(colors)
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawValues(true)
                            valueTextSize = 11f
                            valueTextColor = ContextCompat.getColor(this@LaporanAdminActivity, R.color.on_background)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            setDrawFilled(true)
                            fillColor = ContextCompat.getColor(this@LaporanAdminActivity, R.color.stat_blue_text)
                            fillAlpha = 30
                        }

                        lineChartTrend.apply {
                            data = LineData(lineDataSet)
                            description.isEnabled = false
                            legend.isEnabled = true
                            legend.textColor = ContextCompat.getColor(this@LaporanAdminActivity, R.color.on_background)
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                valueFormatter = object : com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels) {
                                    override fun getFormattedValue(value: Float): String {
                                        return labels.getOrNull(value.toInt()) ?: ""
                                    }
                                }
                                textColor = ContextCompat.getColor(this@LaporanAdminActivity, R.color.on_background)
                                setDrawGridLines(false)
                                granularity = 1f
                            }
                            axisLeft.apply {
                                textColor = ContextCompat.getColor(this@LaporanAdminActivity, R.color.on_background)
                                axisMinimum = 0f
                                granularity = 1f
                            }
                            axisRight.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(false)
                            setPinchZoom(false)
                            setBackgroundColor(Color.TRANSPARENT)
                            animateX(500)
                            invalidate()
                        }
                    }
                }
            } catch (e: Exception) { android.util.Log.w("LaporanAdmin", "Attendance chart error", e) }

            try {
                val chartResponse = RetrofitClient.apiService.getChartData("Bearer $token", startDate = startDate, endDate = endDate)
                if (chartResponse.isSuccessful) {
                    val chartData = chartResponse.body()?.data
                    if (chartData != null) {
                        val prayerBreakdown = chartData.getAsJsonArray("prayer_breakdown")
                        if (prayerBreakdown != null) {
                            var maxHadir = 1
                            val prayerData = mutableMapOf<String, Int>()
                            for (item in prayerBreakdown) {
                                val obj = item.asJsonObject
                                val prayer = obj.get("prayer")?.asString ?: continue
                                val hadir = obj.get("hadir")?.asInt ?: 0
                                prayerData[prayer.lowercase()] = hadir
                                if (hadir > maxHadir) maxHadir = hadir
                            }
                            val dhuha = prayerData["dhuha"] ?: 0
                            val dzuhur = prayerData["dzuhur"] ?: 0
                            val jumat = prayerData["jumat"] ?: 0

                            progressBarDhuha.max = maxHadir; progressBarDhuha.progress = dhuha; tvBarDhuha.text = "$dhuha hadir"
                            progressBarDzuhur.max = maxHadir; progressBarDzuhur.progress = dzuhur; tvBarDzuhur.text = "$dzuhur hadir"
                            progressBarJumat.max = maxHadir; progressBarJumat.progress = jumat; tvBarJumat.text = "$jumat hadir"
                        }
                    }
                }
            } catch (e: Exception) { android.util.Log.w("LaporanAdmin", "Prayer breakdown chart error", e) }
        }
    }

    // ===== DATA LOADING (PAGE-BASED) =====

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        showLoading(true)

        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val sholatApi = if (selectedSholat == "Semua Sholat") null else selectedSholat.lowercase()
        val searchApi = searchQuery.ifEmpty { null }
        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)

        lifecycleScope.launch {
            repository.getHistoryStaff(
                token = token,
                startDate = startDate,
                endDate = endDate,
                kelas = kelasApi,
                jurusan = jurusanApi,
                jenisSholat = sholatApi,
                search = searchApi,
                page = currentPage,
                limit = pageSize
            ).fold(
                onSuccess = { data ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        currentPageItems = data.absensi
                        val pagination = data.pagination
                        totalItems = pagination?.totalItems ?: currentPageItems.size
                        totalPages = pagination?.totalPages ?: 1
                        if (totalPages < 1) totalPages = 1

                        val offset = (currentPage - 1) * pageSize
                        absensiAdapter.updateData(currentPageItems, offset)

                        tvDataAbsensiTitle.text = "Data Absensi ($totalItems entri)"
                        updatePaginationUI()
                        showLoading(false)
                        isLoading = false

                        if (currentPageItems.isEmpty()) {
                            showEmptyState("Tidak ada data absensi untuk filter ini")
                        } else {
                            hideEmptyState()
                        }
                    }
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        showLoading(false)
                        isLoading = false
                        showEmptyState("Gagal memuat data: ${error.message}")
                    }
                }
            )
        }
    }

    private fun updatePaginationUI() {
        paginationContainer.removeAllViews()
        if (totalPages <= 1) return

        val maxVisible = 5
        var startPage = maxOf(1, currentPage - maxVisible / 2)
        val endPage = minOf(totalPages, startPage + maxVisible - 1)
        startPage = maxOf(1, endPage - maxVisible + 1)

        if (currentPage > 1) {
            val prevBtn = createPageButton("‹") {
                if (!isLoading) { currentPage--; loadData() }
            }
            paginationContainer.addView(prevBtn)
        }

        if (startPage > 1) {
            paginationContainer.addView(createPageButton("1") { currentPage = 1; loadData() })
            if (startPage > 2) {
                val dots = TextView(this).apply {
                    text = "…"
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
                if (!isLoading && currentPage != i) { currentPage = i; loadData() }
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
                    text = "…"
                    setPadding(8, 0, 8, 0)
                    gravity = android.view.Gravity.CENTER
                    textSize = 14f
                    setTextColor(getColor(R.color.text_secondary))
                }
                paginationContainer.addView(dots)
            }
            paginationContainer.addView(createPageButton(totalPages.toString()) { currentPage = totalPages; loadData() })
        }

        if (currentPage < totalPages) {
            val nextBtn = createPageButton("›") {
                if (!isLoading) { currentPage++; loadData() }
            }
            paginationContainer.addView(nextBtn)
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

    // ===== FORMAT PICKER (SIMPLE BOTTOM SHEET) =====

    private fun showFormatPicker() {
        val formats = arrayOf("Excel (.xlsx)", "PDF (.pdf)", "CSV (.csv)")
        AlertDialog.Builder(this)
            .setTitle("Pilih Format")
            .setItems(formats) { _, which ->
                when (which) {
                    0 -> downloadReport("excel")
                    1 -> downloadReport("pdf")
                    2 -> downloadReport("csv")
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ===== DOWNLOAD (USES CURRENT FILTERS) =====

    private fun downloadReport(format: String) {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas
        val sholatApi = if (selectedSholat == "Semua Sholat") null else selectedSholat.lowercase()
        val searchApi = searchQuery.ifEmpty { null }

        // Show active filters to user so they know what is being exported
        val activeFilters = mutableListOf<String>()
        if (jurusanApi != null) activeFilters.add("Jurusan: $selectedJurusan")
        if (kelasApi != null) activeFilters.add("Kelas: $selectedKelas")
        if (sholatApi != null) activeFilters.add("Sholat: $selectedSholat")
        if (searchApi != null) activeFilters.add("Cari: $searchQuery")
        if (activeFilters.isNotEmpty()) {
            Toast.makeText(this, "Filter aktif: ${activeFilters.joinToString(", ")}", Toast.LENGTH_LONG).show()
        }

        val btn = when (format) {
            "excel" -> btnExportExcel
            "pdf" -> btnExportPdf
            else -> btnDownloadLaporan
        }
        btn.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response: Response<ResponseBody> = when (format) {
                    "excel" -> RetrofitClient.apiService.exportAttendanceReport(
                        token = "Bearer $token", startDate = startDate, endDate = endDate,
                        jurusan = jurusanApi, kelas = kelasApi, jenisSholat = sholatApi, search = searchApi
                    )
                    "pdf" -> RetrofitClient.apiService.exportAttendanceReportPdf(
                        token = "Bearer $token", startDate = startDate, endDate = endDate,
                        jurusan = jurusanApi, kelas = kelasApi, jenisSholat = sholatApi, search = searchApi
                    )
                    "csv" -> RetrofitClient.apiService.exportAttendanceCSV(
                        token = "Bearer $token", startDate = startDate, endDate = endDate,
                        jurusan = jurusanApi, kelas = kelasApi, jenisSholat = sholatApi, search = searchApi
                    )
                    else -> RetrofitClient.apiService.exportAttendanceReport(
                        token = "Bearer $token", startDate = startDate, endDate = endDate,
                        jurusan = jurusanApi, kelas = kelasApi, jenisSholat = sholatApi, search = searchApi
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val ext = when (format) { "excel" -> "xlsx"; "pdf" -> "pdf"; "csv" -> "csv"; else -> "xlsx" }
                    val mime = when (format) {
                        "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        "pdf" -> "application/pdf"
                        "csv" -> "text/csv"
                        else -> "application/octet-stream"
                    }
                    val fileName = "Laporan_Absensi_${startDate}_$endDate.$ext"
                    
                    // Perform file I/O on IO thread
                    val fileUri = saveFileToDownloads(response.body()!!, fileName, mime)
                    
                    withContext(Dispatchers.Main) {
                        if (isFinishing || isDestroyed) return@withContext
                        btn.isEnabled = true
                        if (fileUri != null) {
                            showShareOption(fileUri, mime, fileName)
                        } else {
                            Toast.makeText(this@LaporanAdminActivity, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (isFinishing || isDestroyed) return@withContext
                        btn.isEnabled = true
                        Toast.makeText(this@LaporanAdminActivity, "Gagal mengunduh: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    btn.isEnabled = true
                    Toast.makeText(this@LaporanAdminActivity, "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showShareOption(uri: android.net.Uri, mimeType: String, fileName: String) {
        AlertDialog.Builder(this)
            .setTitle("Laporan Berhasil Diunduh")
            .setMessage("File: $fileName\n\nIngin membagikan laporan ini?")
            .setPositiveButton("Bagikan") { _, _ ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(intent, "Bagikan Laporan"))
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun saveFileToDownloads(body: ResponseBody, fileName: String, mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"): android.net.Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    body.byteStream().use { inputStream -> inputStream.copyTo(outputStream) }
                }
                uri
            }
        } catch (e: Exception) { 
            android.util.Log.e("LaporanAdminActivity", "Error saving file: ${e.message}")
            null 
        }
    }

    // ===== HELPERS =====

    private fun showDatePicker(currentDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance()
            selected.set(year, month, dayOfMonth)
            onDateSelected(selected)
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
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

    private fun updateDateDisplay() {
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvTanggalAwal.text = displayDateFormat.format(tanggalAwal.time)
        tvTanggalAkhir.text = displayDateFormat.format(tanggalAkhir.time)
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerAbsensi.visibility = View.GONE
            emptyStateContainer.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        emptyStateContainer.visibility = View.VISIBLE
        recyclerAbsensi.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateContainer.visibility = View.GONE
        recyclerAbsensi.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        searchJob?.cancel()
        super.onDestroy()
    }
}
