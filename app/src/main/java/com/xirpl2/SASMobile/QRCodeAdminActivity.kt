package com.xirpl2.SASMobile

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.OutputStream
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

    // Download button
    private lateinit var btnDownload: MaterialButton

    private var countDownTimer: CountDownTimer? = null
    private var codeTimer: CountDownTimer? = null
    private var autoRefreshJob: Job? = null
    private var codeRefreshJob: Job? = null
    private var currentBitmap: Bitmap? = null

    private val allowedPrayers = JadwalSholatHelper.ALLOWED_PRAYERS

    private val QR_REFRESH_INTERVAL = 30_000L  // 30 seconds (match desktop)
    private val CODE_REFRESH_INTERVAL = 20_000L // 20 seconds (match desktop)

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.QR_CODE

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_admin)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupClickListeners()

        // Load QR and manual code directly (match desktop - backend handles schedule check)
        loadQRCode()
        loadAttendanceCode()

        // Auto-refresh QR every 30s and manual code every 20s (match desktop)
        startAutoRefresh()
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

        btnDownload = findViewById(R.id.btnDownload)
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadQRCode()
            loadAttendanceCode()
        }

        btnDownload.setOnClickListener {
            downloadQRCode()
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(QR_REFRESH_INTERVAL)
                loadQRCode()
            }
        }
        // Manual code refreshes every 20s
        codeRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(CODE_REFRESH_INTERVAL)
                loadAttendanceCode()
            }
        }
    }

    private fun loadQRCode() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            showError("Sesi telah berakhir, silakan login kembali")
            return
        }

        // Only show loading on first load, not on auto-refresh
        if (currentBitmap == null) {
            showLoading()
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getCurrentQRCode("Bearer $token")
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        if (!allowedPrayers.contains(data.jenis_sholat)) {
                            runOnUiThread {
                                showError("QR Code hanya tersedia untuk sholat Dhuha, Dhuhur, dan Jumat.")
                            }
                            return@launch
                        }
                        runOnUiThread {
                            displayQRCode(data.qr_code, data.jenis_sholat, data.expires_at)
                            showQRCode()
                        }
                    }
                } else {
                    if (response.code() == 404) {
                        runOnUiThread {
                            showNoSchedule("Tidak ada jadwal sholat aktif saat ini")
                        }
                    }
                }
            } catch (e: Exception) {
                // Silent fail on auto-refresh to avoid spamming user
                if (currentBitmap == null) {
                    runOnUiThread {
                        showError("Gagal memuat QR code: ${e.message}")
                    }
                }
            }
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
                } else {
                    runOnUiThread {
                        cardManualCode.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                // Silent fail on auto-refresh
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

    private fun displayQRCode(base64WithPrefix: String, jenisSholat: String, expiresAt: String) {
        tvJenisSholat.text = "Sholat $jenisSholat"
        tvStatus.text = "Auto-refresh setiap 30 detik"
        tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))

        val bitmap = decodeBase64ToBitmap(base64WithPrefix)
        if (bitmap != null) {
            currentBitmap = bitmap
            ivQRCode.setImageBitmap(bitmap)
            ivQRCode.alpha = 1.0f
            btnDownload.isEnabled = true
        } else {
            showError("Gagal memuat gambar QR code")
            return
        }

        startCountdown(expiresAt)
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
        ivQRCode.alpha = 0.5f
        // Auto-refresh will pick up a new one
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        containerQR.visibility = View.GONE
    }

    private fun showQRCode() {
        progressBar.visibility = View.GONE
        containerQR.visibility = View.VISIBLE
    }

    private fun showNoSchedule(message: String) {
        progressBar.visibility = View.GONE
        containerQR.visibility = View.VISIBLE
        tvJenisSholat.text = message
        tvStatus.text = "QR Code akan otomatis muncul saat waktu sholat tiba"
        tvStatus.setTextColor(getColor(android.R.color.darker_gray))
        tvCountdown.text = ""
        ivQRCode.setImageResource(R.drawable.ic_qr_code)
        ivQRCode.alpha = 0.3f
        currentBitmap = null
        btnDownload.isEnabled = false
        cardManualCode.visibility = View.GONE
    }

    private fun showError(message: String) {
        runOnUiThread {
            progressBar.visibility = View.GONE
            containerQR.visibility = View.VISIBLE
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun downloadQRCode() {
        val bitmap = currentBitmap ?: return

        lifecycleScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "QR_Presensi_$timestamp.png"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SAS Mobile")
                    }
                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                        outputStream?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val sasDir = java.io.File(dir, "SAS Mobile")
                    sasDir.mkdirs()
                    val file = java.io.File(sasDir, filename)
                    file.outputStream().use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }

                runOnUiThread {
                    Toast.makeText(this@QRCodeAdminActivity, "QR Code tersimpan di Pictures/SAS Mobile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@QRCodeAdminActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        codeTimer?.cancel()
        autoRefreshJob?.cancel()
        codeRefreshJob?.cancel()
    }
}
