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
import com.xirpl2.SASMobile.model.QRCodeData
import com.xirpl2.SASMobile.repository.QRCodeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity untuk menampilkan QR Code siswa
 * Siswa dapat menunjukkan QR code ini untuk dipindai oleh guru/admin
 */
class StudentQRActivity : AppCompatActivity() {

    private lateinit var ivQRCode: ImageView
    private lateinit var tvJenisSholat: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var containerQR: View
    private lateinit var containerError: View
    private lateinit var tvErrorMessage: TextView

    private val repository = QRCodeRepository()
    private var countDownTimer: CountDownTimer? = null
    private var currentQRData: QRCodeData? = null
    
    private val TAG = "StudentQRActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_qr)
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

    private fun loadQRCode() {
        val token = getAuthToken()
        
        if (token.isEmpty()) {
            showError("Sesi telah berakhir, silakan login kembali")
            return
        }

        showLoading()
        
        lifecycleScope.launch {
            repository.generateQRCode(token).fold(
                onSuccess = { qrData ->
                    runOnUiThread {
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
        
        // Update status
        tvStatus.text = "Tunjukkan QR code ini ke guru"
        tvStatus.setTextColor(getColor(R.color.text_secondary))
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
        tvStatus.text = "Tekan tombol refresh untuk mendapatkan QR code baru"
        tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
        
        // Dim the QR code to indicate it's expired
        ivQRCode.alpha = 0.5f
        
        // Show refresh button prominently
        btnRefresh.visibility = View.VISIBLE
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
