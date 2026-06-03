package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.DhuhaJurusanData
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch

class BerandaGuruActivity : BaseAdminActivity() {

    private lateinit var tvTotalSiswaValue: TextView
    private lateinit var tvHadirHariIniValue: TextView
    private lateinit var tvIzinSakitValue: TextView
    private lateinit var tvAlphaValue: TextView
    private lateinit var tvHadirHariIniSub: TextView
    private lateinit var tvIzinSakitSub: TextView
    private lateinit var tvAlphaSub: TextView
    private lateinit var tvNamaSholat: TextView
    private lateinit var tvWaktuSholat: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var btnQRCode: View
    private lateinit var cardJadwalDhuha: View
    private lateinit var rvDhuhaSchedule: RecyclerView
    private lateinit var rvJurusan: RecyclerView

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var jurusanAdapter: JurusanAdapter
    private lateinit var dhuhaScheduleAdapter: DhuhaScheduleAdapter
    
    private val repository = BerandaRepository()
    private val TAG = "BerandaGuruActivity"
    
    
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
        setContentView(R.layout.activity_beranda_guru)
        setupStatusBar()

        findViewById<View>(R.id.topBarContent)?.let { topBar ->
            applyEdgeToEdge(topBar)
        }

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadStatistik()
            setupJadwalSholat()
            setupJurusanList()
        }

        findViewById<android.view.View>(R.id.main)?.post {
            loadStatistik()
            setupJadwalSholat()
            setupJurusanList()
        }
    }
    
    private fun initializeViews() {
        tvTotalSiswaValue = findViewById(R.id.tvTotalSiswaValue)
        tvHadirHariIniValue = findViewById(R.id.tvHadirHariIniValue)
        tvIzinSakitValue = findViewById(R.id.tvIzinSakitValue)
        tvAlphaValue = findViewById(R.id.tvAlphaValue)
        tvHadirHariIniSub = findViewById(R.id.tvHadirHariIniSub)
        tvIzinSakitSub = findViewById(R.id.tvIzinSakitSub)
        tvAlphaSub = findViewById(R.id.tvAlphaSub)
        tvNamaSholat = findViewById(R.id.tvNamaSholat)
        tvWaktuSholat = findViewById(R.id.tvWaktuSholat)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        btnQRCode = findViewById(R.id.btnQRCode)
        cardJadwalDhuha = findViewById(R.id.cardJadwalDhuha)
        rvDhuhaSchedule = findViewById(R.id.rvDhuhaSchedule)
        rvJurusan = findViewById(R.id.rvJurusan)

        btnQRCode.setOnClickListener {
            startActivity(Intent(this, QRCodeAdminActivity::class.java))
        }
    }
    
    private fun loadStatistik() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        lifecycleScope.launch {
            repository.getStatistics(token, today).fold(
                onSuccess = { stats ->
                    if (!isFinishing && !isDestroyed) {
                        tvTotalSiswaValue.text = stats.total_siswa.toString()
                        tvHadirHariIniValue.text = stats.total_kehadiran_hari_ini.toString()
                        val izinSakit = stats.total_izin_hari_ini + stats.total_sakit_hari_ini
                        tvIzinSakitValue.text = izinSakit.toString()
                        tvAlphaValue.text = stats.total_alpha_hari_ini.toString()
                        val todayLabel = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id")).format(java.util.Date())
                        tvHadirHariIniSub.text = todayLabel
                        tvIzinSakitSub.text = todayLabel
                        tvAlphaSub.text = todayLabel
                    }
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load statistik: ${error.message}")
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                }
            )
        }
    }

    private fun setupJadwalSholat() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getClosestPrayerSchedule(token).fold(
                onSuccess = { closestData ->
                    if (!isFinishing && !isDestroyed) {
                        val activePrayer = closestData.current ?: closestData.next
                        val isCurrentlyActive = closestData.current != null

                        if (activePrayer != null && activePrayer.waktuSholat != null) {
                            val namaSholat = activePrayer.waktuSholat.jenisSholat?.namaJenis ?: "Sholat"
                            val jamMulai = activePrayer.waktuSholat.waktuMulai ?: ""
                            val jamSelesai = activePrayer.waktuSholat.waktuSelesai ?: ""

                            tvNamaSholat.text = namaSholat
                            tvWaktuSholat.text = "Waktu : $jamMulai - $jamSelesai"

                            if (isCurrentlyActive) {
                                tvStatusBadge.visibility = View.VISIBLE
                                tvStatusBadge.text = "Berlangsung"
                                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_berlangsung)
                            } else {
                                tvStatusBadge.visibility = View.VISIBLE
                                tvStatusBadge.text = "Akan Datang"
                                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_akandatang)
                            }

                            val isDhuhaActive = namaSholat.equals("Dhuha", ignoreCase = true) && isCurrentlyActive
                            cardJadwalDhuha.visibility = if (isDhuhaActive) View.VISIBLE else View.GONE
                            if (isDhuhaActive) {
                                loadDhuhaSchedule()
                            }
                        } else {
                            tvNamaSholat.text = "-"
                            tvWaktuSholat.text = "Tidak ada jadwal yang akan datang dalam\nwaktu dekat"
                            tvStatusBadge.visibility = View.GONE
                            cardJadwalDhuha.visibility = View.GONE
                        }
                    }
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load jadwal sholat: ${error.message}")
                    if (!isFinishing && !isDestroyed) {
                        tvNamaSholat.text = "-"
                        tvWaktuSholat.text = "Gagal memuat jadwal"
                        tvStatusBadge.visibility = View.GONE
                    }
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
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
                        dhuhaScheduleAdapter = DhuhaScheduleAdapter()
                        dhuhaScheduleAdapter.submitList(data)
                        rvDhuhaSchedule.apply {
                            layoutManager = LinearLayoutManager(this@BerandaGuruActivity)
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
    
    override fun onResume() {
        super.onResume()
        loadStatistik()
        setupJadwalSholat()
        setupJurusanList()
        clockHandler.postDelayed(clockRunnable, clockRefreshInterval)
    }
    
    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }
    
    private fun setupJurusanList() {
        val token = getAuthToken()
        
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            repository.getDhuhaToday(token).fold(
                onSuccess = { dhuhaData ->
                    if (!isFinishing && !isDestroyed) {
                        val safeData = dhuhaData ?: emptyList()
                        jurusanAdapter = JurusanAdapter(safeData)

                        rvJurusan.apply {
                            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@BerandaGuruActivity, 2)
                            adapter = jurusanAdapter
                            isNestedScrollingEnabled = false
                        }
                    }
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    if (!isFinishing && !isDestroyed) {
                        jurusanAdapter = JurusanAdapter(emptyList<DhuhaJurusanData>())

                        rvJurusan.apply {
                            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@BerandaGuruActivity, 2)
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
