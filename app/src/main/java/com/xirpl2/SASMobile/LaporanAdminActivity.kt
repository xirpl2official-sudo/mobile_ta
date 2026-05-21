package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var recyclerAbsensi: RecyclerView

    // Pagination views
    private lateinit var btnPrevPage: MaterialButton
    private lateinit var tvPageInfo: TextView
    private lateinit var btnNextPage: MaterialButton

    // Buttons
    private lateinit var btnDownloadLaporan: MaterialButton
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var btnExportPdf: MaterialButton

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

    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions = listOf("Semua Kelas", "10", "11", "12")
    private val sholatOptions = listOf("Semua Sholat", "Dhuha", "Dzuhur", "Jumat")

    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        setupFilters()
        setupButtons()
        setupPagination()

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
        recyclerAbsensi = findViewById(R.id.recyclerAbsensi)

        btnPrevPage = findViewById(R.id.btnPrevPage)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        btnNextPage = findViewById(R.id.btnNextPage)

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
        btnPrevPage.setOnClickListener {
            if (currentPage > 1 && !isLoading) {
                currentPage--
                loadData()
            }
        }
        btnNextPage.setOnClickListener {
            if (currentPage < totalPages && !isLoading) {
                currentPage++
                loadData()
            }
        }
        updatePaginationUI()
    }

    private fun setupFilters() {
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
            }
        }

        tvTanggalAkhir.setOnClickListener {
            showDatePicker(tanggalAkhir) { selectedDate ->
                tanggalAkhir = selectedDate
                updateDateDisplay()
                currentPage = 1
                loadData()
            }
        }

        tvFilterSholat.setOnClickListener {
            showFilterDialog("Pilih Jenis Sholat", sholatOptions, selectedSholat) { selected ->
                selectedSholat = selected
                tvFilterSholat.text = selected
                currentPage = 1
                loadData()
            }
        }

        tvFilterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                tvFilterJurusan.text = selected
                currentPage = 1
                loadData()
            }
        }

        tvFilterKelas.setOnClickListener {
            showFilterDialog("Pilih Kelas", kelasOptions, selectedKelas) { selected ->
                selectedKelas = selected
                tvFilterKelas.text = selected
                currentPage = 1
                loadData()
            }
        }
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

        lifecycleScope.launch {
            try {
                val statsResponse = RetrofitClient.apiService.getAttendanceAnalytics("Bearer $token")
                if (statsResponse.isSuccessful) {
                    val stats = statsResponse.body()?.data
                    if (stats != null) {
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
                    }
                }
            } catch (_: Exception) {}

            try {
                val chartResponse = RetrofitClient.apiService.getChartData("Bearer $token")
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
            } catch (_: Exception) {}
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
                    runOnUiThread {
                        currentPageItems = data.absensi
                        val pagination = data.pagination
                        totalItems = pagination?.total_items ?: currentPageItems.size
                        totalPages = pagination?.total_pages ?: 1
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
                    runOnUiThread {
                        showLoading(false)
                        isLoading = false
                        showEmptyState("Gagal memuat data: ${error.message}")
                    }
                }
            )
        }
    }

    private fun updatePaginationUI() {
        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages
        tvPageInfo.text = "Halaman $currentPage dari $totalPages"
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

        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas

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
                        token = "Bearer $token", startDate = startDate, endDate = endDate, jurusan = jurusanApi ?: ""
                    )
                    "pdf" -> RetrofitClient.apiService.exportAttendanceReportPdf(
                        token = "Bearer $token", startDate = startDate, endDate = endDate, jurusan = jurusanApi
                    )
                    "csv" -> RetrofitClient.apiService.exportAttendanceCSV(
                        token = "Bearer $token", startDate = startDate, endDate = endDate, jurusan = jurusanApi
                    )
                    else -> RetrofitClient.apiService.exportAttendanceReport(
                        token = "Bearer $token", startDate = startDate, endDate = endDate, jurusan = jurusanApi ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    btn.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        val ext = when (format) { "excel" -> "xlsx"; "pdf" -> "pdf"; "csv" -> "csv"; else -> "xlsx" }
                        val mime = when (format) {
                            "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            "pdf" -> "application/pdf"
                            "csv" -> "text/csv"
                            else -> "application/octet-stream"
                        }
                        val fileName = "Laporan_Absensi_${startDate}_$endDate.$ext"
                        val success = saveFileToDownloads(response.body()!!, fileName, mime)
                        if (success) {
                            Toast.makeText(this@LaporanAdminActivity, "Laporan berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LaporanAdminActivity, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LaporanAdminActivity, "Gagal mengunduh: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btn.isEnabled = true
                    Toast.makeText(this@LaporanAdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFileToDownloads(body: ResponseBody, fileName: String, mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"): Boolean {
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
                true
            } ?: false
        } catch (_: Exception) { false }
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
        tvTanggalAwal.text = displayDateFormat.format(tanggalAwal.time)
        tvTanggalAkhir.text = displayDateFormat.format(tanggalAkhir.time)
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerAbsensi.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        recyclerAbsensi.visibility = View.GONE
    }

    private fun hideEmptyState() {
        tvEmptyState.visibility = View.GONE
        recyclerAbsensi.visibility = View.VISIBLE
    }
}
