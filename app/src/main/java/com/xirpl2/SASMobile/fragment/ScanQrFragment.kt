package com.xirpl2.SASMobile.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.StudentMainActivity
import com.xirpl2.SASMobile.model.QRCodeVerifyData
import com.xirpl2.SASMobile.repository.QRCodeRepository
import com.xirpl2.SASMobile.repository.PerizinanHalanganRepository
import kotlinx.coroutines.launch

class ScanQrFragment : Fragment(R.layout.fragment_scan_qr) {

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
    private lateinit var toggleScanType: MaterialButtonToggleGroup
    private lateinit var btnScanHalangan: MaterialButton

    private val repository = QRCodeRepository()
    private val halanganRepository = PerizinanHalanganRepository()
    private var isProcessing = false
    private var isHalanganMode = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) barcodeView.resume() else showPermissionDeniedDialog() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyEdgeToEdge(view)
        initializeViews(view)
        setupToggle()
        setupClickListeners()
        setupBarcodeScanner()
        checkCameraPermission()
    }

    private fun initializeViews(view: View) {
        barcodeView = view.findViewById(R.id.barcodeView)
        btnScan = view.findViewById(R.id.btnScan)
        tvStatus = view.findViewById(R.id.tvStatus)
        cardResult = view.findViewById(R.id.cardResult)
        tvStudentName = view.findViewById(R.id.tvStudentName)
        tvStudentClass = view.findViewById(R.id.tvStudentClass)
        tvPrayerType = view.findViewById(R.id.tvPrayerType)
        tvAttendanceStatus = view.findViewById(R.id.tvAttendanceStatus)
        tvAttendanceTime = view.findViewById(R.id.tvAttendanceTime)
        btnScanAgain = view.findViewById(R.id.btnScanAgain)
        progressBar = view.findViewById(R.id.progressBarScan)
        toggleScanType = view.findViewById(R.id.toggleScanType)
        btnScanHalangan = view.findViewById(R.id.btnScanHalangan)
    }

    private fun setupToggle() {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        val jk = sharedPref.getString("jenis_kelamin", "L")
        if (jk != "P") {
            toggleScanType.visibility = View.GONE
        }
        toggleScanType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isHalanganMode = checkedId == R.id.btnScanHalangan
        }
    }

    private fun setupClickListeners() {
        btnScan.setOnClickListener { if (!isProcessing) startScanning() }
        btnScanAgain.setOnClickListener { hideResult(); startScanning() }
    }

    private fun setupBarcodeScanner() {
        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(
            com.google.zxing.BarcodeFormat.QR_CODE
        ))
    }

    private fun startScanning() {
        if (isProcessing) return
        if (!hasCameraPermission()) { requestCameraPermission(); return }
        tvStatus.visibility = View.GONE; hideResult()
        barcodeView.resume(); barcodeView.setTorch(false)
        barcodeView.decodeSingle(object : com.journeyapps.barcodescanner.BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                barcodeView.pause()
                activity?.runOnUiThread {
                    if (result != null && result.text.isNotBlank()) {
                        if (isHalanganMode) verifyHalanganQR(result.text)
                        else verifyAbsensiQR(result.text)
                    } else showStatus("Tidak ada QR yang terdeteksi", false)
                }
            }
            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    private fun verifyAbsensiQR(qrToken: String) {
        val token = getToken()
        if (token.isEmpty()) { showStatus("Sesi berakhir, login kembali", false); return }
        isProcessing = true; showLoading()
        lifecycleScope.launch {
            repository.verifyQRCode(token, qrToken).fold(
                onSuccess = { data -> hideLoading(); isProcessing = false; showVerificationResult(data) },
                onFailure = { error -> hideLoading(); isProcessing = false; showStatus(error.message ?: "Gagal", false) }
            )
        }
    }

    private fun verifyHalanganQR(qrToken: String) {
        val token = getToken()
        if (token.isEmpty()) { showStatus("Sesi berakhir, login kembali", false); return }
        isProcessing = true; showLoading()
        lifecycleScope.launch {
            halanganRepository.verifyHalangan(token, qrToken).fold(
                onSuccess = { data ->
                    hideLoading(); isProcessing = false
                    tvStudentName.text = data.namaSiswa
                    tvStudentClass.text = if (data.jurusan.isNotEmpty()) "${data.kelas} - ${data.jurusan}" else data.kelas
                    tvPrayerType.text = "Halangan"
                    tvAttendanceStatus.text = data.status.replaceFirstChar { it.uppercase() }
                    tvAttendanceTime.text = data.tanggal
                    cardResult.visibility = View.VISIBLE
                    showStatus("Pengajuan halangan tercatat!", true)
                    Toast.makeText(requireContext(), "Halangan berhasil diajukan", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    hideLoading(); isProcessing = false
                    showStatus(error.message ?: "Gagal verifikasi halangan", false)
                }
            )
        }
    }

    private fun showVerificationResult(data: QRCodeVerifyData) {
        tvStudentName.text = data.nama_siswa
        tvStudentClass.text = if (data.jurusan != null) "${data.kelas} - ${data.jurusan}" else data.kelas
        tvPrayerType.text = "Salat ${data.jenis_sholat}"
        tvAttendanceStatus.text = data.status.replaceFirstChar { it.uppercase() }
        tvAttendanceTime.text = data.tanggal?.let { formatDate(it) } ?: "--"
        val statusColor = when (data.status.trim().uppercase()) { "HADIR" -> requireContext().getColor(R.color.status_success); "ALPHA" -> requireContext().getColor(R.color.status_error); else -> requireContext().getColor(R.color.status_warning) }
        tvAttendanceStatus.setTextColor(statusColor)
        cardResult.visibility = View.VISIBLE
        if (data.valid) { showStatus("Kehadiran berhasil dicatat!", true); Toast.makeText(requireContext(), "Kehadiran berhasil dicatat!", Toast.LENGTH_SHORT).show() }
        else { showStatus("Anda sudah tercatat hadir", false); Toast.makeText(requireContext(), "Sudah tercatat sebelumnya", Toast.LENGTH_SHORT).show() }
    }

    private fun formatDate(tanggal: String): String {
        return try {
            val parts = tanggal.split("-")
            if (parts.size == 3) {
                val monthNames = arrayOf("Januari","Februari","Maret","April","Mei","Juni","Juli","Agustus","September","Oktober","November","Desember")
                "${parts[2].toInt()} ${monthNames.getOrElse(parts[1].toInt() - 1) { parts[1] }} ${parts[0]}"
            } else tanggal
        } catch (e: Exception) { tanggal }
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        tvStatus.text = message; tvStatus.visibility = View.VISIBLE
        tvStatus.setTextColor(if (isSuccess) requireContext().getColor(R.color.status_success) else requireContext().getColor(R.color.status_error))
    }

    private fun hideResult() { cardResult.visibility = View.GONE }
    private fun showLoading() { progressBar.visibility = View.VISIBLE; btnScan.isEnabled = false; btnScan.alpha = 0.5f }
    private fun hideLoading() { progressBar.visibility = View.GONE; btnScan.isEnabled = true; btnScan.alpha = 1.0f }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun checkCameraPermission() { if (hasCameraPermission()) barcodeView.resume() else requestCameraPermission() }
    private fun requestCameraPermission() { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Izin Kamera Diperlukan").setMessage("Aktifkan izin kamera di Pengaturan untuk scan QR.")
            .setPositiveButton("Buka Pengaturan") { _, _ -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", requireContext().packageName, null) }) }
            .setNegativeButton("Kembali", null).show()
    }

    override fun onResume() { super.onResume(); if (hasCameraPermission() && ::cardResult.isInitialized && cardResult.visibility != View.VISIBLE && !isProcessing) barcodeView.resume() }
    override fun onPause() { super.onPause(); if (::barcodeView.isInitialized) barcodeView.pause() }
    override fun onDestroyView() { super.onDestroyView(); if (::barcodeView.isInitialized) barcodeView.pause() }

    private fun getToken(): String {
        val act = requireActivity()
        return if (act is StudentMainActivity) act.getAuthTokenSiswa() else ""
    }

    private fun applyEdgeToEdge(view: View) {
        val topBar = view.findViewById<View>(R.id.topBarContent) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }
}
