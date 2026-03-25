package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.model.DhuhaJurusanData
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch

class BerandaAdminActivity : BaseAdminActivity() {

    private lateinit var tvTotalSiswaValue: TextView
    private lateinit var tvHadirHariIniValue: TextView
    private lateinit var tvIzinSakitValue: TextView
    private lateinit var tvKehadiranValue: TextView
    private lateinit var tvNamaSholat: TextView
    private lateinit var tvWaktuSholat: TextView
    private lateinit var rvJurusan: RecyclerView
    private lateinit var btnGenerateQR: com.google.android.material.button.MaterialButton
    
    private lateinit var jurusanAdapter: JurusanAdapter
    
    private val repository = BerandaRepository()
    private val TAG = "BerandaAdminActivity"
    
    // Real-time clock handler for auto-refreshing jadwal terdekat
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRefreshInterval = 30_000L // 30 seconds
    private val clockRunnable = object : Runnable {
        override fun run() {
            setupJadwalSholat()
            clockHandler.postDelayed(this, clockRefreshInterval)
        }
    }

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.BERANDA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda_admin)
        setupStatusBar()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupQRCodeButton()

        // Load data after layout is ready
        findViewById<View>(R.id.main).post {
            loadStatistikFromAPI()
            setupJadwalSholat()
            setupJurusanList()
            loadNotificationCount()
        }
    }
    
    private fun initializeViews() {
        rvJurusan = findViewById(R.id.rvJurusan)
        tvTotalSiswaValue = findViewById(R.id.tvTotalSiswaValue)
        tvHadirHariIniValue = findViewById(R.id.tvHadirHariIniValue)
        tvIzinSakitValue = findViewById(R.id.tvIzinSakitValue)
        tvKehadiranValue = findViewById(R.id.tvKehadiranValue)
        tvNamaSholat = findViewById(R.id.tvNamaSholat)
        tvWaktuSholat = findViewById(R.id.tvWaktuSholat)
        btnGenerateQR = findViewById(R.id.btnGenerateQR)
    }
    
    /**
     * Load statistik from /statistics API (unified endpoint)
     * Now includes Izin/Sakit counts directly — no need for separate API call
     */
    private fun loadStatistikFromAPI() {
        lifecycleScope.launch {
            // Load global statistics (today's attendance) -- public endpoint, no auth required
            repository.getStatistics().fold(
                onSuccess = { globalStats ->
                    runOnUiThread {
                        // Update Total Siswa
                        tvTotalSiswaValue.text = globalStats.total_siswa.toString()

                        // Update Hadir Hari Ini (based on absensi masuk / QR scan)
                        tvHadirHariIniValue.text = globalStats.total_kehadiran_hari_ini.toString()

                        // Update Izin/Sakit from the unified statistics response
                        val izinSakit = globalStats.total_izin_hari_ini + globalStats.total_sakit_hari_ini
                        tvIzinSakitValue.text = izinSakit.toString()

                        // Update Alpha
                        tvKehadiranValue.text = globalStats.total_alpha_hari_ini.toString()

                        Log.d(TAG, "Stats loaded: total=${globalStats.total_siswa}, hadir=${globalStats.total_kehadiran_hari_ini}, izin=${globalStats.total_izin_hari_ini}, sakit=${globalStats.total_sakit_hari_ini}, alpha=${globalStats.total_alpha_hari_ini}")
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading global stats: ${error.message}")
                }
            )
        }
    }
    
    private fun setupQRCodeButton() {
        val btnGenerateQR = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerateQR)
        btnGenerateQR.setOnClickListener {
            // Navigate to StaffQRActivity to generate and display QR code
            startActivity(Intent(this@BerandaAdminActivity, StaffQRActivity::class.java))
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupJadwalSholat()
        setupJurusanList()
        loadStatistikFromAPI()
        loadNotificationCount()
        // Start real-time clock for jadwal refresh
        clockHandler.postDelayed(clockRunnable, clockRefreshInterval)
    }
    
    override fun onPause() {
        super.onPause()
        // Stop real-time clock when activity is not visible
        clockHandler.removeCallbacks(clockRunnable)
    }

    /**
     * Setup jadwal sholat terdekat dynamically from API
     * Shows upcoming or current prayer based on time
     * Auto-switches from Dhuha to Dhuhur/Jumat when time passes
     */
    private fun setupJadwalSholat() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    val upcomingPrayer = JadwalSholatHelper.getUpcomingPrayerFromList(list)
                    runOnUiThread {
                        if (upcomingPrayer != null) {
                            tvNamaSholat.text = upcomingPrayer.namaSholat
                            tvWaktuSholat.text = "Waktu : ${upcomingPrayer.jamMulai} - ${upcomingPrayer.jamSelesai}"
                            btnGenerateQR.visibility = View.VISIBLE
                        } else {
                            tvNamaSholat.text = "-"
                            tvWaktuSholat.text = "Tidak ada jadwal yang akan datang dalam\nwaktu dekat"
                            btnGenerateQR.visibility = View.GONE
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading jadwal sholat: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Load notification count and show badge if there are pending students
     */
    private fun loadNotificationCount() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getNotifications("Bearer $token")
                if (response.isSuccessful) {
                    val count = response.body()?.count ?: 0
                    runOnUiThread {
                        val tvNotifBadge = findViewById<TextView>(R.id.tvNotifBadge)
                        if (tvNotifBadge != null) {
                            if (count > 0) {
                                tvNotifBadge.visibility = View.VISIBLE
                                tvNotifBadge.text = if (count > 99) "99+" else count.toString()
                            } else {
                                tvNotifBadge.visibility = View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications: ${e.message}")
            }
        }
    }
    
    /**
     * Setup RecyclerView untuk list jurusan
     * Shows only jurusan scheduled for today (daily filtering)
     */
    private fun setupJurusanList() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            Log.w(TAG, "No auth token, cannot load jurusan schedules")
            return
        }
        
        lifecycleScope.launch {
            repository.getDhuhaToday(token).fold(
                onSuccess = { dhuhaData ->
                    runOnUiThread {
                        val safeData = dhuhaData ?: emptyList()
                        jurusanAdapter = JurusanAdapter(safeData)
                        
                        rvJurusan.apply {
                            layoutManager = LinearLayoutManager(this@BerandaAdminActivity)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                        
                        Log.d(TAG, "Loaded ${safeData.size} jurusan schedules for today")
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading jurusan schedules: ${error.message}")
                    // Fallback to empty list
                    runOnUiThread {
                        jurusanAdapter = JurusanAdapter(emptyList<DhuhaJurusanData>())
                        
                        rvJurusan.apply {
                            layoutManager = LinearLayoutManager(this@BerandaAdminActivity)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                }
            )
        }
    }
}