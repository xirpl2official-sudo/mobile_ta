package com.xirpl2.SASMobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xirpl2.SASMobile.repository.PerizinanHalanganRepository
import kotlinx.coroutines.launch

class ValidasiHalanganGuruActivity : BaseAdminActivity() {

    private lateinit var barcodeView: BarcodeView
    private lateinit var btnScan: MaterialButton
    private lateinit var cardScanResult: MaterialCardView
    private lateinit var tvSiswaInfo: TextView
    private lateinit var tvTanggalInfo: TextView
    private lateinit var etCatatan: TextInputEditText
    private lateinit var btnSetujui: MaterialButton
    private lateinit var btnTolak: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    private val repository = PerizinanHalanganRepository()
    private var scannedToken: String? = null
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

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.BERANDA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validasi_halangan_guru)
        window.statusBarColor = 0xFF2886D6.toInt()

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupBarcodeScanner()
        setupClickListeners()
        checkCameraPermission()
    }

    private fun initializeViews() {
        barcodeView = findViewById(R.id.barcodeView)
        btnScan = findViewById(R.id.btnScan)
        cardScanResult = findViewById(R.id.cardScanResult)
        tvSiswaInfo = findViewById(R.id.tvSiswaInfo)
        tvTanggalInfo = findViewById(R.id.tvTanggalInfo)
        etCatatan = findViewById(R.id.etCatatan)
        btnSetujui = findViewById(R.id.btnSetujui)
        btnTolak = findViewById(R.id.btnTolak)
        progressBar = findViewById(R.id.progressBarScan)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun setupBarcodeScanner() {
        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(
            com.google.zxing.BarcodeFormat.QR_CODE,
            com.google.zxing.BarcodeFormat.CODE_128
        ))
    }

    private fun setupClickListeners() {
        btnScan.setOnClickListener {
            if (!isProcessing) {
                startScanning()
            }
        }

        btnSetujui.setOnClickListener {
            validateHalangan("approved")
        }

        btnTolak.setOnClickListener {
            validateHalangan("rejected")
        }
    }

    private fun startScanning() {
        if (isProcessing) return
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        tvStatus.visibility = View.GONE
        cardScanResult.visibility = View.GONE
        scannedToken = null
        barcodeView.resume()
        btnScan.text = "Memindai..."

        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                barcodeView.pause()
                btnScan.text = "Mulai Pindai"

                runOnUiThread {
                    if (result != null && result.text.isNotBlank()) {
                        scannedToken = result.text
                        showScannedInfo(result.text)
                    } else {
                        showStatus("Tidak ada QR yang terdeteksi", false)
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    private fun showScannedInfo(token: String) {
        tvSiswaInfo.text = "Token: ${token.take(16)}..."
        tvTanggalInfo.text = "Menunggu validasi..."
        cardScanResult.visibility = View.VISIBLE
        tvStatus.visibility = View.GONE
    }

    private fun validateHalangan(status: String) {
        val token = getAuthToken()
        val qrToken = scannedToken

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir", Toast.LENGTH_SHORT).show()
            return
        }
        if (qrToken == null) {
            Toast.makeText(this, "Scan QR terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        showLoading()
        cardScanResult.visibility = View.GONE

        val catatan = etCatatan.text?.toString()?.takeIf { it.isNotBlank() }

        lifecycleScope.launch {
            repository.validateHalangan(token, qrToken, status, catatan).fold(
                onSuccess = { message ->
                    isProcessing = false
                    hideLoading()
                    Toast.makeText(this@ValidasiHalanganGuruActivity, message, Toast.LENGTH_SHORT).show()
                    resetScan()
                },
                onFailure = { error ->
                    isProcessing = false
                    hideLoading()
                    showStatus(error.message ?: "Validasi gagal", false)
                    cardScanResult.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun resetScan() {
        scannedToken = null
        etCatatan.text?.clear()
        cardScanResult.visibility = View.GONE
        tvStatus.visibility = View.GONE
        barcodeView.resume()
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        tvStatus.text = message
        tvStatus.visibility = View.VISIBLE
        tvStatus.setTextColor(
            if (isSuccess) getColor(R.color.status_success)
            else getColor(R.color.status_error)
        )
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnScan.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnScan.isEnabled = true
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
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
            .setMessage("Aplikasi memerlukan akses kamera untuk memindai QR code perizinan halangan.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Kembali") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && !isProcessing) {
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
