package com.xirpl2.SASMobile

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.xirpl2.SASMobile.model.RequestHalanganData
import com.xirpl2.SASMobile.repository.PerizinanHalanganRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PengajuanHalanganActivity : BaseSiswaActivity() {

    private lateinit var etTanggalMulai: TextInputEditText
    private lateinit var btnAjukan: MaterialButton
    private lateinit var cardHasil: MaterialCardView
    private lateinit var tvStatusLabel: TextView
    private lateinit var qrContainer: View
    private lateinit var ivQRCode: ImageView
    private lateinit var tvQrToken: TextView
    private lateinit var tvInfo: TextView
    private lateinit var infoIstihadhah: View

    private val repository = PerizinanHalanganRepository()
    private var selectedDate: String? = null

    override fun getCurrentMenuItem(): SiswaMenuItem = SiswaMenuItem.PENGAJUAN_IZIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_halangan)
        window.statusBarColor = 0xFF2886D6.toInt()

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupDatePicker()
        setupSubmitButton()
    }

    private fun initializeViews() {
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        btnAjukan = findViewById(R.id.btnAjukan)
        cardHasil = findViewById(R.id.cardHasil)
        tvStatusLabel = findViewById(R.id.tvStatusLabel)
        qrContainer = findViewById(R.id.qrContainer)
        ivQRCode = findViewById(R.id.ivQRCode)
        tvQrToken = findViewById(R.id.tvQrToken)
        tvInfo = findViewById(R.id.tvInfo)
        infoIstihadhah = findViewById(R.id.infoIstihadhah)
    }

    private fun setupDatePicker() {
        etTanggalMulai.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(this, { _, y, m, d ->
                selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d)
                etTanggalMulai.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
                btnAjukan.isEnabled = true
            }, year, month, day)

            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }
    }

    private fun setupSubmitButton() {
        btnAjukan.setOnClickListener {
            val date = selectedDate
            if (date == null) {
                Toast.makeText(this, "Pilih tanggal mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = getAuthToken()
            if (token.isEmpty()) {
                Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnAjukan.isEnabled = false
            btnAjukan.text = "Memproses..."

            lifecycleScope.launch {
                repository.requestHalangan(token, date).fold(
                    onSuccess = { data ->
                        showResult(data)
                    },
                    onFailure = { error ->
                        btnAjukan.isEnabled = true
                        btnAjukan.text = "Ajukan Izin Halangan"
                        Toast.makeText(this@PengajuanHalanganActivity, error.message ?: "Gagal mengajukan", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun showResult(data: RequestHalanganData) {
        cardHasil.visibility = View.VISIBLE
        btnAjukan.visibility = View.GONE

        if (data.isIstihadhah) {
            tvStatusLabel.text = "Status: Istihadhah Check"
            tvStatusLabel.setTextColor(getColor(android.R.color.holo_orange_dark))
        } else {
            tvStatusLabel.text = "Status: ${data.statusValidasi.replaceFirstChar { it.uppercase() }}"
            tvStatusLabel.setTextColor(getColor(R.color.blue_theme))
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(data.qrToken, BarcodeFormat.QR_CODE, 400, 400)
                val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
                for (x in 0 until 400) {
                    for (y in 0 until 400) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                    }
                }
                withContext(Dispatchers.Main) {
                    qrContainer.visibility = View.VISIBLE
                    ivQRCode.setImageBitmap(bitmap)
                    tvQrToken.visibility = View.VISIBLE
                    tvQrToken.text = data.qrToken.take(16) + "..."

                    tvInfo.text = "Tunjukkan QR ini kepada guru agama untuk validasi.\nBerlaku: ${data.tanggalMulai} s/d ${data.tanggalSelesai}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvInfo.text = "QR Token: ${data.qrToken.take(32)}..."
                    tvInfo.visibility = View.VISIBLE
                }
            }
        }
    }
}
