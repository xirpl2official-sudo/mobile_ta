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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.model.DuhaJurusanData
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.utils.NotificationCounterManager
import kotlinx.coroutines.launch

class BerandaAdminActivity : BaseAdminActivity() {

    private lateinit var tvTotalSiswaValue: TextView
    private lateinit var tvHadirHariIniValue: TextView
    private lateinit var tvIzinSakitValue: TextView
    private lateinit var tvAlphaValue: TextView
    private lateinit var tvHadirHariIniSub: TextView
    private lateinit var tvIzinSakitSub: TextView
    private lateinit var tvAlphaSub: TextView
    private lateinit var tvNamaSholat: TextView
    private lateinit var tvWaktuSholat: TextView
    private lateinit var layoutUnregisteredWarning: View
    private lateinit var tvUnregisteredCount: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var btnQRCode: View

    // Quick Actions
    private lateinit var btnQuickPresensi: View
    private lateinit var btnQuickLaporan: View
    private lateinit var btnQuickSiswa: View
    private lateinit var btnQuickJadwal: View
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var cardJadwalDuha: View
    private lateinit var rvDuhaSchedule: RecyclerView
    private lateinit var notificationBellContainer: android.widget.FrameLayout
    private lateinit var tvNotificationBadge: TextView
    private lateinit var rvJurusan: RecyclerView

    private lateinit var jurusanAdapter: JurusanAdapter
    private lateinit var DuhaScheduleAdapter: DuhaScheduleAdapter

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
        setupRecyclerViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupQuickActions()

