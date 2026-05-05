package com.xirpl2.SASMobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.QRCodeData
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.repository.QRCodeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StaffQRActivity : AppCompatActivity() {

    private lateinit var ivQRCode: ImageView
    private lateinit var tvJenisSholat: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var containerQR: View
    private lateinit var containerError: View
    private lateinit var tvErrorMessage: TextView

    private val repository = QRCodeRepository()
    private val berandaRepository = BerandaRepository()
    private var countDownTimer: CountDownTimer? = null
    private var currentQRData: QRCodeData? = null
    
    private val TAG = "StaffQRActivity"

    private val allowedPrayers = JadwalSholatHelper.ALLOWED_PRAYERS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_qr)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        initializeViews()
        setupClickListeners()
        
        
        loadQRCode()
    }

    private fun initializeViews() {
        ivQRCode = findViewById(R.id.ivQRCode)
        tvJenisSholat = findViewById(R.id.tvJenisSholat)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvStatus = findViewById(R.id.tvStatus)
        tvInstructions = findViewById(R.id.tvInstructions)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)
        containerQR = findViewById(R.id.containerQR)
        containerError = findViewById(R.id.containerError)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
    }

    private fun setupClickListeners() {
        
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        
        btnRefresh.setOnClickListener {
            loadQRCode()
        }

        
        findViewById<MaterialButton>(R.id.btnRetry).setOnClickListener {
            loadQRCode()
        }
    }

    private fun findUpcomingPrayerFromAPI(jadwalList: List<JadwalSholatData>): JadwalSholat? {
        val hariIni = getHariIni()
        
        
        for (jadwal in jadwalList) {
            
            if (allowedPrayers.none { it.equals(jadwal.jenis_sholat, ignoreCase = true) }) {
                continue
            }

            
            if (jadwal.hari != null && !jadwal.hari.equals(hariIni, ignoreCase = true)) {
                continue
            }

            val status = getStatusFromAPI(jadwal.jam_mulai, jadwal.jam_selesai)
            
            if (status == StatusSholat.SEDANG_BERLANGSUNG || 
                status == StatusSholat.AKAN_DATANG) {
                return JadwalSholat(
                    namaSholat = jadwal.jenis_sholat,
                    jamMulai = jadwal.jam_mulai,
                    jamSelesai = jadwal.jam_selesai,
                    status = status
                )
            }
        }
        
        
        
        val todaySchedules = jadwalList.filter { 
            it.hari == null || it.hari.equals(hariIni, ignoreCase = true) 
        }
        
        val lastJadwal = todaySchedules.lastOrNull()
        return lastJadwal?.let {
            val status = getStatusFromAPI(it.jam_mulai, it.jam_selesai)
            JadwalSholat(
                namaSholat = it.jenis_sholat,
                jamMulai = it.jam_mulai,
                jamSelesai = it.jam_selesai,
                status = status
            )
        }
    }
    
    private fun getHariIni(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Senin"
        }
    }
    
    private fun getStatusFromAPI(jamMulai: String, jamSelesai: String): StatusSholat {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        
        val mulaiParts = jamMulai.split(":")
        val mulaiHour = mulaiParts[0].toInt()
        val mulaiMinute = mulaiParts[1].toInt()
        val mulaiInMinutes = mulaiHour * 60 + mulaiMinute
        
        
        val selesaiParts = jamSelesai.split(":")
        val selesaiHour = selesaiParts[0].toInt()
        val selesaiMinute = selesaiParts[1].toInt()
        val selesaiInMinutes = selesaiHour * 60 + selesaiMinute
        
        return when {
            currentTimeInMinutes in mulaiInMinutes..selesaiInMinutes -> StatusSholat.SEDANG_BERLANGSUNG
            currentTimeInMinutes < mulaiInMinutes -> StatusSholat.AKAN_DATANG
            else -> StatusSholat.SELESAI
        }
    }
    
    private fun loadQRCode() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            showError("Sesi telah berakhir, silakan login kembali")
            return
        }
        
        showLoading()
        
        lifecycleScope.launch {
            
            berandaRepository.getJadwalSholat(token).fold(
                onSuccess = { jadwalList ->
                    
                    val upcomingPrayer = findUpcomingPrayerFromAPI(jadwalList)
                    
                    if (upcomingPrayer == null) {
                        runOnUiThread {
                            showError("Tidak ada jadwal sholat saat ini")
                        }
                        return@fold
                    }
                    
                    
                    
                        
                            
                        
                        
                    
                    
                    
                    if (upcomingPrayer.status == StatusSholat.SELESAI) {
                        runOnUiThread {
                            showError("Waktu sholat ${upcomingPrayer.namaSholat} telah berakhir.\n\nQR Code tidak tersedia di luar waktu sholat.")
                        }
                        return@fold
                    }
                    
                    
                    generateQRCode(token)
                },
                onFailure = { error ->
                    runOnUiThread {
                        showError("Gagal memuat jadwal sholat: ${error.message}")
                    }
                }
            )
        }
    }
    
    private fun generateQRCode(token: String) {
        lifecycleScope.launch {
            repository.generateQRCode(token).fold(
                onSuccess = { qrData ->
                    runOnUiThread {
                        
                        if (!allowedPrayers.contains(qrData.jenis_sholat)) {
                            showError("QR Code hanya tersedia untuk sholat Dhuha, Dhuhur, dan Jumat.")
                            return@runOnUiThread
                        }
                        
                        currentQRData = qrData
                        displayQRCode(qrData)
                        showQRCode()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        showError(error.message ?: "Gagal generate QR code")
                    }
                }
            )
        }
    }

    private fun displayQRCode(qrData: QRCodeData) {
        
        tvJenisSholat.text = "Sholat ${qrData.jenis_sholat}"
        
        
        val bitmap = decodeBase64ToBitmap(qrData.qr_code)
        if (bitmap != null) {
            ivQRCode.setImageBitmap(bitmap)
        } else {
            showError("Gagal memuat gambar QR code")
            return
        }

        
        startCountdown(qrData.expires_at)
        
        
        tvStatus.text = "QR Code Aktif"
        tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        
        
        tvInstructions.text = "Tampilkan QR code ini di layar/proyektor.\nSiswa dapat memindai untuk absensi."
        tvInstructions.visibility = View.VISIBLE
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun startCountdown(expiresAt: String) {
        
        countDownTimer?.cancel()
        
        try {
            
            val expiryTime = parseExpiryTime(expiresAt)
            val currentTime = System.currentTimeMillis()
            val remainingTime = expiryTime - currentTime
            
            if (remainingTime <= 0) {
                onQRCodeExpired()
                return
            }
            
            countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = (millisUntilFinished / 1000) / 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    tvCountdown.text = String.format("Berlaku: %02d:%02d", minutes, seconds)
                    
                    
                    if (millisUntilFinished < 60000) {
                        tvCountdown.setTextColor(getColor(android.R.color.holo_red_light))
                    } else {
                        tvCountdown.setTextColor(getColor(android.R.color.holo_green_dark))
                    }
                }

                override fun onFinish() {
                    onQRCodeExpired()
                }
            }.start()
            
        } catch (e: Exception) {
            tvCountdown.text = "Waktu tidak tersedia"
        }
    }

    private fun parseExpiryTime(expiresAt: String): Long {
        return try {
            
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(expiresAt)?.time ?: 0L
        } catch (e: Exception) {
            try {
                
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                format.parse(expiresAt)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun onQRCodeExpired() {
        tvCountdown.text = "QR Code Kadaluarsa"
        tvCountdown.setTextColor(getColor(android.R.color.holo_red_dark))
        tvStatus.text = "QR Code Expired"
        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        tvInstructions.text = "Tekan tombol refresh untuk generate QR code baru"
        
        
        ivQRCode.alpha = 0.5f
        
        
        btnRefresh.visibility = View.VISIBLE
        
        
        android.os.Handler(mainLooper).postDelayed({
            if (!isFinishing && !isDestroyed) {
                loadQRCode()
            }
        }, 2000)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        containerQR.visibility = View.GONE
        containerError.visibility = View.GONE
        btnRefresh.visibility = View.GONE
    }

    private fun showQRCode() {
        progressBar.visibility = View.GONE
        containerQR.visibility = View.VISIBLE
        containerError.visibility = View.GONE
        btnRefresh.visibility = View.VISIBLE
        ivQRCode.alpha = 1.0f
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        containerQR.visibility = View.GONE
        containerError.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }

    private fun getAuthToken(): String {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
