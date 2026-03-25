package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.util.Log
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

/**
 * Activity untuk Siswa memindai QR Code yang ditampilkan oleh Staff
 * 
 * Workflow:
 * 1. Staff menampilkan QR code di layar/proyektor
 * 2. Siswa membuka activity ini dan memindai QR code
 * 3. Token dari QR code dikirim ke API untuk verifikasi dan pencatatan kehadiran
 * 4. Siswa hanya dapat memindai sekali per sesi sholat
 */
class ScanQrActivity : AppCompatActivity() {

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
        
        // Result card views (may not exist in old layout)
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
        // Back button
        findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Scan button
        btnScan.setOnClickListener {
            if (!isProcessing) {
                startScanning()
            }
        }

        // Scan again button (in result card)
        btnScanAgain.setOnClickListener {
            hideResult()
            startScanning()
        }
    }

    private fun setupBarcodeScanner() {
        // Only allow QR code format
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
                        Log.d(TAG, "QR Token scanned: ${qrToken.take(50)}...")
                        
                        // Verify the scanned QR code
                        verifyQRCode(qrToken)
                    } else {
                        showStatus("Tidak ada QR yang terdeteksi", false)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // Can be ignored
            }
        })
    }

    /**
     * Verify scanned QR code with the backend API
     */
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
                    Log.d(TAG, "Verification success: ${verifyData.siswa.nama}")
                    runOnUiThread {
                        hideLoading()
                        isProcessing = false
                        showVerificationResult(verifyData)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Verification failed: ${error.message}")
                    runOnUiThread {
                        hideLoading()
                        isProcessing = false
                        showStatus(error.message ?: "Verifikasi gagal", false)
                    }
                }
            )
        }
    }

    /**
     * Display verification result in the result card
     * Shows student's own attendance confirmation
     */
    private fun showVerificationResult(data: QRCodeVerifyData) {
        // Update student info (this is the student's own info)
        tvStudentName.text = data.siswa.nama
        
        val classInfo = if (data.siswa.jurusan != null) {
            "${data.siswa.kelas} - ${data.siswa.jurusan}"
        } else {
            data.siswa.kelas
        }
        tvStudentClass.text = classInfo
        
        // Update attendance info
        tvPrayerType.text = "Sholat ${data.absensi.jenis_sholat}"
        tvAttendanceStatus.text = data.absensi.status
        tvAttendanceTime.text = formatTime(data.absensi.waktu_absen)
        
        // Set status color based on attendance status
        val statusColor = when (data.absensi.status.uppercase()) {
            "HADIR" -> getColor(android.R.color.holo_green_dark)
            "ALPHA" -> getColor(android.R.color.holo_red_dark)
            else -> getColor(android.R.color.holo_orange_dark)
        }
        tvAttendanceStatus.setTextColor(statusColor)
        
        // Show the result card
        cardResult.visibility = View.VISIBLE
        
        // Show success status for student
        showStatus("✓ Kehadiran Anda berhasil dicatat!", true)
        
        // Play success feedback for student
        Toast.makeText(this, "Kehadiran berhasil dicatat!", Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(waktuAbsen: String): String {
        return try {
            // Extract time part from datetime string
            if (waktuAbsen.contains("T")) {
                val timePart = waktuAbsen.substringAfter("T").substringBefore("Z")
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    "${parts[0]}:${parts[1]}"
                } else {
                    waktuAbsen
                }
            } else if (waktuAbsen.contains(" ")) {
                waktuAbsen.substringAfter(" ").take(5)
            } else {
                waktuAbsen.take(5)
            }
        } catch (e: Exception) {
            waktuAbsen
        }
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        tvStatus.text = message
        tvStatus.visibility = View.VISIBLE
        tvStatus.setTextColor(
            if (isSuccess) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
    }

    private fun hideResult() {
        cardResult.visibility = View.GONE
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnScan.isEnabled = false
        btnScan.alpha = 0.5f
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnScan.isEnabled = true
        btnScan.alpha = 1.0f
    }

    private fun getAuthToken(): String {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        // Don't auto-resume, control manually via decodeSingle
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}