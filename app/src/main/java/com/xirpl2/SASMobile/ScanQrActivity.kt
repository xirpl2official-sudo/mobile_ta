package com.xirpl2.SASMobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            barcodeView.resume()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)
        window.statusBarColor = 0xFF2886D6.toInt()

        initializeViews()
        setupWindowInsets()
        setupClickListeners()
        setupBarcodeScanner()
        checkCameraPermission()
    }

    private fun initializeViews() {
        barcodeView = findViewById(R.id.barcodeView)
        btnScan = findViewById(R.id.btnScan)
        tvStatus = findViewById(R.id.tvStatus)

        cardResult = findViewById(R.id.cardResult)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentClass = findViewById(R.id.tvStudentClass)
        tvPrayerType = findViewById(R.id.tvPrayerType)
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus)
        tvAttendanceTime = findViewById(R.id.tvAttendanceTime)
        btnScanAgain = findViewById(R.id.btnScanAgain)
        progressBar = findViewById(R.id.progressBarScan)
    }

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
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

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

        tvStudentName.text = data.nama_siswa

        val classInfo = if (data.jurusan != null) {
            "${data.kelas} - ${data.jurusan}"
        } else {
            data.kelas
        }
        tvStudentClass.text = classInfo


        tvPrayerType.text = "Sholat ${data.jenis_sholat}"
        tvAttendanceStatus.text = data.status.replaceFirstChar { it.uppercase() }
        tvAttendanceTime.text = formatDate(data.tanggal)


        val statusColor = when (data.status.trim().uppercase()) {
            "HADIR" -> getColor(android.R.color.holo_green_dark)
            "ALPHA" -> getColor(android.R.color.holo_red_dark)
            else -> getColor(android.R.color.holo_orange_dark)
        }
        tvAttendanceStatus.setTextColor(statusColor)
        
        
        cardResult.visibility = View.VISIBLE
        
        
        showStatus("✓ Kehadiran Anda berhasil dicatat!", true)
        
        
        Toast.makeText(this, "Kehadiran berhasil dicatat!", Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(tanggal: String?): String {
        if (tanggal.isNullOrBlank()) return "--"
        return try {
            val parts = tanggal.split("-")
            if (parts.size == 3) {
                val day = parts[2].toInt()
                val month = parts[1].toInt()
                val year = parts[0]
                val monthNames = arrayOf(
                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                )
                val monthName = if (month in 1..12) monthNames[month - 1] else parts[1]
                "$day $monthName $year"
            } else {
                tanggal
            }
        } catch (e: Exception) {
            tanggal
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

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() {
        if (hasCameraPermission()) {
            barcodeView.resume()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Kamera Diperlukan")
            .setMessage("Aplikasi memerlukan akses kamera untuk memindai QR code presensi sholat. Silakan aktifkan izin kamera di Pengaturan.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Kembali") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun getAuthToken(): String {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && cardResult.visibility != View.VISIBLE && !isProcessing) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        barcodeView.pause()
        super.onDestroy()
    }
}