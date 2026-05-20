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
    private lateinit var tvHadirHariIniSub: TextView
    private lateinit var tvIzinSakitSub: TextView
    private lateinit var tvKehadiranSub: TextView
    private lateinit var tvNamaSholat: TextView
    private lateinit var tvWaktuSholat: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var btnQRCode: View
    private lateinit var cardJadwalDhuha: View
    private lateinit var rvDhuhaSchedule: RecyclerView
    private lateinit var rvJurusan: RecyclerView

    private lateinit var jurusanAdapter: JurusanAdapter
    private lateinit var dhuhaScheduleAdapter: DhuhaScheduleAdapter
    
    private val repository = BerandaRepository()
    private val TAG = "BerandaAdminActivity"
    
    
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

        findViewById<View>(R.id.topBarContent)?.let { topBar ->
            applyEdgeToEdge(topBar)
        }

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()

        // Load data safely
        if (!isFinishing && !isDestroyed) {
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
        tvHadirHariIniSub = findViewById(R.id.tvHadirHariIniSub)
        tvIzinSakitSub = findViewById(R.id.tvIzinSakitSub)
        tvKehadiranSub = findViewById(R.id.tvKehadiranSub)
        tvNamaSholat = findViewById(R.id.tvNamaSholat)
        tvWaktuSholat = findViewById(R.id.tvWaktuSholat)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        btnQRCode = findViewById(R.id.btnQRCode)
        cardJadwalDhuha = findViewById(R.id.cardJadwalDhuha)
        rvDhuhaSchedule = findViewById(R.id.rvDhuhaSchedule)

        btnQRCode.setOnClickListener {
            startActivity(Intent(this, QRCodeAdminActivity::class.java))
        }
    }
    
    private fun loadStatistikFromAPI() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        lifecycleScope.launch {
            repository.getStatistics(token, today).fold(
                onSuccess = { globalStats ->
                    if (!isFinishing && !isDestroyed) {
                        tvTotalSiswaValue.text = globalStats.total_siswa.toString()
                        tvHadirHariIniValue.text = globalStats.total_kehadiran_hari_ini.toString()
                        val izinSakit = globalStats.total_izin_hari_ini + globalStats.total_sakit_hari_ini
                        tvIzinSakitValue.text = izinSakit.toString()
                        tvKehadiranValue.text = globalStats.total_alpha_hari_ini.toString()
                        val todayLabel = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id")).format(java.util.Date())
                        tvHadirHariIniSub.text = todayLabel
                        tvIzinSakitSub.text = todayLabel
                        tvKehadiranSub.text = todayLabel
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load statistics: ${error.message}")
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        setupJadwalSholat()
        setupJurusanList()
        loadStatistikFromAPI()
        loadNotificationCount()
        
        clockHandler.postDelayed(clockRunnable, clockRefreshInterval)
    }
    
    override fun onPause() {
        super.onPause()
        
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun setupJadwalSholat() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    val upcomingPrayer = JadwalSholatHelper.getUpcomingPrayerFromList(list)
                    if (!isFinishing && !isDestroyed) {
                        if (upcomingPrayer != null) {
                            tvNamaSholat.text = upcomingPrayer.namaSholat
                            tvWaktuSholat.text = "Waktu : ${upcomingPrayer.jamMulai} - ${upcomingPrayer.jamSelesai}"

                            when (upcomingPrayer.status) {
                                com.xirpl2.SASMobile.model.StatusSholat.SEDANG_BERLANGSUNG -> {
                                    tvStatusBadge.visibility = View.VISIBLE
                                    tvStatusBadge.text = "Berlangsung"
                                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_berlangsung)
                                }
                                com.xirpl2.SASMobile.model.StatusSholat.AKAN_DATANG -> {
                                    tvStatusBadge.visibility = View.VISIBLE
                                    tvStatusBadge.text = "Akan Datang"
                                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_akandatang)
                                }
                                else -> {
                                    tvStatusBadge.visibility = View.GONE
                                }
                            }

                            val isDhuhaActive = upcomingPrayer.namaSholat.equals("Dhuha", ignoreCase = true) &&
                                    upcomingPrayer.status == com.xirpl2.SASMobile.model.StatusSholat.SEDANG_BERLANGSUNG
                            cardJadwalDhuha.visibility = if (isDhuhaActive) View.VISIBLE else View.GONE
                            if (isDhuhaActive) {
                                loadDhuhaSchedule()
                            }
                        } else {
                            tvNamaSholat.text = "-"
                            tvWaktuSholat.text = "Tidak ada jadwal yang akan datang"
                            tvStatusBadge.visibility = View.GONE
                            cardJadwalDhuha.visibility = View.GONE
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load jadwal sholat: ${error.message}")
                }
            )
        }
    }

    private fun loadDhuhaSchedule() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalDhuhaKeahlian(token).fold(
                onSuccess = { data ->
                    if (!isFinishing && !isDestroyed) {
                        dhuhaScheduleAdapter = DhuhaScheduleAdapter(data)
                        rvDhuhaSchedule.apply {
                            layoutManager = LinearLayoutManager(this@BerandaAdminActivity)
                            adapter = dhuhaScheduleAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load dhuha schedule: ${error.message}")
                }
            )
        }
    }
    
    private fun loadNotificationCount() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getNotifications("Bearer $token")
                if (response.isSuccessful) {
                    val count = response.body()?.pagination?.totalRecords ?: 0
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
                Log.w(TAG, "Failed to load notification count: ${e.message}")
            }
        }
    }
    
    private fun setupJurusanList() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            repository.getDhuhaToday(token).fold(
                onSuccess = { dhuhaData ->
                    if (!isFinishing && !isDestroyed) {
                        val safeData = dhuhaData ?: emptyList()
                        jurusanAdapter = JurusanAdapter(safeData)
                        
                        rvJurusan.apply {
                            layoutManager = LinearLayoutManager(this@BerandaAdminActivity)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                },
                onFailure = { error ->
                    if (!isFinishing && !isDestroyed) {
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

    override fun onDestroy() {
        clockHandler.removeCallbacks(clockRunnable)
        if (::rvJurusan.isInitialized) {
            rvJurusan.adapter = null
        }
        if (::rvDhuhaSchedule.isInitialized) {
            rvDhuhaSchedule.adapter = null
        }
        super.onDestroy()
    }
}