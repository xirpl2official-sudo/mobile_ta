package com.xirpl2.SASMobile

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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.QRCodeData
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.repository.QRCodeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class QRCodeAdminActivity : BaseAdminActivity() {

    private lateinit var ivQRCode: ImageView
    private lateinit var tvJenisSholat: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var containerQR: View
    
    // Manual Code Views
    private lateinit var tvManualCode: TextView
    private lateinit var tvCodeExpires: TextView
    private lateinit var cardManualCode: View

    private val qrRepository = QRCodeRepository()
    private val berandaRepository = BerandaRepository()
    private var countDownTimer: CountDownTimer? = null
    private var codeTimer: CountDownTimer? = null
    
    private val allowedPrayers = JadwalSholatHelper.ALLOWED_PRAYERS

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.QR_CODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_admin)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupClickListeners()
        
        loadQRCode()
        loadAttendanceCode()
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
        
        tvManualCode = findViewById(R.id.tvManualCode)
        tvCodeExpires = findViewById(R.id.tvCodeExpires)
        cardManualCode = findViewById(R.id.cardManualCode)
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadQRCode()
            loadAttendanceCode()
        }
    }

    private fun loadAttendanceCode() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.generateAttendanceCode("Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        runOnUiThread {
                            displayAttendanceCode(data.code, data.expiresIn)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("QRCodeAdmin", "Failed to load attendance code", e)
            }
        }
    }

    private fun displayAttendanceCode(code: String, expiresIn: Int) {
        tvManualCode.text = code
        cardManualCode.visibility = View.VISIBLE
        
        codeTimer?.cancel()
        codeTimer = object : CountDownTimer(expiresIn * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvCodeExpires.text = "Kode berubah dalam $seconds detik"
            }
            override fun onFinish() {
                loadAttendanceCode()
            }
        }.start()
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
                    val upcomingPrayer = JadwalSholatHelper.getUpcomingPrayerFromList(jadwalList)
                    
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

    private fun getStatusFromAPI(jamMulai: String, jamSelesai: String): StatusSholat {
        return JadwalSholatHelper.getStatusSholat(jamMulai, jamSelesai)
    }

    private fun generateQRCode(token: String) {
        lifecycleScope.launch {
            qrRepository.generateQRCode(token).fold(
                onSuccess = { qrData ->
                    runOnUiThread {
                        if (!allowedPrayers.contains(qrData.jenis_sholat)) {
                            showError("QR Code hanya tersedia untuk sholat Dhuha, Dhuhur, dan Jumat.")
                            return@runOnUiThread
                        }
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
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val pureBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun startCountdown(expiresAt: String) {
        countDownTimer?.cancel()
        try {
            val expiryTime = parseExpiryTime(expiresAt)
            val remainingTime = expiryTime - System.currentTimeMillis()
            
            if (remainingTime <= 0) {
                onQRCodeExpired()
                return
            }
            
            countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = (millisUntilFinished / 1000) / 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    tvCountdown.text = String.format("Berlaku: %02d:%02d", minutes, seconds)
                    tvCountdown.setTextColor(getColor(if (millisUntilFinished < 60000) android.R.color.holo_red_light else android.R.color.holo_green_dark))
                }
                override fun onFinish() { onQRCodeExpired() }
            }.start()
        } catch (e: Exception) { tvCountdown.text = "Waktu tidak tersedia" }
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
            } catch (e2: Exception) { 0L }
        }
    }

    private fun onQRCodeExpired() {
        tvCountdown.text = "QR Code Kadaluarsa"
        tvCountdown.setTextColor(getColor(android.R.color.holo_red_dark))
        tvStatus.text = "QR Code Expired"
        tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        ivQRCode.alpha = 0.5f
        btnRefresh.visibility = View.VISIBLE
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        containerQR.visibility = View.GONE
    }

    private fun showQRCode() {
        progressBar.visibility = View.GONE
        containerQR.visibility = View.VISIBLE
        ivQRCode.alpha = 1.0f
    }

    private fun showError(message: String) {
        runOnUiThread {
            progressBar.visibility = View.GONE
            containerQR.visibility = View.VISIBLE
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        codeTimer?.cancel()
    }
}