package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    private lateinit var actvKelas: AutoCompleteTextView
    private lateinit var actvJurusan: AutoCompleteTextView
    private lateinit var rgJenisKelamin: RadioGroup
    private lateinit var btnKirim: MaterialButton
    private lateinit var loadingOverlay: android.view.View

    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")
    private val kelasOptions = listOf("10", "11", "12")
    private val tahunMasukOptions = (2020..2026).map { it.toString() }.reversed()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_siswa)
        
        setupStatusBar()
        initViews()
        setupDropdowns()
        setupListeners()
    }

    private fun setupStatusBar() {
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
    }

    private fun initViews() {
        etNamaSiswa = findViewById(R.id.etNamaSiswa)
        etNisSiswa = findViewById(R.id.etNisSiswa)
        actvTahunMasuk = findViewById(R.id.actvTahunMasuk)
        actvKelas = findViewById(R.id.actvKelas)
        actvJurusan = findViewById(R.id.actvJurusan)
        rgJenisKelamin = findViewById(R.id.rgJenisKelamin)
        btnKirim = findViewById(R.id.btnKirim)
        loadingOverlay = findViewById(R.id.loadingOverlay)
    }

    private fun setupDropdowns() {
        val tahunAdapter = ArrayAdapter(this, R.layout.dropdown_item, tahunMasukOptions)
        actvTahunMasuk.setAdapter(tahunAdapter)

        val kelasAdapter = ArrayAdapter(this, R.layout.dropdown_item, kelasOptions)
        actvKelas.setAdapter(kelasAdapter)

        val jurusanAdapter = ArrayAdapter(this, R.layout.dropdown_item, fixedJurusanList)
        actvJurusan.setAdapter(jurusanAdapter)
    }

    private fun setupListeners() {
        findViewById<android.view.View>(R.id.iconBackContainer)?.setOnClickListener {
            finish()
        }
        findViewById<android.view.View>(R.id.iconBack)?.setOnClickListener {
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

        val request = CreateSiswaRequest(
            nis = nis,
            nama_siswa = nama,
            jenis_kelamin = jk,
            id_tahun_masuk = tahunMasuk.toIntOrNull()
        )

        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("auth_token", "") ?: ""
        
        btnKirim.isEnabled = false
        loadingOverlay.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            repository.createSiswa(token, request).fold(
                onSuccess = {
                    runOnUiThread {
                        loadingOverlay.visibility = android.view.View.GONE
                        Toast.makeText(this@TambahSiswaActivity, "Siswa berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        btnKirim.isEnabled = true
                        loadingOverlay.visibility = android.view.View.GONE
                        Toast.makeText(this@TambahSiswaActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
