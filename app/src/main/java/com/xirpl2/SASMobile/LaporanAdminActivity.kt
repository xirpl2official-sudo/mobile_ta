package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.xirpl2.SASMobile.adapter.LaporanAbsensiAdapter
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import com.xirpl2.SASMobile.model.LaporanStatistik
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LaporanAdminActivity : BaseAdminActivity() {

    private val TAG = "LaporanAdminActivity"
    private val repository = BerandaRepository()

    
    private lateinit var mainScrollView: NestedScrollView

    
    private lateinit var tvTanggalAwal: TextView
    private lateinit var tvTanggalAkhir: TextView
    private lateinit var tvFilterJurusan: TextView
    private lateinit var tvFilterKelas: TextView

    
    private lateinit var progressKehadiran: ProgressBar
    private lateinit var tvCountKehadiran: TextView 
    private lateinit var tvCountIzin: TextView
    private lateinit var tvCountSakit: TextView
    private lateinit var tvCountAlpha: TextView

    
    private lateinit var tvDataAbsensiTitle: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerAbsensi: RecyclerView

    
    
    private lateinit var btnExportExcel: MaterialButton
    private lateinit var btnExportPdf: MaterialButton

    
    private lateinit var absensiAdapter: LaporanAbsensiAdapter

    
    private val allItems = mutableListOf<AbsensiStaffItem>()
    private var statistik: LaporanStatistik? = null
    
    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private var isLoading = false
    private var isLastPage = false
    private val pageSize = 20 
    
    private var loadJob: Job? = null

    
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedKelas: String = "Semua Kelas"
    private var tanggalAwal: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -6) 
    }
    private var tanggalAkhir: Calendar = Calendar.getInstance() 

    
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "X", "XI", "XII")

    
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
        setupInfiniteScroll()
        
        
        updateDateDisplay()

        
        loadData(reset = true)
    }

    private fun initViews() {
        
        mainScrollView = findViewById(R.id.mainScrollView)

        
        tvTanggalAwal = findViewById(R.id.tvTanggalAwal)
        tvTanggalAkhir = findViewById(R.id.tvTanggalAkhir)
        tvFilterJurusan = findViewById(R.id.tvFilterJurusan)
        tvFilterKelas = findViewById(R.id.tvFilterKelas)

        
        progressKehadiran = findViewById(R.id.progressKehadiran)
        tvCountKehadiran = findViewById(R.id.tvCountKehadiran) 
        tvCountIzin = findViewById(R.id.tvCountIzin)
        tvCountSakit = findViewById(R.id.tvCountSakit)
        tvCountAlpha = findViewById(R.id.tvCountAlpha)

        
        tvDataAbsensiTitle = findViewById(R.id.tvDataAbsensiTitle)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerAbsensi = findViewById(R.id.recyclerAbsensi)

        
        
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
    
    private fun setupInfiniteScroll() {
        mainScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight)) {
                if (!isLoading && !isLastPage) {
                    loadMoreData()
                }
            }
        })
    }

    private fun setupFilters() {
        
        tvTanggalAwal.setOnClickListener {
            showDatePicker(tanggalAwal) { selectedDate ->
                tanggalAwal = selectedDate
                updateDateDisplay()
                loadData(reset = true)
            }
        }

        
        tvTanggalAkhir.setOnClickListener {
            showDatePicker(tanggalAkhir) { selectedDate ->
                tanggalAkhir = selectedDate
                updateDateDisplay()
                loadData(reset = true)
            }
        }

        
        tvFilterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                tvFilterJurusan.text = selected
                loadData(reset = true)
            }
        }

        
        tvFilterKelas.setOnClickListener {
            showFilterDialog("Pilih Kelas", kelasOptions, selectedKelas) { selected ->
                selectedKelas = selected
                tvFilterKelas.text = selected
                loadData(reset = true)
            }
        }
    }

    private fun setupButtons() {
        

        
        val role = getSharedPreferences("UserData", android.content.Context.MODE_PRIVATE)
            .getString("user_role", "")?.lowercase() ?: ""
        
        
        btnExportExcel.setOnClickListener {
            downloadExcelReport()
        }
        
        btnExportPdf.setOnClickListener {
            downloadPdfReport()
        }
    }

    private fun downloadExcelReport() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") "" else selectedJurusan

        showLoading(true)
        btnExportExcel.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.exportAttendanceReport(
                    token = "Bearer $token",
                    startDate = startDate,
                    endDate = endDate,
                    jurusan = jurusanApi
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val fileName = "Laporan_Absensi_${selectedJurusan.replace(" ", "_")}_$startDate.xlsx"
                        val success = saveFileToDownloads(body, fileName)
                        
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            btnExportExcel.isEnabled = true
                            if (success) {
                                Toast.makeText(this@LaporanAdminActivity, "Laporan berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@LaporanAdminActivity, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        btnExportExcel.isEnabled = true
                        Toast.makeText(this@LaporanAdminActivity, "Gagal mengunduh: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnExportExcel.isEnabled = true
                    Toast.makeText(this@LaporanAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
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

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun downloadPdfReport() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = apiDateFormat.format(tanggalAwal.time)
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas

        showLoading(true)
        btnExportPdf.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.exportAttendancePdf(
                    token = "Bearer $token",
                    tanggal = startDate,
                    kelas = kelasApi,
                    jurusan = jurusanApi
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val fileName = "Laporan_Absensi_${selectedJurusan.replace(" ", "_")}_$startDate.pdf"
                        val success = saveFileToDownloads(body, fileName, "application/pdf")

                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            btnExportPdf.isEnabled = true
                            if (success) {
                                Toast.makeText(this@LaporanAdminActivity, "PDF berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@LaporanAdminActivity, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        btnExportPdf.isEnabled = true
                        Toast.makeText(this@LaporanAdminActivity, "Gagal mengunduh PDF: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnExportPdf.isEnabled = true
                    Toast.makeText(this@LaporanAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDatePicker(currentDate: Calendar, onDateSelected: (Calendar) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar)
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
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

    private fun loadData(reset: Boolean) {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        if (reset) {
            loadJob?.cancel()
            currentPage = 1
            allItems.clear()
            absensiAdapter.clearData()
            isLastPage = false
            showLoading(true)
        } else {
            
            
            progressLoading.visibility = View.VISIBLE
        }

        isLoading = true

        
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)

        lifecycleScope.launch {
            repository.getHistoryStaff(
                token = token,
                startDate = startDate,
                endDate = endDate,
                kelas = kelasApi,
                jurusan = jurusanApi,
                page = currentPage,
                limit = pageSize
            ).fold(
                onSuccess = { data ->
                    runOnUiThread {
                        if (reset) {
                            statistik = data.statistik
                            allItems.clear()
                        }
                        
                        val newItems = data.absensi
                        allItems.addAll(newItems ?: emptyList())
                        
                        val pagination = data.pagination
                        totalItems = pagination?.total_items ?: allItems.size
                        totalPages = pagination?.total_pages ?: 1
                        
                        if (pagination != null) {
                            isLastPage = currentPage >= totalPages
                        } else {
                            isLastPage = newItems.isEmpty() || newItems.size < pageSize
                        }
                        
                        showLoading(false)
                        isLoading = false
                        
                        absensiAdapter.updateData(allItems.toList())
                        
                        if (reset) {
                            updateStatistikDisplay()
                        }
                        
                        val currentCount = allItems.size
                        tvDataAbsensiTitle.text = "Data Absensi ($currentCount / $totalItems entri)"

                        if (allItems.isEmpty()) {
                            showEmptyState("Tidak ada data absensi untuk periode ini")
                        } else {
                            hideEmptyState()
                        }
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        showLoading(false)
                        isLoading = false
                        if (allItems.isEmpty()) {
                            showEmptyState("Gagal memuat data: ${error.message}")
                        } else {
                            Toast.makeText(this@LaporanAdminActivity, "Gagal memuat halaman berikutnya", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
    
    private fun loadMoreData() {
        if (isLoading || isLastPage) return
        currentPage++
        loadData(reset = false)
    }

    private fun updateStatistikDisplay() {
        statistik?.let { stat ->
            
            val percentKehadiran = stat.persentase_hadir
            progressKehadiran.progress = percentKehadiran.toInt().coerceIn(0, 100)
            
            
            tvCountKehadiran.text = stat.total_hadir.toString()

            
            tvCountIzin.text = stat.total_izin.toString()
            tvCountSakit.text = stat.total_sakit.toString()
            tvCountAlpha.text = stat.total_alpha.toString()
        } ?: run {
            
            progressKehadiran.progress = 0
            tvCountKehadiran.text = "0"
            tvCountIzin.text = "0"
            tvCountSakit.text = "0"
            tvCountAlpha.text = "0"
        }
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show && allItems.isEmpty()) {
            recyclerAbsensi.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        } else if (!show && allItems.isNotEmpty()) {
            recyclerAbsensi.visibility = View.VISIBLE
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
