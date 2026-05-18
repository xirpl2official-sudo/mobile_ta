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
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BerandaActivity : BaseActivity() {

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
    
    // Guard against duplicate loadAllData calls from onCreate+onResume race condition
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beranda)

        // Basic initialization
        initializeViews()
        setupPopupMenu()
        setupJadwalSholat()
        setupRiwayatAbsensi()
        setupAbsensiButton()
        setupNotificationButton()
        
        // Load data is handled in onResume to prevent duplicate calls and race conditions
    }

    private fun loadAllData() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            return
        }
        
        loadStatistics()
        loadRiwayatFromAPI()
        
        lifecycleScope.launch {
            loadJadwalSholatFromAPI(token)
        }
    }
    
    private fun initializeViews() {
        rvJadwalSholat = findViewById(R.id.rvJadwalSholat)
        rvRiwayatAbsensi = findViewById(R.id.rvRiwayatAbsensi)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        tvHadirValue = findViewById(R.id.tvHadirValue)
        tvStatistikValue = findViewById(R.id.tvStatistikValue)
        notificationCounter = findViewById(R.id.notificationCounter)
    }

    private fun setupNotificationButton() {
        val iconNotifikasi = findViewById<ImageView>(R.id.iconNotifikasi)
        
        iconNotifikasi.setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }
        
        
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
            
            startActivity(Intent(this@BerandaActivity, ScanQrActivity::class.java))
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardHadir).setOnClickListener {
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
    }
    
    private fun getJenisKelaminFromStorage(): JadwalSholatHelper.JenisKelamin {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val jenisKelaminStr = sharedPref.getString("jenis_kelamin", "L") ?: "L"
        
        return if (jenisKelaminStr == "P") {
            JadwalSholatHelper.JenisKelamin.PEREMPUAN
        } else {
            JadwalSholatHelper.JenisKelamin.LAKI_LAKI
        }
    }
    
    private fun setupPopupMenu() {
        val iconMenu = findViewById<ImageView>(R.id.iconMenu)
        iconMenu.setOnClickListener {
            showPopupMenu(it)
        }
    }
    
    private fun showPopupMenu(anchorView: android.view.View) {
        
        dismissPopupMenu()
        
        
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_menu, null)
        
        
        val (nama, nis) = getUserDataFromStorage()
        
        
        val tvStudentName = popupView.findViewById<TextView>(R.id.tvStudentName)
        val tvStudentNIS = popupView.findViewById<TextView>(R.id.tvStudentNIS)
        tvStudentName.text = nama
        tvStudentNIS.text = nis
        
        
        val btnPengajuanIzin = popupView.findViewById<LinearLayout>(R.id.btnPengajuanIzin)
        val btnSettings = popupView.findViewById<LinearLayout>(R.id.btnSettings)
        val btnLogout = popupView.findViewById<LinearLayout>(R.id.btnLogout)

        btnPengajuanIzin.setOnClickListener {
            dismissPopupMenu()
            
            val intent = Intent(this, PengajuanIzinActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            dismissPopupMenu()
            
            val intent = Intent(this, PengaturanAkunActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            dismissPopupMenu()
            handleLogout()
        }
        
        
        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true 
        )
        
        
        popupWindow?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow?.isOutsideTouchable = true
        
        
        popupWindow?.showAsDropDown(anchorView, 0, 10, Gravity.START)
    }
    
    private fun dismissPopupMenu() {
        popupWindow?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        popupWindow = null
    }
    
    private fun getUserDataFromStorage(): Pair<String, String> {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val nama = sharedPref.getString("nama_siswa", "Nama Siswa") ?: "Nama Siswa"
        val nis = sharedPref.getString("nis", "0000000000") ?: "0000000000"
        return Pair(nama, nis)
    }
    
    private fun handleLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                val token = getAuthToken()
                
                lifecycleScope.launch(Dispatchers.IO) {
                    // Call logout API to invalidate token on server
                    try {
                        if (token.isNotEmpty()) {
                            RetrofitClient.apiService.logout("Bearer $token")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Logout API call failed (non-critical): ${e.message}")
                    }
                    
                    launch(Dispatchers.Main) {
                        // Clear ALL SharedPreferences stores
                        getSharedPreferences("user_session", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        getSharedPreferences("UserData", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        
                        Toast.makeText(this@BerandaActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@BerandaActivity, MasukActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
    
    private suspend fun loadJadwalSholatFromAPI(token: String) {
        repository.getJadwalSholatToday(token).fold(
            onSuccess = { jadwalDataList ->
                
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
                
                
                runOnUiThread {
                    if (!::jadwalAdapter.isInitialized) {
                        jadwalAdapter = JadwalSholatAdapter()
                        rvJadwalSholat.adapter = jadwalAdapter
                    }
                    jadwalAdapter.submitList(jadwalList)
                }
            },
            onFailure = { error ->
                Log.w(TAG, "Failed to load jadwal sholat: ${error.message}")
            }
        )
    }
    
    private fun loadStatistics() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        lifecycleScope.launch {
            repository.getStatistics(token, today).fold(
                onSuccess = { statistics ->
                    runOnUiThread {
                        
                        tvTotalValue.text = statistics.total_siswa.toString()
                        
                        
                        
                        
                        tvStatistikValue.text = "${(statistics.persentase_kehadiran * 100).toInt()}%"
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load statistics: ${error.message}")
                }
            )
        }
    }
    
    private fun getAuthToken(): String {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
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
                    
                    val absensiList = historyData.absensi ?: emptyList()
                    
                    
                    val hadirCount = absensiList.count { 
                        it.status.uppercase() == "HADIR" 
                    }
                    
                    
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
                    
                    
                    runOnUiThread {
                        
                        tvHadirValue.text = hadirCount.toString()
                        
                        if (riwayatList.isNotEmpty()) {
                            if (!::riwayatAdapter.isInitialized) {
                                riwayatAdapter = RiwayatAbsensiAdapter()
                                rvRiwayatAbsensi.adapter = riwayatAdapter
                            }
                            riwayatAdapter.submitList(riwayatList)
                        }
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to load riwayat absensi: ${error.message}")
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateNotificationCounter()
        
        val token = getAuthToken()
        if (token.isNotEmpty() && !isDataLoaded) {
            isDataLoaded = true
            loadAllData()
        }
        
        
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationCounterBroadcast, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(notificationCounterBroadcast, filter)
        }
    }
    
    override fun onPause() {
        super.onPause()
        notificationCounterBroadcast?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                
            }
        }
    }

    override fun onDestroy() {
        if (::rvJadwalSholat.isInitialized) rvJadwalSholat.adapter = null
        if (::rvRiwayatAbsensi.isInitialized) rvRiwayatAbsensi.adapter = null
        super.onDestroy()
    }
}