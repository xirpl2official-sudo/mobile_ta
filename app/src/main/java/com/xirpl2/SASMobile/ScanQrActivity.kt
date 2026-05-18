package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xirpl2.SASMobile.model.QRCodeVerifyData
import com.xirpl2.SASMobile.repository.QRCodeRepository
import kotlinx.coroutines.launch

class ScanQrActivity : BaseActivity() {

    private lateinit var barcodeView: BarcodeView
    private lateinit var btnScan: Button
    private lateinit var tvStatus: TextView
    private lateinit var cardResult: CardView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentClass: TextView
    private lateinit var tvPrayerType: TextView
    private lateinit var tvAttendanceStatus: TextView
    private lateinit var tvAttendanceTime: TextView
    private lateinit var btnScanAgain: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val repository = QRCodeRepository()
    private val TAG = "ScanQrActivity"
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan_qr)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        initializeViews()
        setupWindowInsets()
        setupClickListeners()
        setupBarcodeScanner()
    }

    private fun initializeViews() {
        barcodeView = findViewById(R.id.barcodeView)
        btnScan = findViewById(R.id.btnScan)
        tvStatus = findViewById(R.id.tvStatus)
        
        
        cardResult = findViewById(R.id.cardResult) ?: createDummyCardView()
        tvStudentName = findViewById(R.id.tvStudentName) ?: createDummyTextView()
        tvStudentClass = findViewById(R.id.tvStudentClass) ?: createDummyTextView()
        tvPrayerType = findViewById(R.id.tvPrayerType) ?: createDummyTextView()
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus) ?: createDummyTextView()
        tvAttendanceTime = findViewById(R.id.tvAttendanceTime) ?: createDummyTextView()
        btnScanAgain = findViewById(R.id.btnScanAgain) ?: createDummyButton()
        progressBar = findViewById(R.id.progressBarScan) ?: createDummyProgressBar()
    }

    private fun createDummyCardView(): CardView = CardView(this).apply { visibility = View.GONE }
    private fun createDummyTextView(): TextView = TextView(this)
    private fun createDummyButton(): MaterialButton = MaterialButton(this)
    private fun createDummyProgressBar(): ProgressBar = ProgressBar(this).apply { visibility = View.GONE }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupClickListeners() {
        
        findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        
        btnScan.setOnClickListener {
            if (!isProcessing) {
                startScanning()
            }
        }

        
        btnScanAgain.setOnClickListener {
            hideResult()
            startScanning()
        }
    }

    private fun setupBarcodeScanner() {
        
        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(com.google.zxing.BarcodeFormat.QR_CODE))
    }

    private fun startScanning() {
        if (isProcessing) return
        
        tvStatus.visibility = View.GONE
        hideResult()
        barcodeView.resume()
        barcodeView.setTorch(false)

        barcodeView.decodeSingle(object : com.journeyapps.barcodescanner.BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                barcodeView.pause()

                runOnUiThread {
                    if (result != null && result.text.isNotBlank()) {
                        val qrToken = result.text
                        
                        
                        verifyQRCode(qrToken)
                    } else {
                        showStatus("Tidak ada QR yang terdeteksi", false)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                
            }
        })
    }

    private fun verifyQRCode(qrToken: String) {
        val authToken = getAuthToken()
        
        if (authToken.isEmpty()) {
            showStatus("Sesi telah berakhir, silakan login kembali", false)
            return
        }

        isProcessing = true
        showLoading()

        lifecycleScope.launch {
            repository.verifyQRCode(authToken, qrToken).fold(
                onSuccess = { verifyData ->
                    runOnUiThread {
                        hideLoading()
                        isProcessing = false
                        showVerificationResult(verifyData)
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        hideLoading()
                        isProcessing = false
                        showStatus(error.message ?: "Verifikasi gagal", false)
                    }
                }
            )
        }
    }

    private fun showVerificationResult(data: QRCodeVerifyData) {
        
        tvStudentName.text = data.siswa.nama
        
        val classInfo = if (data.siswa.jurusan != null) {
            "${data.siswa.kelas} - ${data.siswa.jurusan}"
        } else {
            data.siswa.kelas
        }
        tvStudentClass.text = classInfo
        
        
        tvPrayerType.text = "Sholat ${data.absensi.jenis_sholat}"
        tvAttendanceStatus.text = data.absensi.status
        tvAttendanceTime.text = formatTime(data.absensi.waktu_absen)
        
        
        val statusColor = when (data.absensi.status.uppercase()) {
            "HADIR" -> getColor(android.R.color.holo_green_dark)
            "ALPHA" -> getColor(android.R.color.holo_red_dark)
            else -> getColor(android.R.color.holo_orange_dark)
        }
        tvAttendanceStatus.setTextColor(statusColor)
        
        
        cardResult.visibility = View.VISIBLE
        
        
        showStatus("✓ Kehadiran Anda berhasil dicatat!", true)
        
        
        Toast.makeText(this, "Kehadiran berhasil dicatat!", Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(waktuAbsen: String?): String {
        if (waktuAbsen.isNullOrBlank()) return "--:--"
        return try {
            if (waktuAbsen.contains("T")) {
                val timePart = waktuAbsen.substringAfter("T").substringBefore("Z")
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    "${parts[0]}:${parts[1]}"
                } else {
                    waktuAbsen.take(5)
                }
            } else if (waktuAbsen.contains(" ")) {
                waktuAbsen.substringAfter(" ").take(5)
            } else {
                waktuAbsen.take(5)
            }
        } catch (e: Exception) {
            waktuAbsen.take(5)
        }
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        runOnUiThread {
            tvStatus.text = message
            tvStatus.visibility = View.VISIBLE
            tvStatus.setTextColor(
                if (isSuccess) getColor(android.R.color.holo_green_dark)
                else getColor(android.R.color.holo_red_dark)
            )
        }
    }

    private fun hideResult() {
        runOnUiThread {
            cardResult.visibility = View.GONE
        }
    }

    private fun showLoading() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            btnScan.isEnabled = false
            btnScan.alpha = 0.5f
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            progressBar.visibility = View.GONE
            btnScan.isEnabled = true
            btnScan.alpha = 1.0f
        }
    }

    private fun getAuthToken(): String {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}