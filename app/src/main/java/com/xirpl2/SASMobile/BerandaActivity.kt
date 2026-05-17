package com.xirpl2.SASMobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
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

        
        initializeViews()
        
        
        setupPopupMenu()
        
        
        setupJadwalSholat()
        setupRiwayatAbsensi()

        setupAbsensiButton()
        setupNotificationButton()
        
        
        loadStatistics()
        
        
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
        
        
        jadwalAdapter = JadwalSholatAdapter(emptyList())

        
        rvJadwalSholat.apply {
            layoutManager = LinearLayoutManager(this@BerandaActivity)
            adapter = jadwalAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupRiwayatAbsensi() {
        
        val emptyList = emptyList<RiwayatAbsensi>()

        
        riwayatAdapter = RiwayatAbsensiAdapter(emptyList)

        
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
                
                val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()
                
                
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                
                
                val intent = Intent(this, MasukActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
    
    private fun loadDataFromAPI() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            
            return
        }
        
        lifecycleScope.launch {
            
            loadJadwalSholatFromAPI(token)
            
            
            loadRiwayatAbsensiFromAPI(token)
        }
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
                    jadwalAdapter = JadwalSholatAdapter(jadwalList)
                    rvJadwalSholat.adapter = jadwalAdapter
                }
            },
            onFailure = { error ->
                
            }
        )
    }
    
    private suspend fun loadRiwayatAbsensiFromAPI(token: String) {
        repository.getHistorySiswa(token, 0).fold(
            onSuccess = { historyData ->
                
                val absensiList = historyData.absensi ?: emptyList()
                
                
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
                    riwayatAdapter = RiwayatAbsensiAdapter(riwayatList)
                    rvRiwayatAbsensi.adapter = riwayatAdapter
                }
            },
            onFailure = { error ->
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
        jadwalAdapter = JadwalSholatAdapter(newData)
        rvJadwalSholat.adapter = jadwalAdapter
    }

    fun refreshRiwayatAbsensi(newData: List<RiwayatAbsensi>) {
        riwayatAdapter = RiwayatAbsensiAdapter(newData)
        rvRiwayatAbsensi.adapter = riwayatAdapter
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
                        
                        if (riwayatList.isEmpty()) {
                        } else {
                            riwayatAdapter = RiwayatAbsensiAdapter(riwayatList)
                            rvRiwayatAbsensi.adapter = riwayatAdapter
                        }
                    }
                },
                onFailure = { error ->
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateNotificationCounter()
        
        
        val token = getAuthToken()
        if (token.isNotEmpty()) {
            lifecycleScope.launch {
                loadJadwalSholatFromAPI(token)
                
                loadRiwayatFromAPI() 
                loadStatistics()
            }
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
        registerReceiver(notificationCounterBroadcast, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
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
}