package com.xirpl2.SASMobile

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xirpl2.SASMobile.model.RequestHalanganData
import com.xirpl2.SASMobile.repository.PerizinanHalanganRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class PengajuanHalanganActivity : BaseSiswaActivity() {

    private lateinit var cardForm: MaterialCardView
    private lateinit var etTanggalMulai: TextInputEditText
    private lateinit var btnAjukan: MaterialButton
    private lateinit var cardStatus: MaterialCardView
    private lateinit var tvStatusHalangan: TextView
    private lateinit var tvPeriodeHalangan: TextView
    private lateinit var btnScanVerify: MaterialButton
    private lateinit var barcodeView: BarcodeView
    private lateinit var tvScanResult: TextView

    private val repository = PerizinanHalanganRepository()
    private var selectedDate: String? = null
    private var submittedId: Int? = null
    private var isProcessing = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) barcodeView.resume()
    }

    override fun getCurrentMenuItem(): SiswaMenuItem = SiswaMenuItem.PENGAJUAN_HALANGAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_halangan)
        window.statusBarColor = 0xFF2886D6.toInt()

        initViews()
        setupDrawerAndSidebar()
        setupWindowInsets()
        setupDatePicker()
        setupSubmitButton()
        setupBarcodeScanner()
        setupBackButton()
    }

    private fun initViews() {
        cardForm = findViewById(R.id.cardForm)
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        btnAjukan = findViewById(R.id.btnAjukan)
        cardStatus = findViewById(R.id.cardStatus)
        tvStatusHalangan = findViewById(R.id.tvStatusHalangan)
        tvPeriodeHalangan = findViewById(R.id.tvPeriodeHalangan)
        btnScanVerify = findViewById(R.id.btnScanVerify)
        barcodeView = findViewById(R.id.barcodeView)
        tvScanResult = findViewById(R.id.tvScanResult)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDatePicker() {
        etTanggalMulai.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                etTanggalMulai.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
                btnAjukan.isEnabled = true
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }
    }

    private fun setupSubmitButton() {
        btnAjukan.setOnClickListener {
            val date = selectedDate ?: run {
                Toast.makeText(this, "Pilih tanggal mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val token = getAuthToken()
            if (token.isEmpty()) {
                Toast.makeText(this, "Sesi berakhir", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnAjukan.isEnabled = false
            btnAjukan.text = "Memproses..."
            lifecycleScope.launch {
                repository.requestHalangan(token, date).fold(
                    onSuccess = { data -> showSubmitted(data) },
                    onFailure = { e ->
                        btnAjukan.isEnabled = true
                        btnAjukan.text = "Ajukan Izin Halangan"
                        Toast.makeText(this@PengajuanHalanganActivity, e.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun showSubmitted(data: RequestHalanganData) {
        cardForm.visibility = View.GONE
        cardStatus.visibility = View.VISIBLE
        submittedId = data.id

        when {
            data.isIstihadhah -> {
                tvStatusHalangan.text = "Status: Perlu Cek Medis"
                tvStatusHalangan.setTextColor(getColor(R.color.status_warning))
            }
            else -> {
                tvStatusHalangan.text = "Status: Menunggu Verifikasi"
                tvStatusHalangan.setTextColor(getColor(R.color.blue_theme))
            }
        }
        tvPeriodeHalangan.text = "Berlaku: ${data.tanggalMulai} s/d ${data.tanggalSelesai}"

        if (data.statusValidasi != "approved") {
            btnScanVerify.visibility = View.VISIBLE
            btnScanVerify.setOnClickListener { startQRScan() }
        }
    }

    private fun setupBarcodeScanner() {
        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(
            com.google.zxing.BarcodeFormat.QR_CODE
        ))
    }

    private fun startQRScan() {
        if (isProcessing) return
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }
        btnScanVerify.visibility = View.GONE
        barcodeView.visibility = View.VISIBLE
        tvScanResult.visibility = View.GONE
        barcodeView.resume()

        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                barcodeView.pause()
                runOnUiThread {
                    if (result != null && result.text.isNotBlank()) {
                        val scanned = result.text.trim()
                        val id = scanned.toIntOrNull()
                        if (id != null) {
                            verifyScanned(id)
                        } else {
                            showScanError("QR tidak valid")
                        }
                    } else {
                        showScanError("Tidak terdeteksi")
                    }
                }
            }
            override fun possibleResultPoints(points: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    private fun verifyScanned(halanganId: Int) {
        val token = getAuthToken()
        if (token.isEmpty()) return
        isProcessing = true
        tvScanResult.visibility = View.VISIBLE
        tvScanResult.text = "Memverifikasi..."
        lifecycleScope.launch {
            repository.verifyHalangan(token, halanganId).fold(
                onSuccess = {
                    isProcessing = false
                    tvScanResult.text = "Halangan berhasil diverifikasi!"
                    tvScanResult.setTextColor(getColor(R.color.status_success))
                    barcodeView.visibility = View.GONE
                    tvStatusHalangan.text = "Status: Disetujui"
                    tvStatusHalangan.setTextColor(getColor(R.color.status_success))
                    btnScanVerify.visibility = View.GONE
                    Toast.makeText(this@PengajuanHalanganActivity, "Berhasil diverifikasi", Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    isProcessing = false
                    showScanError(e.message ?: "Verifikasi gagal")
                }
            )
        }
    }

    private fun showScanError(msg: String) {
        tvScanResult.visibility = View.VISIBLE
        tvScanResult.text = msg
        tvScanResult.setTextColor(getColor(R.color.status_error))
        barcodeView.visibility = View.GONE
        btnScanVerify.visibility = View.VISIBLE
    }

    private fun setupBackButton() {
        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized && barcodeView.visibility == View.VISIBLE && hasCameraPermission() && !isProcessing) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) barcodeView.pause()
    }

    override fun onDestroy() {
        if (::barcodeView.isInitialized) barcodeView.pause()
        super.onDestroy()
    }
}