        // Data loads in onResume — no need to load here
    }
    
    private fun initializeViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.blue_theme)
        swipeRefresh.setOnRefreshListener { refreshAllData() }

        rvJurusan = findViewById(R.id.rvJurusan)
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
        cardJadwalDuha = findViewById(R.id.cardJadwalDuha)
        rvDuhaSchedule = findViewById(R.id.rvDuhaSchedule)

        btnQuickPresensi = findViewById(R.id.btnQuickPresensi)
        btnQuickLaporan = findViewById(R.id.btnQuickLaporan)
        btnQuickSiswa = findViewById(R.id.btnQuickSiswa)
        btnQuickJadwal = findViewById(R.id.btnQuickJadwal)

        layoutUnregisteredWarning = findViewById(R.id.layoutUnregisteredWarning)
        tvUnregisteredCount = findViewById(R.id.tvUnregisteredCount)

        btnQRCode.setOnClickListener {
            startActivity(Intent(this, QRCodeAdminActivity::class.java))
        }

        notificationBellContainer = findViewById(R.id.notificationBellContainer)
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge)
        notificationBellContainer.setOnClickListener {
            startActivity(Intent(this, NotificationCenterActivity::class.java))
        }
        NotificationCounterManager.counter.observe(this) { count ->
            if (count > 0) {
                tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
                tvNotificationBadge.visibility = View.VISIBLE
            } else {
                tvNotificationBadge.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerViews() {
        jurusanAdapter = JurusanAdapter()
        rvJurusan.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@BerandaAdminActivity, 2)
            adapter = jurusanAdapter
            isNestedScrollingEnabled = false
        }

        DuhaScheduleAdapter = DuhaScheduleAdapter()
        rvDuhaSchedule.apply {
            layoutManager = LinearLayoutManager(this@BerandaAdminActivity)
            adapter = DuhaScheduleAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun checkUnregisteredStudents() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnregisteredStudents("Bearer $token", pageSize = 1)
                if (response.isSuccessful) {
                    val count = response.body()?.pagination?.total_items ?: 0
                    if (count > 0) {
                        layoutUnregisteredWarning.visibility = View.VISIBLE
                        tvUnregisteredCount.text = "$count siswa belum mendaftarkan perangkat"
                        layoutUnregisteredWarning.setOnClickListener {
                            startActivity(Intent(this@BerandaAdminActivity, SiswaBelumTerdaftarAdminActivity::class.java))
                        }
                    } else {
                        layoutUnregisteredWarning.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unregistered check failed", e)
            }
        }
    }

    private fun setupQuickActions() {
        btnQuickPresensi.setOnClickListener {
            startActivity(Intent(this, PresensiSholatAdminActivity::class.java))
        }
        btnQuickLaporan.setOnClickListener {
            startActivity(Intent(this, LaporanAdminActivity::class.java))
        }
        btnQuickSiswa.setOnClickListener {
            startActivity(Intent(this, DataSiswaAdminActivity::class.java))
        }
        btnQuickJadwal.setOnClickListener {
            startActivity(Intent(this, JadwalSholatAdminActivity::class.java))
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
                        tvAlphaValue.text = globalStats.total_alpha_hari_ini.toString()

                        val todayLabel = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id")).format(java.util.Date())
                        tvHadirHariIniSub.text = todayLabel
                        tvIzinSakitSub.text = todayLabel
                        tvAlphaSub.text = todayLabel
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load statistics: ${error.message}")
                    if (!isFinishing && !isDestroyed) {
                        tvTotalSiswaValue.text = "-"
                        tvHadirHariIniValue.text = "-"
                        tvIzinSakitValue.text = "-"
                        tvAlphaValue.text = "-"
                        showErrorWithRetry("Gagal memuat statistik")
                    }
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()

        NotificationCounterManager.syncFromPreferences(this)

        loadStatistikFromAPI()
        setupJadwalSholat()
        setupJurusanList()
        checkUnregisteredStudents()

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
            repository.getClosestPrayerSchedule(token).fold(
                onSuccess = { closestData ->
                    if (!isFinishing && !isDestroyed) {
                        val activePrayer = closestData.current ?: closestData.next
                        val isCurrentlyActive = closestData.current != null

                        if (activePrayer != null && activePrayer.waktuSholat != null) {
                            val namaSholat = activePrayer.waktuSholat.jenisSholat?.namaJenis ?: "Salat"
                            val jamMulai = activePrayer.waktuSholat.waktuMulai ?: ""
                            val jamSelesai = activePrayer.waktuSholat.waktuSelesai ?: ""

                            tvNamaSholat.text = namaSholat
                            tvWaktuSholat.text = "Waktu : $jamMulai - $jamSelesai WIB"

                            if (isCurrentlyActive) {
                                tvStatusBadge.visibility = View.VISIBLE
                                tvStatusBadge.text = "Berlangsung"
                                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_berlangsung)
                            } else {
                                tvStatusBadge.visibility = View.VISIBLE
                                tvStatusBadge.text = "Akan Datang"
                                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_akandatang)
                            }

                            val isDuhaActive = namaSholat.equals("Duha", ignoreCase = true) && isCurrentlyActive
                            cardJadwalDuha.visibility = if (isDuhaActive) View.VISIBLE else View.GONE
                            if (isDuhaActive) {
                                loadDuhaSchedule()
                            }
                        } else {
                            tvNamaSholat.text = "-"
                            tvWaktuSholat.text = "Tidak ada jadwal yang akan datang"
                            tvStatusBadge.visibility = View.GONE
                            cardJadwalDuha.visibility = View.GONE
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load jadwal sholat: ${error.message}")
                    if (!isFinishing && !isDestroyed) {
                        tvNamaSholat.text = "-"
                        tvWaktuSholat.text = "Gagal memuat jadwal"
                        tvStatusBadge.visibility = View.GONE
                        showErrorWithRetry("Gagal memuat jadwal sholat")
                    }
                }
            )
        }
    }

    private fun refreshAllData() {
        loadStatistikFromAPI()
        setupJadwalSholat()
        setupJurusanList()
        checkUnregisteredStudents()

        // Stop spinner after a short delay to let coroutines start
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500)
            if (!isFinishing && !isDestroyed) {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showErrorWithRetry(message: String) {
        if (isFinishing || isDestroyed) return
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Coba Lagi") { refreshAllData() }
            .setActionTextColor(getColor(R.color.blue_theme))
            .show()
    }

    private fun loadDuhaSchedule() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalDuhaKeahlian(token).fold(
                onSuccess = { data ->
                    if (!isFinishing && !isDestroyed) {
                        DuhaScheduleAdapter.submitList(data)
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load Duha schedule: ${error.message}")
                }
            )
        }
    }
    
    private fun setupJurusanList() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            repository.getDuhaToday(token).fold(
                onSuccess = { DuhaData ->
                    if (!isFinishing && !isDestroyed) {
                        val safeData = DuhaData ?: emptyList()
                        jurusanAdapter.updateData(safeData)
                    }
                },
                onFailure = { error ->
                    if (!isFinishing && !isDestroyed) {
                        jurusanAdapter.updateData(emptyList())
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
        if (::rvDuhaSchedule.isInitialized) {
            rvDuhaSchedule.adapter = null
        }
        super.onDestroy()
    }
}