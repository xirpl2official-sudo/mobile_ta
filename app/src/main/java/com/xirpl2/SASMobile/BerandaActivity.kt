package com.xirpl2.SASMobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
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
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class BerandaActivity : AppCompatActivity() {

    private lateinit var rvJadwalSholat: RecyclerView
    private lateinit var rvRiwayatAbsensi: RecyclerView
    private lateinit var tvTotalValue: TextView
    private lateinit var tvHadirValue: TextView
    private lateinit var tvStatistikValue: TextView

    private lateinit var jadwalAdapter: JadwalSholatAdapter
    private lateinit var riwayatAdapter: RiwayatAbsensiAdapter
    
    private var popupWindow: PopupWindow? = null
    
    private val repository = BerandaRepository()
    private val TAG = "BerandaActivity"
    
    private var notificationCounterBroadcast: BroadcastReceiver? = null
    private lateinit var notificationCounter: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)

        // Initialize Views
        initializeViews()
        
        // Setup Popup Menu
        setupPopupMenu()
        
        // Setup RecyclerViews dengan data dummy dulu
        setupJadwalSholat()
        setupRiwayatAbsensi()

        setupAbsensiButton()
        setupNotificationButton()
        
        // Load statistics from API
        loadStatistics()
        
        // Load riwayat absensi dari API untuk siswa yang login
        loadRiwayatFromAPI()
    }
    
    private fun initializeViews() {
        rvJadwalSholat = findViewById(R.id.rvJadwalSholat)
        rvRiwayatAbsensi = findViewById(R.id.rvRiwayatAbsensi)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvHadirValue = findViewById(R.id.tvHadirValue)
        tvStatistikValue = findViewById(R.id.tvStatistikValue)
    }

    private fun setupNotificationButton() {
        val iconNotifikasi = findViewById<ImageView>(R.id.iconNotifikasi)
        notificationCounter = findViewById<android.widget.TextView>(R.id.notificationCounter)
        
        iconNotifikasi.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }
        
        // Load and display notification count
        updateNotificationCounter()
    }
    
    private fun updateNotificationCounter() {
        val sharedPref = getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
        val count = sharedPref.getInt("notification_count", 0)
        
        if (count > 0) {
            notificationCounter.text = count.toString()
            notificationCounter.visibility = android.view.View.VISIBLE
        } else {
            notificationCounter.visibility = android.view.View.GONE
        }
    }

    private fun setupAbsensiButton() {
        val btnAbsensi = findViewById<android.widget.Button>(R.id.btnAbsensi)
        btnAbsensi.setOnClickListener {
            // Navigate to ScanQrActivity for students to scan QR code displayed by staff
            startActivity(Intent(this@BerandaActivity, ScanQrActivity::class.java))
        }
    }
    private fun setupJadwalSholat() {
        // Initialize with empty list or loading state
        // Data will be populated from API in onResume -> loadJadwalSholatFromAPI
        jadwalAdapter = JadwalSholatAdapter(emptyList())

        // Setup RecyclerView
        rvJadwalSholat.apply {
            layoutManager = LinearLayoutManager(this@BerandaActivity)
            adapter = jadwalAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupRiwayatAbsensi() {
        // Initialize with empty list - data will be loaded from API
        val emptyList = emptyList<RiwayatAbsensi>()

        // Setup adapter with empty list
        riwayatAdapter = RiwayatAbsensiAdapter(emptyList)

        // Setup RecyclerView
        rvRiwayatAbsensi.apply {
            layoutManager = LinearLayoutManager(this@BerandaActivity)
            adapter = riwayatAdapter
            isNestedScrollingEnabled = false
        }
    }
    
    /**
     * Mendapatkan jenis kelamin dari SharedPreferences
     * Data ini seharusnya sudah disimpan saat login/registrasi
     */
    private fun getJenisKelaminFromStorage(): JadwalSholatHelper.JenisKelamin {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val jenisKelaminStr = sharedPref.getString("jenis_kelamin", "L") ?: "L"
        
        return if (jenisKelaminStr == "P") {
            JadwalSholatHelper.JenisKelamin.PEREMPUAN
        } else {
            JadwalSholatHelper.JenisKelamin.LAKI_LAKI
        }
    }
    
    /**
     * Setup Popup Menu untuk icon hamburger
     */
    private fun setupPopupMenu() {
        val iconMenu = findViewById<ImageView>(R.id.iconMenu)
        iconMenu.setOnClickListener {
            showPopupMenu(it)
        }
    }
    
    /**
     * Menampilkan popup menu di bawah icon hamburger
     */
    private fun showPopupMenu(anchorView: android.view.View) {
        // Dismiss popup yang sedang aktif jika ada
        dismissPopupMenu()
        
        // Inflate layout popup menu
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_menu, null)
        
        // Get user data dari SharedPreferences
        val (nama, nis) = getUserDataFromStorage()
        
        // Populate data ke popup
        val tvStudentName = popupView.findViewById<TextView>(R.id.tvStudentName)
        val tvStudentNIS = popupView.findViewById<TextView>(R.id.tvStudentNIS)
        tvStudentName.text = nama
        tvStudentNIS.text = nis
        
        // Setup click listeners untuk menu items
        val btnSettings = popupView.findViewById<LinearLayout>(R.id.btnSettings)
        val btnLogout = popupView.findViewById<LinearLayout>(R.id.btnLogout)
        
        btnSettings.setOnClickListener {
            dismissPopupMenu()
            // Navigate to Settings Activity
            val intent = Intent(this, PengaturanAkunActivity::class.java)
            startActivity(intent)
        }
        
        btnLogout.setOnClickListener {
            dismissPopupMenu()
            handleLogout()
        }
        
        // Create PopupWindow
        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true // focusable agar bisa dismiss saat klik di luar
        )
        
        // Set background untuk shadow dan dismiss on outside touch
        popupWindow?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow?.isOutsideTouchable = true
        
        // Show popup di bawah anchor view (icon menu)
        popupWindow?.showAsDropDown(anchorView, 0, 10, Gravity.START)
    }
    
    /**
     * Dismiss popup menu jika sedang ditampilkan
     */
    private fun dismissPopupMenu() {
        popupWindow?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        popupWindow = null
    }
    
    /**
     * Mendapatkan data user (nama dan NIS) dari SharedPreferences
     */
    private fun getUserDataFromStorage(): Pair<String, String> {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val nama = sharedPref.getString("nama_siswa", "Nama Siswa") ?: "Nama Siswa"
        val nis = sharedPref.getString("nis", "0000000000") ?: "0000000000"
        return Pair(nama, nis)
    }
    
    /**
     * Handle logout dengan konfirmasi dialog
     */
    private fun handleLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Clear auth token dan user data
                val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                
                // Show confirmation
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                
                // Navigate back to Login Activity
                val intent = Intent(this, MasukActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
    
    /**
     * Load data dari API
     * Uncomment ketika backend sudah ready
     */
    private fun loadDataFromAPI() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to login
            return
        }
        
        lifecycleScope.launch {
            // Load Jadwal Sholat
            loadJadwalSholatFromAPI(token)
            
            // Load Riwayat Absensi
            loadRiwayatAbsensiFromAPI(token)
            
            // Load Statistik
            loadStatistikFromAPI(token)
        }
    }
    
    private suspend fun loadJadwalSholatFromAPI(token: String) {
        repository.getJadwalSholat(token).fold(
            onSuccess = { jadwalDataList ->
                // Convert API data ke model lokal
                val jadwalList = jadwalDataList
                    .filter { data ->
                        JadwalSholatHelper.ALLOWED_PRAYERS.any { it.equals(data.jenis_sholat, ignoreCase = true) }
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
                
                // Update adapter
                runOnUiThread {
                    jadwalAdapter = JadwalSholatAdapter(jadwalList)
                    rvJadwalSholat.adapter = jadwalAdapter
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading jadwal sholat: ${error.message}")
                // Tetap gunakan data dummy jika API gagal
            }
        )
    }
    
    private suspend fun loadRiwayatAbsensiFromAPI(token: String) {
        repository.getHistorySiswa(token, 0).fold(
            onSuccess = { historyData ->
                // Use empty list if absensi is null
                val absensiList = historyData.absensi ?: emptyList()
                
                // Convert API data ke model lokal
                val riwayatList = absensiList.map { data ->
                    val status = when (data.status.uppercase()) {
                        "HADIR" -> StatusAbsensi.HADIR
                        "ALPHA" -> StatusAbsensi.ALPHA
                        "SAKIT" -> StatusAbsensi.SAKIT
                        "IZIN" -> StatusAbsensi.IZIN
                        else -> StatusAbsensi.ALPHA
                    }
                    
                    RiwayatAbsensi(
                        tanggal = formatTanggal(data.tanggal),
                        namaSholat = data.getPrayerName(),
                        status = status,
                        waktuAbsen = formatWaktu(data.waktu_absen)
                    )
                }
                
                // Update adapter
                runOnUiThread {
                    riwayatAdapter = RiwayatAbsensiAdapter(riwayatList)
                    rvRiwayatAbsensi.adapter = riwayatAdapter
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading riwayat absensi: ${error.message}")
            }
        )
    }
    
    private suspend fun loadStatistikFromAPI(token: String) {
        // Load statistics (Hadir & Percentage)
        repository.getStatistikAbsensi(token).fold(
            onSuccess = { statistik ->
                runOnUiThread {
                    // tvTotalValue.text = statistik.total_hari.toString() // Removed: will use Total Siswa
                    tvHadirValue.text = statistik.total_hadir.toString()
                    tvStatistikValue.text = "${(statistik.persentase_kehadiran * 100).toInt()}%"
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading statistik: ${error.message}")
            }
        )
        
        // Load Total Siswa
        repository.getTotalSiswa(token).fold(
            onSuccess = { totalSiswa ->
                runOnUiThread {
                    tvTotalValue.text = totalSiswa.toString()
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Error loading total siswa: ${error.message}")
                // Fallback if endpoint request fails (e.g. 404), maybe show user's total days or --
                runOnUiThread {
                   // tvTotalValue.text = "--"
                }
            }
        )
    }
    
    /**
     * Load statistics from /api/statistics endpoint
     * This endpoint doesn't require authentication
     */
    private fun loadStatistics() {
        lifecycleScope.launch {
            repository.getStatistics().fold(
                onSuccess = { statistics ->
                    runOnUiThread {
                        // Update Total Siswa
                        tvTotalValue.text = statistics.total_siswa.toString()
                        
                        // Note: Hadir value is now loaded from history/siswa API (student's weekly attendance)
                        
                        // Update Statistik (persentase kehadiran)
                        tvStatistikValue.text = "${(statistics.persentase_kehadiran * 100).toInt()}%"
                    }
                    Log.d(TAG, "Statistics loaded: total_siswa=${statistics.total_siswa}, persentase=${statistics.persentase_kehadiran}%")
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading statistics: ${error.message}")
                    // Keep showing "--" as default if API fails
                }
            )
        }
    }
    
    private fun getAuthToken(): String {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
    }
    
    /**
     * Format tanggal dari "YYYY-MM-DD" ke "DD MMM YYYY"
     * Contoh: "2024-11-14" -> "14 NOV 2024"
     */
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
    
    /**
     * Format waktu dari "HH:mm:ss" ke "HH:mm"
     * Contoh: "07:30:00" -> "07:30"
     */
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

    // Fungsi helper untuk refresh data dari database/API
    fun refreshJadwalSholat(newData: List<JadwalSholat>) {
        jadwalAdapter = JadwalSholatAdapter(newData)
        rvJadwalSholat.adapter = jadwalAdapter
    }

    fun refreshRiwayatAbsensi(newData: List<RiwayatAbsensi>) {
        riwayatAdapter = RiwayatAbsensiAdapter(newData)
        rvRiwayatAbsensi.adapter = riwayatAdapter
    }
    
    /**
     * Load riwayat absensi dari API untuk siswa yang sedang login
     * Menggantikan data dummy dengan data real dari backend
     */
    private fun loadRiwayatFromAPI() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            Log.w(TAG, "No auth token, skipping riwayat absensi load")
            return
        }
        
        lifecycleScope.launch {
            repository.getHistorySiswa(token, 0).fold(
                onSuccess = { historyData ->
                    // Use empty list if absensi is null
                    val absensiList = historyData.absensi ?: emptyList()
                    
                    // Count HADIR status for this week's attendance
                    val hadirCount = absensiList.count { 
                        it.status.uppercase() == "HADIR" 
                    }
                    
                    // Convert API data ke model lokal
                    val riwayatList = absensiList.map { data ->
                        val status = when (data.status.uppercase()) {
                            "HADIR" -> StatusAbsensi.HADIR
                            "ALPHA" -> StatusAbsensi.ALPHA
                            "SAKIT" -> StatusAbsensi.SAKIT
                            "IZIN" -> StatusAbsensi.IZIN
                            else -> StatusAbsensi.ALPHA
                        }
                        
                        RiwayatAbsensi(
                            tanggal = formatTanggal(data.tanggal),
                            namaSholat = data.getPrayerName(),
                            status = status,
                            waktuAbsen = formatWaktu(data.waktu_absen)
                        )
                    }
                    
                    // Update adapter and hadir count on UI thread
                    runOnUiThread {
                        // Update Hadir card with student's weekly attendance count
                        tvHadirValue.text = hadirCount.toString()
                        
                        if (riwayatList.isEmpty()) {
                            Log.d(TAG, "Riwayat absensi kosong untuk siswa ini")
                        } else {
                            riwayatAdapter = RiwayatAbsensiAdapter(riwayatList)
                            rvRiwayatAbsensi.adapter = riwayatAdapter
                            Log.d(TAG, "Loaded ${riwayatList.size} riwayat absensi, hadir: $hadirCount")
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading riwayat absensi: ${error.message}")
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateNotificationCounter()
        
        // Refresh API data
        val token = getAuthToken()
        if (token.isNotEmpty()) {
            lifecycleScope.launch {
                loadJadwalSholatFromAPI(token)
                // Also refresh other dynamic data if needed
                loadRiwayatFromAPI() 
                loadStatistics()
            }
        }
        
        // Register broadcast receiver to listen for notification count changes
        notificationCounterBroadcast = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                val newCount = intent.getIntExtra("count", 0)
                if (newCount > 0) {
                    notificationCounter.text = newCount.toString()
                    notificationCounter.visibility = android.view.View.VISIBLE
                } else {
                    notificationCounter.visibility = android.view.View.GONE
                }
            }
        }
        
        val filter = IntentFilter("com.xirpl2.SASMobile.NOTIFICATION_COUNT_CHANGED")
        registerReceiver(notificationCounterBroadcast, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    }
    
    override fun onPause() {
        super.onPause()
        notificationCounterBroadcast?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Receiver wasn't registered
            }
        }
    }
}