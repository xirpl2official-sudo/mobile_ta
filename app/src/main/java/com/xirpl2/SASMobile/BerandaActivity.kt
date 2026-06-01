package com.xirpl2.SASMobile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.RiwayatAbsensi
import com.xirpl2.SASMobile.model.StatusAbsensi
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BerandaActivity : BaseSiswaActivity() {

    private lateinit var rvJadwalSholat: RecyclerView
    private lateinit var rvRiwayatAbsensi: RecyclerView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvAlphaValue: TextView
    private lateinit var tvIzinSakitValue: TextView
    private lateinit var tvJadwalError: TextView
    private lateinit var tvRiwayatError: TextView

    private lateinit var jadwalAdapter: JadwalSholatAdapter
    private lateinit var riwayatAdapter: RiwayatAbsensiAdapter
    
    private val repository = BerandaRepository()
    private val TAG = "BerandaActivity"

    // Guard against duplicate loadAllData calls from onCreate+onResume race condition
    private var isDataLoaded = false

    override fun getCurrentMenuItem(): SiswaMenuItem = SiswaMenuItem.BERANDA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)
        setupStatusBar()

        findViewById<android.view.View>(R.id.topBarContent)?.let { topBar ->
            applyEdgeToEdge(topBar)
        }

        // Basic initialization
        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupJadwalSholat()
        setupRiwayatAbsensi()
        setupAbsensiButton()

        // Load data is handled in onResume to prevent duplicate calls and race conditions
    }

    private fun loadAllData() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            return
        }

        // Statistics are derived from the student's own history response (statistik field),
        // matching the desktop which uses historyWrapper.statistik from getStudentAttendanceHistory.
        // No separate /statistics call needed — that endpoint returns global school-wide data.
        loadRiwayatFromAPI()

        lifecycleScope.launch {
            loadJadwalSholatFromAPI(token)
        }
    }
    
    private fun initializeViews() {
        rvJadwalSholat = findViewById(R.id.rvJadwalSholat)
        rvRiwayatAbsensi = findViewById(R.id.rvRiwayatAbsensi)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvAlphaValue = findViewById(R.id.tvAlphaValue)
        tvIzinSakitValue = findViewById(R.id.tvIzinSakitValue)
        tvJadwalError = findViewById(R.id.tvJadwalError)
        tvRiwayatError = findViewById(R.id.tvRiwayatError)
    }


    private fun setupAbsensiButton() {
        val btnAbsensi = findViewById<android.widget.Button>(R.id.btnAbsensi)
        btnAbsensi.setOnClickListener {
            
            startActivity(Intent(this@BerandaActivity, ScanQrActivity::class.java))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardTotalAlpha).setOnClickListener {
            val dialog = PresenceDetailDialogFragment()
            dialog.show(supportFragmentManager, "PresenceDetail")
        }
    }
    private fun setupJadwalSholat() {
        
        
        jadwalAdapter = JadwalSholatAdapter()

        
        rvJadwalSholat.apply {
            layoutManager = LinearLayoutManager(this@BerandaActivity)
            adapter = jadwalAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupRiwayatAbsensi() {

        riwayatAdapter = RiwayatAbsensiAdapter()

        rvRiwayatAbsensi.apply {
            layoutManager = LinearLayoutManager(this@BerandaActivity)
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUnduhRiwayat).setOnClickListener {
            exportRiwayatToCsv()
        }
    }

    private fun exportRiwayatToCsv() {
        val items = riwayatAdapter.currentList
        if (items.isEmpty()) {
            Toast.makeText(this, "Tidak ada data riwayat untuk diunduh", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val header = "Tanggal,Sholat,Waktu,Status"
            val rows = items.map { item ->
                "${item.tanggal},${item.namaSholat},${item.waktuAbsen ?: "-"},${item.status.name}"
            }
            val csv = "\uFEFF" + (listOf(header) + rows).joinToString("\n")

            val fileName = "riwayat_absensi_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.csv"

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: Use MediaStore.Downloads API
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(csv.toByteArray())
                        }
                        withContext(Dispatchers.Main) {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@BerandaActivity, "Tersimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!isFinishing && !isDestroyed) {
                                Toast.makeText(this@BerandaActivity, "Gagal membuat file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Android 9 and below: Use legacy file API
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, fileName)
                    file.writeText(csv)
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(this@BerandaActivity, "Tersimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal mengunduh riwayat: ${e.message}")
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@BerandaActivity, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun getJenisKelaminFromStorage(): JadwalSholatHelper.JenisKelamin {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
        val jenisKelaminStr = sharedPref.getString("jenis_kelamin", "L") ?: "L"
        
        return if (jenisKelaminStr == "P") {
            JadwalSholatHelper.JenisKelamin.PEREMPUAN
        } else {
            JadwalSholatHelper.JenisKelamin.LAKI_LAKI
        }
    }
    
    private suspend fun loadJadwalSholatFromAPI(token: String) {
        // Server /today returns ALL prayers for the day (Subuh, Dzuhur, Ashar, Maghrib, Isya, Dhuha, Jumat).
        // Client-side gender filter: on Friday, male sees Jumat (not Dzuhur), female sees Dzuhur (not Jumat).
        // Status (AKAN_DATANG/SEDANG_BERLANGSUNG/SELESAI) is computed client-side — server doesn't provide it.
        val jenisKelamin = getJenisKelaminFromStorage()
        val allowedByGender = JadwalSholatHelper.getJadwalSholatByGender(jenisKelamin)

        repository.getJadwalSholatToday(token).fold(
            onSuccess = { jadwalDataList ->

                val jadwalList = jadwalDataList
                    .filter { data ->
                        allowedByGender.any { it.equals(data.jenis_sholat, ignoreCase = true) }
                    }
                    .map { data ->
                    val status = JadwalSholatHelper.getStatusSholat(data.jam_mulai, data.jam_selesai)
                    JadwalSholat(
                        id = data.id,
                        namaSholat = data.jenis_sholat,
                        jamMulai = data.jam_mulai,
                        jamSelesai = data.jam_selesai,
                        status = status
                    )
                }
                
                
                runOnUiThread {
                    if (!::jadwalAdapter.isInitialized) {
                        jadwalAdapter = JadwalSholatAdapter()
                        rvJadwalSholat.adapter = jadwalAdapter
                    }
                    tvJadwalError.visibility = android.view.View.GONE
                    rvJadwalSholat.visibility = android.view.View.VISIBLE
                    jadwalAdapter.submitList(jadwalList)
                }
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to load jadwal sholat: ${error.message}")
                runOnUiThread {
                    rvJadwalSholat.visibility = android.view.View.GONE
                    tvJadwalError.visibility = android.view.View.VISIBLE
                }
            }
        )
    }

    
    private fun formatTanggal(tanggal: String): String {
        return try {
            val parts = tanggal.split("-")
            if (parts.size == 3) {
                val tahun = parts[0]
                val bulan = when (parts[1]) {
                    "01" -> "JAN"
                    "02" -> "FEB"
                    "03" -> "MAR"
                    "04" -> "APR"
                    "05" -> "MEI"
                    "06" -> "JUN"
                    "07" -> "JUL"
                    "08" -> "AGU"
                    "09" -> "SEP"
                    "10" -> "OKT"
                    "11" -> "NOV"
                    "12" -> "DES"
                    else -> parts[1]
                }
                val hari = parts[2].toIntOrNull()?.toString() ?: parts[2]
                "$hari $bulan $tahun"
            } else {
                tanggal
            }
        } catch (e: Exception) {
            tanggal
        }
    }
    
    private fun formatWaktu(waktu: String?): String? {
        if (waktu == null) return null
        return try {
            val parts = waktu.split(":")
            if (parts.size >= 2) {
                "${parts[0]}:${parts[1]}"
            } else {
                waktu
            }
        } catch (e: Exception) {
            waktu
        }
    }

    
    fun refreshJadwalSholat(newData: List<JadwalSholat>) {
        if (!::jadwalAdapter.isInitialized) {
            jadwalAdapter = JadwalSholatAdapter()
            rvJadwalSholat.adapter = jadwalAdapter
        }
        jadwalAdapter.submitList(newData)
    }

    fun refreshRiwayatAbsensi(newData: List<RiwayatAbsensi>) {
        if (!::riwayatAdapter.isInitialized) {
            riwayatAdapter = RiwayatAbsensiAdapter()
            rvRiwayatAbsensi.adapter = riwayatAdapter
        }
        riwayatAdapter.submitList(newData)
    }
    
    private fun loadRiwayatFromAPI() {
        val token = getAuthToken()

        if (token.isEmpty()) {
            return
        }

        lifecycleScope.launch {
            repository.getHistorySiswa(token, 0).fold(
                onSuccess = { historyData ->

                    // Update stat cards from the student's own statistik (matching desktop:
                    // statsData = historyWrapper.statistik from getStudentAttendanceHistory)
                    val stats = historyData.statistik
                    if (stats != null) {
                        runOnUiThread {
                            tvTotalValue.text = stats.total_absensi.toString()
                            tvAlphaValue.text = stats.total_alpha.toString()
                            tvIzinSakitValue.text = (stats.total_izin + stats.total_sakit).toString()
                        }
                    }

                    val absensiList = historyData.absensi ?: emptyList()

                    val riwayatList = absensiList.map { data ->
                        val status = StatusAbsensi.fromString(data.status)

                        RiwayatAbsensi(
                            id = data.id,
                            tanggal = formatTanggal(data.tanggal),
                            namaSholat = data.getPrayerName() ?: "-",
                            status = status,
                            waktuAbsen = formatWaktu(data.waktu_absen)
                        )
                    }

                    runOnUiThread {
                        if (riwayatList.isNotEmpty()) {
                            if (!::riwayatAdapter.isInitialized) {
                                riwayatAdapter = RiwayatAbsensiAdapter()
                                rvRiwayatAbsensi.adapter = riwayatAdapter
                            }
                            tvRiwayatError.visibility = android.view.View.GONE
                            findViewById<LinearLayout>(R.id.riwayatHeader).visibility = android.view.View.VISIBLE
                            findViewById<android.view.View>(R.id.dividerHeader).visibility = android.view.View.VISIBLE
                            rvRiwayatAbsensi.visibility = android.view.View.VISIBLE
                            riwayatAdapter.submitList(riwayatList)
                        } else {
                            findViewById<LinearLayout>(R.id.riwayatHeader).visibility = android.view.View.GONE
                            findViewById<android.view.View>(R.id.dividerHeader).visibility = android.view.View.GONE
                            rvRiwayatAbsensi.visibility = android.view.View.GONE
                            tvRiwayatError.text = "Belum ada riwayat absensi"
                            tvRiwayatError.visibility = android.view.View.VISIBLE
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load riwayat absensi: ${error.message}")
                    runOnUiThread {
                        rvRiwayatAbsensi.visibility = android.view.View.GONE
                        findViewById<LinearLayout>(R.id.riwayatHeader).visibility = android.view.View.GONE
                        findViewById<android.view.View>(R.id.dividerHeader).visibility = android.view.View.GONE
                        tvRiwayatError.visibility = android.view.View.VISIBLE
                    }
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()

        val token = getAuthToken()
        if (token.isNotEmpty() && !isDataLoaded) {
            isDataLoaded = true
            loadAllData()
        }
    }

    override fun onPause() {
        super.onPause()
        isDataLoaded = false
    }

    override fun onDestroy() {
        if (::rvJadwalSholat.isInitialized) rvJadwalSholat.adapter = null
        if (::rvRiwayatAbsensi.isInitialized) rvRiwayatAbsensi.adapter = null
        super.onDestroy()
    }
}