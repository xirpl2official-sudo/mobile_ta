package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.model.CreateSiswaRequest
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.util.*

class TambahSiswaActivity : BaseActivity() {

    private val repository = BerandaRepository()
    
    private lateinit var etNamaSiswa: TextInputEditText
    private lateinit var etNisSiswa: TextInputEditText
    private lateinit var actvTahunMasuk: AutoCompleteTextView
    private lateinit var etTahunAjaranMulai: TextInputEditText
    private lateinit var etTahunAjaranSelesai: TextInputEditText
    private lateinit var actvKelas: AutoCompleteTextView
    private lateinit var actvJurusan: AutoCompleteTextView
    private lateinit var actvPart: AutoCompleteTextView
    private lateinit var rgJenisKelamin: RadioGroup
    private lateinit var btnKirim: MaterialButton
    private lateinit var btnBatal: MaterialButton

    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val kelasOptions = listOf("X", "XI", "XII")
    private val partOptions = listOf("1", "2", "3", "4")
    private val tahunMasukOptions = (2020..2026).map { it.toString() }.reversed()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_siswa)
        
        setupStatusBar()
        initViews()
        setupDropdowns()
        setupListeners()
        
        // Auto-fill current academic year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        etTahunAjaranMulai.setText(currentYear.toString())
        etTahunAjaranSelesai.setText((currentYear + 1).toString())
    }

    private fun setupStatusBar() {
        window.statusBarColor = getColor(R.color.surface)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun initViews() {
        etNamaSiswa = findViewById(R.id.etNamaSiswa)
        etNisSiswa = findViewById(R.id.etNisSiswa)
        actvTahunMasuk = findViewById(R.id.actvTahunMasuk)
        etTahunAjaranMulai = findViewById(R.id.etTahunAjaranMulai)
        etTahunAjaranSelesai = findViewById(R.id.etTahunAjaranSelesai)
        actvKelas = findViewById(R.id.actvKelas)
        actvJurusan = findViewById(R.id.actvJurusan)
        actvPart = findViewById(R.id.actvPart)
        rgJenisKelamin = findViewById(R.id.rgJenisKelamin)
        btnKirim = findViewById(R.id.btnKirim)
        btnBatal = findViewById(R.id.btnBatal)
    }

    private fun setupDropdowns() {
        val tahunAdapter = ArrayAdapter(this, R.layout.dropdown_item, tahunMasukOptions)
        actvTahunMasuk.setAdapter(tahunAdapter)

        val kelasAdapter = ArrayAdapter(this, R.layout.dropdown_item, kelasOptions)
        actvKelas.setAdapter(kelasAdapter)

        val jurusanAdapter = ArrayAdapter(this, R.layout.dropdown_item, fixedJurusanList)
        actvJurusan.setAdapter(jurusanAdapter)

        val partAdapter = ArrayAdapter(this, R.layout.dropdown_item, partOptions)
        actvPart.setAdapter(partAdapter)
    }

    private fun setupListeners() {
        btnBatal.setOnClickListener {
            finish()
        }

        btnKirim.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun validateAndSubmit() {
        val nama = etNamaSiswa.text?.toString()?.trim() ?: ""
        val nis = etNisSiswa.text?.toString()?.trim() ?: ""
        val tahunMasuk = actvTahunMasuk.text?.toString()?.trim() ?: ""
        val kelas = actvKelas.text?.toString()?.trim() ?: ""
        val jurusan = actvJurusan.text?.toString()?.trim() ?: ""
        val part = actvPart.text?.toString()?.trim() ?: ""
        val jk = if (rgJenisKelamin.checkedRadioButtonId == R.id.rbLakiLaki) "L" else "P"

        if (nama.isEmpty()) {
            etNamaSiswa.error = "Nama tidak boleh kosong"
            return
        }
        if (nis.isEmpty()) {
            etNisSiswa.error = "NIS tidak boleh kosong"
            return
        }
        if (kelas.isEmpty()) {
            Toast.makeText(this, "Silakan pilih kelas", Toast.LENGTH_SHORT).show()
            return
        }
        if (jurusan.isEmpty()) {
            Toast.makeText(this, "Silakan pilih jurusan", Toast.LENGTH_SHORT).show()
            return
        }

        // Combine into expected format if necessary, e.g., "XII RPL 1"
        val fullKelas = if (part.isNotEmpty()) "$kelas $jurusan $part" else "$kelas $jurusan"

        val request = CreateSiswaRequest(
            nis = nis,
            nama_siswa = nama,
            jenis_kelamin = jk,
            kelas = kelas,
            jurusan = jurusan,
            id_tahun_masuk = tahunMasuk.toIntOrNull()
        )

        val token = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("auth_token", "") ?: ""
        
        btnKirim.isEnabled = false
        btnKirim.text = "MENGIRIM..."

        lifecycleScope.launch {
            repository.createSiswa(token, request).fold(
                onSuccess = {
                    runOnUiThread {
                        Toast.makeText(this@TambahSiswaActivity, "Siswa berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        btnKirim.isEnabled = true
                        btnKirim.text = "KIRIM"
                        Toast.makeText(this@TambahSiswaActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
