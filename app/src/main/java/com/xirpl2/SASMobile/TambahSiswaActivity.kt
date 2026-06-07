package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.model.CreateSiswaRequest
import com.xirpl2.SASMobile.model.JurusanItem
import com.xirpl2.SASMobile.model.KelasItem
import com.xirpl2.SASMobile.model.AcademicYear
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class TambahSiswaActivity : BaseActivity() {

    private val repository = BerandaRepository()

    private lateinit var etNamaSiswa: TextInputEditText
    private lateinit var etNisSiswa: TextInputEditText
    private lateinit var actvTahunMasuk: AutoCompleteTextView
    private lateinit var actvJurusan: AutoCompleteTextView
    private lateinit var actvKelas: AutoCompleteTextView
    private lateinit var actvAgama: AutoCompleteTextView
    private lateinit var rgJenisKelamin: RadioGroup
    private lateinit var btnKirim: MaterialButton
    private lateinit var loadingOverlay: View

    private var jurusanList = listOf<JurusanItem>()
    private var kelasList = listOf<KelasItem>()
    private var tahunMasukList = listOf<AcademicYear>()

    private var selectedJurusanId: Int? = null
    private var selectedKelasId: Int? = null
    private var selectedTahunMasukId: Int? = null

    private val agamaOptions = listOf("Islam", "Kristen", "Katolik", "Hindu", "Buddha", "Konghucu")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_siswa)

        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
        initViews()
        setupListeners()
        loadLookups()
    }

    private fun initViews() {
        etNamaSiswa = findViewById(R.id.etNamaSiswa)
        etNisSiswa = findViewById(R.id.etNisSiswa)
        actvTahunMasuk = findViewById(R.id.actvTahunMasuk)
        actvJurusan = findViewById(R.id.actvJurusan)
        actvKelas = findViewById(R.id.actvKelas)
        actvAgama = findViewById(R.id.actvAgama)
        rgJenisKelamin = findViewById(R.id.rgJenisKelamin)
        btnKirim = findViewById(R.id.btnKirim)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        val agamaAdapter = ArrayAdapter(this, R.layout.dropdown_item, agamaOptions)
        actvAgama.setAdapter(agamaAdapter)
        actvAgama.setText(agamaOptions[0], false)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.iconBackContainer)?.setOnClickListener { finish() }
        findViewById<View>(R.id.iconBack)?.setOnClickListener { finish() }

        btnKirim.setOnClickListener { validateAndSubmit() }
    }

    private fun loadLookups() {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        lifecycleScope.launch {
            // Load Jurusan
            repository.getJurusanLookup(token).onSuccess { list ->
                jurusanList = list
                val namaList = list.map { it.nama }
                val jurusanAdapter = ArrayAdapter(this@TambahSiswaActivity, R.layout.dropdown_item, namaList)
                actvJurusan.setAdapter(jurusanAdapter)
            }

            // Load Tahun Masuk
            repository.getAcademicYearsList(token).onSuccess { list ->
                tahunMasukList = list
                val displayList = list.map { if (it.is_active) "${it.tahun} (Aktif)" else it.tahun }
                val tahunAdapter = ArrayAdapter(this@TambahSiswaActivity, R.layout.dropdown_item, displayList)
                actvTahunMasuk.setAdapter(tahunAdapter)

                val activeYear = list.find { it.is_active }
                if (activeYear != null) {
                    val idx = list.indexOf(activeYear)
                    actvTahunMasuk.setText(displayList[idx], false)
                    selectedTahunMasukId = activeYear.id
                }
            }

            // Load Kelas
            repository.getKelasLookup(token).onSuccess { list ->
                kelasList = list
                setupKelasDropdown(null)
            }

            // Jurusan selection → filter Kelas by jurusan (cascading)
            actvJurusan.setOnItemClickListener { parent, _, position, _ ->
                val selected = jurusanList[position]
                selectedJurusanId = selected.id
                selectedKelasId = null
                actvKelas.setText("", false)
                setupKelasDropdown(selected.nama)
            }

            // Kelas selection → store ID
            actvKelas.setOnItemClickListener { parent, _, position, _ ->
                val filtered = if (selectedJurusanId != null) {
                    kelasList.filter { it.jurusan == actvJurusan.text.toString() }
                } else {
                    kelasList
                }
                if (position < filtered.size) {
                    selectedKelasId = filtered[position].id_kelas
                }
            }

            // Tahun Masuk selection → store ID
            actvTahunMasuk.setOnItemClickListener { parent, _, position, _ ->
                if (position < tahunMasukList.size) {
                    selectedTahunMasukId = tahunMasukList[position].id
                }
            }
        }
    }

    private fun setupKelasDropdown(filterJurusan: String?) {
        val filtered = if (filterJurusan != null) {
            kelasList.filter { it.jurusan == filterJurusan }
        } else {
            kelasList
        }
        val labelList = filtered.map { it.label }
        val kelasAdapter = ArrayAdapter(this, R.layout.dropdown_item, labelList)
        actvKelas.setAdapter(kelasAdapter)
    }

    private fun validateAndSubmit() {
        val nama = etNamaSiswa.text?.toString()?.trim() ?: ""
        val nis = etNisSiswa.text?.toString()?.trim() ?: ""
        val jk = if (rgJenisKelamin.checkedRadioButtonId == R.id.rbLakiLaki) "L" else "P"
        val agama = actvAgama.text?.toString()?.trim() ?: "Islam"

        if (nama.isEmpty()) {
            etNamaSiswa.error = "Nama tidak boleh kosong"
            return
        }
        if (nis.isEmpty()) {
            etNisSiswa.error = "NIS tidak boleh kosong"
            return
        }

        val request = CreateSiswaRequest(
            nis = nis,
            nama_siswa = nama,
            jenis_kelamin = jk,
            id_kelas = selectedKelasId,
            id_jurusan = selectedJurusanId,
            id_tahun_masuk = selectedTahunMasukId,
            agama = agama,
            class_status = "active",
            status_akademik = "AKTIF"
        )

        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        btnKirim.isEnabled = false
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            repository.createSiswa(token, request).fold(
                onSuccess = {
                    runOnUiThread {
                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@TambahSiswaActivity, "Siswa berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        btnKirim.isEnabled = true
                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this@TambahSiswaActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
