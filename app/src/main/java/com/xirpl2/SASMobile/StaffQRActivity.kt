package com.xirpl2.SASMobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
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

/**
 * Activity untuk Staff (Admin/Guru/Wali Kelas) menampilkan QR Code
 * Staff menampilkan QR code ini di layar/proyektor untuk dipindai oleh siswa
 * 
 * Workflow:
 * 1. Staff membuka activity ini saat waktu sholat
 * 2. QR code ditampilkan di layar/proyektor
 * 3. Siswa memindai QR code untuk mencatat kehadiran mereka
 * 4. QR code berlaku selama 5 menit, auto-refresh saat expired
 */
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

    /**
     * Allowed prayer types for QR generation
     * Only Dhuha, Dzuhur, and Jumat are allowed
     * Other prayers (Ashar, Maghrib, Isya, Shubuh) are NOT allowed
     */
    private val allowedPrayers = JadwalSholatHelper.ALLOWED_PRAYERS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_qr)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        initializeViews()
        setupClickListeners()
        
        // Load QR code on start
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
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Refresh button
        btnRefresh.setOnClickListener {
            loadQRCode()
        }

        // Retry button in error state
        findViewById<MaterialButton>(R.id.btnRetry).setOnClickListener {
            loadQRCode()
        }
    }

    private fun findUpcomingPrayerFromAPI(jadwalList: List<JadwalSholatData>): JadwalSholat? {
        val hariIni = getHariIni()
        
        // Find first prayer that is upcoming or currently active AND matches today's day
        for (jadwal in jadwalList) {
            // Skip if not allowed prayer
            if (allowedPrayers.none { it.equals(jadwal.jenis_sholat, ignoreCase = true) }) {
                continue
            }

            // Skip if hari doesn't match and it's not a daily schedule (null)
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
        
        // If no specific schedule found, try finding one specifically for today even if SELESAI
        // to avoid falling back to a different day's schedule
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
    
    /**
     * Get current day name in Indonesian (Senin, Selasa, etc.)
     */
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
    
    /**
     * Get status from API times
     */
    private fun getStatusFromAPI(jamMulai: String, jamSelesai: String): StatusSholat {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        // Parse jam mulai
        val mulaiParts = jamMulai.split(":")
        val mulaiHour = mulaiParts[0].toInt()
        val mulaiMinute = mulaiParts[1].toInt()
        val mulaiInMinutes = mulaiHour * 60 + mulaiMinute
        
        // Parse jam selesai
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
            // First, fetch the jadwal list from API
            berandaRepository.getJadwalSholat(token).fold(
                onSuccess = { jadwalList ->
                    // Find the upcoming prayer from API data
                    val upcomingPrayer = findUpcomingPrayerFromAPI(jadwalList)
                    
                    if (upcomingPrayer == null) {
                        runOnUiThread {
                            showError("Tidak ada jadwal sholat saat ini")
                        }
                        return@fold
                    }
                    
                    // Validate prayer type - only allow Dhuha, Dzuhur, Jumat
                    //if (!allowedPrayers.contains(upcomingPrayer.namaSholat)) {
                        //runOnUiThread {
                            //showError("QR Code hanya tersedia untuk sholat Dhuha, Dhuhur, dan Jumat.\n\nSholat ${upcomingPrayer.namaSholat} tidak memerlukan QR Code.")
                        //}
                        //return@fold
                    //}
                    
                    // Check if prayer is currently active or upcoming
                    if (upcomingPrayer.status == StatusSholat.SELESAI) {
                        runOnUiThread {
                            showError("Waktu sholat ${upcomingPrayer.namaSholat} telah berakhir.\n\nQR Code tidak tersedia di luar waktu sholat.")
                        }
                        return@fold
                    }
                    
                    // Proceed to generate QR code
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
    
    /**
     * Generate QR code after validation
     */
    private fun generateQRCode(token: String) {
        lifecycleScope.launch {
            repository.generateQRCode(token).fold(
                onSuccess = { qrData ->
                    runOnUiThread {
                        // Double-check the returned prayer type from backend
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
                    Log.e(TAG, "Error generating QR code: ${error.message}")
                    runOnUiThread {
                        showError(error.message ?: "Gagal generate QR code")
                    }
                }
            )
        }
    }

    private fun displayQRCode(qrData: QRCodeData) {
        // Display prayer type
        tvJenisSholat.text = "Sholat ${qrData.jenis_sholat}"
        
        // Decode and display QR code image
        val bitmap = decodeBase64ToBitmap(qrData.qr_code)
        if (bitmap != null) {
            ivQRCode.setImageBitmap(bitmap)
        } else {
            showError("Gagal memuat gambar QR code")
            return
        }

        // Start countdown timer
        startCountdown(qrData.expires_at)
        
        // Update status for staff
        tvStatus.text = "QR Code Aktif"
        tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        
        // Show instructions for staff
        tvInstructions.text = "Tampilkan QR code ini di layar/proyektor.\nSiswa dapat memindai untuk absensi."
        tvInstructions.visibility = View.VISIBLE
    }

    /**
     * Decode base64 image string to Bitmap
     * Format: "data:image/png;base64,[PNG_DATA]"
     */
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Remove the data URI prefix if present
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64: ${e.message}")
            null
        }
    }

    /**
     * Start countdown timer based on expiry time
     */
    private fun startCountdown(expiresAt: String) {
        // Cancel any existing timer
        countDownTimer?.cancel()
        
        try {
            // Parse expiry time (ISO 8601 format)
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
                    
                    // Change color when time is running low (< 1 minute)
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
            Log.e(TAG, "Error parsing expiry time: ${e.message}")
            tvCountdown.text = "Waktu tidak tersedia"
        }
    }

    /**
     * Parse ISO 8601 expiry time string to milliseconds
     */
    private fun parseExpiryTime(expiresAt: String): Long {
        return try {
            // Try ISO 8601 format with Z suffix
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(expiresAt)?.time ?: 0L
        } catch (e: Exception) {
            try {
                // Try alternative format with timezone offset
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                format.parse(expiresAt)?.time ?: 0L
            } catch (e2: Exception) {
                Log.e(TAG, "Error parsing expiry time: ${e2.message}")
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
        
        // Dim the QR code to indicate it's expired
        ivQRCode.alpha = 0.5f
        
        // Show refresh button prominently
        btnRefresh.visibility = View.VISIBLE
        
        // Auto-refresh after 2 seconds
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
