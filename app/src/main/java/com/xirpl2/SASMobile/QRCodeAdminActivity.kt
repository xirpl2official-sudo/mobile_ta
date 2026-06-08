package com.xirpl2.SASMobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.CountDownTimer
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.repository.QRCodeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Download button removed

    private var countDownTimer: CountDownTimer? = null
    private var codeTimer: CountDownTimer? = null
    private var autoRefreshJob: Job? = null
    private var codeRefreshJob: Job? = null
    private var currentBitmap: Bitmap? = null

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val qrRepository = QRCodeRepository()

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

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadQRCode()
            loadAttendanceCode()
        }

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
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadQRCode()
            loadAttendanceCode()
        }

        // Download button removed - no longer needed
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

        if (currentBitmap == null) {
            showLoading()
        }

        lifecycleScope.launch {
            qrRepository.generateQRCode(token).fold(
                onSuccess = { data ->
                    displayQRCode(data.qr_code, data.jenis_sholat, data.expires_at)
                    showQRCode()
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                },
                onFailure = { e ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    val msg = e.message ?: "Gagal memuat QR code"
                    if (msg.contains("tidak ada jadwal", ignoreCase = true) || msg.contains("404")) {
                        showNoSchedule("Tidak ada jadwal sholat aktif saat ini")
                    } else if (msg.contains("sesi", ignoreCase = true) || msg.contains("401")) {
                        showError("Sesi telah berakhir, silakan login kembali")
                    } else if (currentBitmap == null) {
                        showError(msg)
                    }
                }
            )
        }
    }

    private fun loadAttendanceCode() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            qrRepository.generateAttendanceCode(token).fold(
                onSuccess = { data ->
                    displayAttendanceCode(data.code, data.expiresIn)
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                },
                onFailure = {
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    cardManualCode.visibility = View.GONE
                }
            )
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
        tvJenisSholat.text = "Salat $jenisSholat"
        tvStatus.text = "Auto-refresh setiap 30 detik"
        tvStatus.setTextColor(getColor(R.color.status_success))

        val bitmap = decodeBase64ToBitmap(base64WithPrefix)
        if (bitmap != null) {
            currentBitmap = bitmap
            ivQRCode.setImageBitmap(bitmap)
            ivQRCode.alpha = 1.0f
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
                    tvCountdown.setTextColor(getColor(if (millisUntilFinished < 60000) R.color.status_error else R.color.status_success))
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
        tvCountdown.setTextColor(getColor(R.color.status_error))
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
        tvStatus.text = "QR Code akan otomatis muncul saat waktu salat tiba"
        tvStatus.setTextColor(getColor(R.color.gray_light))
        tvCountdown.text = ""
        ivQRCode.setImageResource(R.drawable.ic_qr_code)
        ivQRCode.alpha = 0.3f
        currentBitmap = null
        cardManualCode.visibility = View.GONE
    }

    private fun showError(message: String) {
        runOnUiThread {
            progressBar.visibility = View.GONE
            containerQR.visibility = View.VISIBLE
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    // downloadQRCode() removed - no longer needed

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        codeTimer?.cancel()
        autoRefreshJob?.cancel()
        codeRefreshJob?.cancel()
    }
}
