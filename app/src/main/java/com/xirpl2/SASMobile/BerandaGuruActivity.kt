package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.DhuhaJurusanData
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Guru Dashboard Activity - Read-only view of today's prayer attendance
 * Guru can see statistics but cannot edit any data
 */
class BerandaGuruActivity : BaseAdminActivity() {

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
    private val TAG = "BerandaGuruActivity"
    
    // Real-time clock handler
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRefreshInterval = 30_000L
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
        
        // Setup QR Code Generator Button (guru can generate QR)
        setupQRCodeButton()
        
        // Load data
        findViewById<android.view.View>(R.id.main).post {
            loadStatistik()
            setupJadwalSholat()
            setupJurusanList()
            loadNotificationCount()
        }
    }
    
    private fun initializeViews() {
        tvTotalSiswaValue = findViewById(R.id.tvTotalSiswaValue)
        tvHadirHariIniValue = findViewById(R.id.tvHadirHariIniValue)
        tvIzinSakitValue = findViewById(R.id.tvIzinSakitValue)
        tvKehadiranValue = findViewById(R.id.tvKehadiranValue)
        tvNamaSholat = findViewById(R.id.tvNamaSholat)
        tvWaktuSholat = findViewById(R.id.tvWaktuSholat)
        rvJurusan = findViewById(R.id.rvJurusan)
        btnGenerateQR = findViewById(R.id.btnGenerateQR)
    }
    
    private fun loadStatistik() {
        lifecycleScope.launch {
            repository.getStatistics().fold(
                onSuccess = { stats ->
                    runOnUiThread {
                        tvTotalSiswaValue.text = stats.total_siswa.toString()
                        tvHadirHariIniValue.text = stats.total_kehadiran_hari_ini.toString()
                        val izinSakit = stats.total_izin_hari_ini + stats.total_sakit_hari_ini
                        tvIzinSakitValue.text = izinSakit.toString()
                        tvKehadiranValue.text = stats.total_alpha_hari_ini.toString()
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading stats: ${error.message}")
                }
            )
        }
    }
    
    private fun setupQRCodeButton() {
        val btnGenerateQR = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerateQR)
        btnGenerateQR?.setOnClickListener {
            startActivity(Intent(this@BerandaGuruActivity, StaffQRActivity::class.java))
        }
    }
    
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
                            btnGenerateQR.visibility = android.view.View.VISIBLE
                        } else {
                            tvNamaSholat.text = "-"
                            tvWaktuSholat.text = "Tidak ada jadwal yang akan datang dalam\nwaktu dekat"
                            btnGenerateQR.visibility = android.view.View.GONE
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading jadwal: ${error.message}")
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadStatistik()
        setupJadwalSholat()
        setupJurusanList()
        loadNotificationCount()
        clockHandler.postDelayed(clockRunnable, clockRefreshInterval)
    }
    
    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }
    
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
                                tvNotifBadge.visibility = android.view.View.VISIBLE
                                tvNotifBadge.text = if (count > 99) "99+" else count.toString()
                            } else {
                                tvNotifBadge.visibility = android.view.View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications: ${e.message}")
            }
        }
    }
    
    private fun setupJurusanList() {
        val token = getAuthToken()
        
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            repository.getDhuhaToday(token).fold(
                onSuccess = { dhuhaData ->
                    runOnUiThread {
                        val safeData = dhuhaData ?: emptyList()
                        jurusanAdapter = JurusanAdapter(safeData)
                        
                        rvJurusan.apply {
                            layoutManager = LinearLayoutManager(this@BerandaGuruActivity)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading jurusan schedules: ${error.message}")
                    // Fallback to empty list
                    runOnUiThread {
                        jurusanAdapter = JurusanAdapter(emptyList<DhuhaJurusanData>())
                        
                        rvJurusan.apply {
                            layoutManager = LinearLayoutManager(this@BerandaGuruActivity)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                }
            )
        }
    }
}
