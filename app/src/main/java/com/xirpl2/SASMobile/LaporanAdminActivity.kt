package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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

/**
 * Activity untuk menampilkan Laporan Presensi
 * Fitur:
 * - Filter berdasarkan tanggal awal, tanggal akhir, jurusan, dan kelas
 * - Ringkasan statistik kehadiran (persentase, izin, sakit, alpha)
 * - Tabel data absensi yang bisa di-scroll horizontal
 * - Pagination / Infinite Scroll untuk performa yang lebih baik
 */
class LaporanAdminActivity : BaseAdminActivity() {

    private val TAG = "LaporanAdminActivity"

    // Views - Scroll Container
    private lateinit var mainScrollView: NestedScrollView

    // Views - Filter
    private lateinit var tvTanggalAwal: TextView
    private lateinit var tvTanggalAkhir: TextView
    private lateinit var tvFilterJurusan: TextView
    private lateinit var tvFilterKelas: TextView

    // Views - Statistik
    private lateinit var progressKehadiran: ProgressBar
    private lateinit var tvCountKehadiran: TextView // Renamed from tvPercentKehadiran
    private lateinit var tvCountIzin: TextView
    private lateinit var tvCountSakit: TextView
    private lateinit var tvCountAlpha: TextView

    // Views - Data Absensi
    private lateinit var tvDataAbsensiTitle: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerAbsensi: RecyclerView

    // Buttons
    // btnUnduhPDF removed
    private lateinit var btnExportExcel: MaterialButton

    // Adapter
    private lateinit var absensiAdapter: LaporanAbsensiAdapter

    // Data & Pagination
    private val allItems = mutableListOf<AbsensiStaffItem>()
    private var statistik: LaporanStatistik? = null
    
    private var currentPage = 1
    private var totalPages = 1
    private var totalItems = 0
    private var isLoading = false
    private var isLastPage = false
    private val pageSize = 20 // Load lebih sedikit per batch untuk fluiditas UI
    
    private var loadJob: Job? = null

    // Filter states
    private var selectedJurusan: String = "Semua Jurusan"
    private var selectedKelas: String = "Semua Kelas"
    private var tanggalAwal: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -6) // Default: 7 hari terakhir (hari ini + 6 hari lalu)
    }
    private var tanggalAkhir: Calendar = Calendar.getInstance() // Default: hari ini

    // Available options for filters
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val jurusanOptions: List<String> = listOf("Semua Jurusan") + fixedJurusanList
    private val kelasOptions: List<String> = listOf("Semua Kelas", "X", "XI", "XII")

    // Date format
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.LAPORAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laporan_admin)
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
        setupFilters()
        setupButtons()
        setupInfiniteScroll()
        
        // Update initial date display
        updateDateDisplay()

        // Load initial data
        loadData(reset = true)
    }

    private fun initViews() {
        // Main Scroll
        mainScrollView = findViewById(R.id.mainScrollView)

        // Filter views
        tvTanggalAwal = findViewById(R.id.tvTanggalAwal)
        tvTanggalAkhir = findViewById(R.id.tvTanggalAkhir)
        tvFilterJurusan = findViewById(R.id.tvFilterJurusan)
        tvFilterKelas = findViewById(R.id.tvFilterKelas)

        // Statistik views
        progressKehadiran = findViewById(R.id.progressKehadiran)
        tvCountKehadiran = findViewById(R.id.tvCountKehadiran) // Changed ID match
        tvCountIzin = findViewById(R.id.tvCountIzin)
        tvCountSakit = findViewById(R.id.tvCountSakit)
        tvCountAlpha = findViewById(R.id.tvCountAlpha)

        // Data Absensi views
        tvDataAbsensiTitle = findViewById(R.id.tvDataAbsensiTitle)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerAbsensi = findViewById(R.id.recyclerAbsensi)

        // Buttons
        // btnUnduhPDF removed
        btnExportExcel = findViewById(R.id.btnExportExcel)
    }

    private fun setupRecyclerView() {
        absensiAdapter = LaporanAbsensiAdapter()
        recyclerAbsensi.apply {
            layoutManager = LinearLayoutManager(this@LaporanAdminActivity)
            adapter = absensiAdapter
            isNestedScrollingEnabled = false // Penting agar tidak konflik dengan NestedScrollView
        }
    }
    
    /**
     * Setup infinite scroll pada NestedScrollView
     * Karena RecyclerView ada di dalam NestedScrollView, kita mendeteksi scroll pada NestedScrollView
     */
    private fun setupInfiniteScroll() {
        mainScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            // Cek jika scroll sudah di bawah
            if (scrollY == (v.getChildAt(0).measuredHeight - v.measuredHeight)) {
                if (!isLoading && !isLastPage) {
                    loadMoreData()
                }
            }
        })
    }

    private fun setupFilters() {
        // Tanggal Awal - Date Picker
        tvTanggalAwal.setOnClickListener {
            showDatePicker(tanggalAwal) { selectedDate ->
                tanggalAwal = selectedDate
                updateDateDisplay()
                loadData(reset = true)
            }
        }

        // Tanggal Akhir - Date Picker
        tvTanggalAkhir.setOnClickListener {
            showDatePicker(tanggalAkhir) { selectedDate ->
                tanggalAkhir = selectedDate
                updateDateDisplay()
                loadData(reset = true)
            }
        }

        // Jurusan Filter
        tvFilterJurusan.setOnClickListener {
            showFilterDialog("Pilih Jurusan", jurusanOptions, selectedJurusan) { selected ->
                selectedJurusan = selected
                tvFilterJurusan.text = selected
                loadData(reset = true)
            }
        }

        // Kelas Filter
        tvFilterKelas.setOnClickListener {
            showFilterDialog("Pilih Kelas", kelasOptions, selectedKelas) { selected ->
                selectedKelas = selected
                tvFilterKelas.text = selected
                loadData(reset = true)
            }
        }
    }

    private fun setupButtons() {
        // btnUnduhPDF removed

        // Role check for report access
        val role = getSharedPreferences("UserData", android.content.Context.MODE_PRIVATE)
            .getString("user_role", "")?.lowercase() ?: ""
        
        // Both Guru and Wali Kelas can see and export reports according to latest request
        btnExportExcel.setOnClickListener {
            downloadExcelReport()
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
                    Log.e(TAG, "Download error", e)
                    Toast.makeText(this@LaporanAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveFileToDownloads(body: ResponseBody, fileName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
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
            Log.e(TAG, "Save file error", e)
            false
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

    /**
     * Load data absensi
     * @param reset Jika true, hapus data lama dan load dari page 1 (filter baru)
     */
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
            // Loading more - show progress at bottom if needed, or just let default progress bar show
            // Disini kita gunakan progress bar utama saja sederhana
            progressLoading.visibility = View.VISIBLE
        }

        isLoading = true

        // Convert filter values to API parameters
        val kelasApi = if (selectedKelas == "Semua Kelas") null else selectedKelas
        val jurusanApi = if (selectedJurusan == "Semua Jurusan") null else selectedJurusan
        val startDate = apiDateFormat.format(tanggalAwal.time)
        val endDate = apiDateFormat.format(tanggalAkhir.time)

        loadJob = lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading page $currentPage... limit=$pageSize")
                
                val response = RetrofitClient.apiService.getHistoryStaff(
                    token = "Bearer $token",
                    kelas = kelasApi,
                    jurusan = jurusanApi,
                    startDate = startDate,
                    endDate = endDate,
                    page = currentPage,
                    limit = pageSize
                )

                if (response.isSuccessful) {
                    val respBody = response.body()
                    val dataWrapper = respBody?.data

                    // Get statistik (only use from first page or accumulate? Usually first page stats are global)
                    if (reset) {
                        statistik = dataWrapper?.statistik
                    }

                    // Get items
                    val newItems = dataWrapper?.absensi ?: emptyList()
                    allItems.addAll(newItems)
                    
                    // Pagination info
                    val pagination = dataWrapper?.pagination
                    totalItems = pagination?.total_items ?: allItems.size
                    totalPages = pagination?.total_pages ?: 1
                    
                    if (pagination != null) {
                        isLastPage = currentPage >= totalPages
                    } else {
                         // Fallback logic if pagination null
                        isLastPage = newItems.isEmpty() || newItems.size < pageSize
                    }

                    runOnUiThread {
                        showLoading(false)
                        isLoading = false
                        
                        // Update Data Views
                        absensiAdapter.updateData(allItems.toList())
                        
                        // Update Statistik UI (only if reset/first load)
                        if (reset) {
                            updateStatistikDisplay()
                        }
                        
                        // Update Headers
                        val currentCount = allItems.size
                        tvDataAbsensiTitle.text = "Data Absensi ($currentCount / $totalItems entri)"

                        if (allItems.isEmpty()) {
                            showEmptyState("Tidak ada data absensi untuk periode ini")
                        } else {
                            hideEmptyState()
                        }
                        
                         Log.d(TAG, "Loaded page $currentPage. Items: ${newItems.size}. Total: $currentCount/$totalItems")
                    }
                } else {
                    runOnUiThread {
                        showLoading(false)
                        isLoading = false
                        Log.e(TAG, "API Error: ${response.code()} - ${response.message()}")
                        if (allItems.isEmpty()) {
                            showEmptyState("Gagal memuat data: ${response.message()}")
                        } else {
                            Toast.makeText(this@LaporanAdminActivity, "Gagal memuat halaman berikutnya", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    isLoading = false
                    Log.e(TAG, "Exception: ${e.message}", e)
                    if (allItems.isEmpty()) {
                        showEmptyState("Terjadi kesalahan: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun loadMoreData() {
        if (isLoading || isLastPage) return
        currentPage++
        loadData(reset = false)
    }

    private fun updateStatistikDisplay() {
        statistik?.let { stat ->
            // Update kehadiran progress visual (still using percentage for the circle)
            val percentKehadiran = stat.persentase_hadir
            progressKehadiran.progress = percentKehadiran.toInt().coerceIn(0, 100)
            
            // Update text to show COUNT (total hadir) instead of percentage
            tvCountKehadiran.text = stat.total_hadir.toString()

            // Update counts
            tvCountIzin.text = stat.total_izin.toString()
            tvCountSakit.text = stat.total_sakit.toString()
            tvCountAlpha.text = stat.total_alpha.toString()
        } ?: run {
            // Default values if no statistik
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
