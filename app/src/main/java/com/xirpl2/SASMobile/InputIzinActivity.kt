package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.model.CreateAbsensiRequest
import com.xirpl2.SASMobile.model.JadwalSholatData
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputIzinActivity : AppCompatActivity() {

    private val TAG = "InputIzinActivity"
    private val repository = BerandaRepository()

    // Views
    private lateinit var actvSiswa: AutoCompleteTextView
    private lateinit var tvSelectedSiswa: android.widget.TextView
    private lateinit var etTanggal: TextInputEditText
    private lateinit var actvJadwal: AutoCompleteTextView
    private lateinit var rgStatus: RadioGroup
    private lateinit var etDeskripsi: TextInputEditText
    private lateinit var btnSimpan: MaterialButton

    // Data
    private var selectedSiswa: SiswaItem? = null
    private var selectedDate: String = "" // YYYY-MM-DD
    private var jadwalList: List<JadwalSholatData> = emptyList()
    private var selectedJadwalId: Int? = null

    // Search
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_izin)

        setupStatusBar()
        initViews()
        setupListeners()
        setupSearchSiswa()
        
        // Default date = today
        val calendar = Calendar.getInstance()
        updateDate(calendar)
        
        // Load initial jadwal for today
        loadJadwalSholat()
    }

    private fun setupStatusBar() {
        window.statusBarColor = 0xFF2886D6.toInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun initViews() {
        actvSiswa = findViewById(R.id.actvSiswa)
        tvSelectedSiswa = findViewById(R.id.tvSelectedSiswa)
        etTanggal = findViewById(R.id.etTanggal)
        actvJadwal = findViewById(R.id.actvJadwal)
        rgStatus = findViewById(R.id.rgStatus)
        etDeskripsi = findViewById(R.id.etDeskripsi)
        btnSimpan = findViewById(R.id.btnSimpan)
        
        // Back button on toolbar
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupListeners() {
        // Date Picker
        etTanggal.setOnClickListener {
            showDatePicker()
        }

        // Simpan Button
        btnSimpan.setOnClickListener {
            submitForm()
        }
        
        // Jadwal Selection
        actvJadwal.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            // Format: "Dhuha - TKJ (06:30)" or "Dzuhur - Umum (12:00)"
            // Find corresponding ID based on exact string match since we generated these strings
            val jadwal = jadwalList.find { 
                val jurusanDisplay = if (it.jurusan.isNullOrEmpty()) "Umum" else it.jurusan
                val display = "${it.jenis_sholat} - $jurusanDisplay (${it.jam_mulai})"
                display == selectedItem
            }
            if (jadwal != null) {
                selectedJadwalId = jadwal.id
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDate(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDate(calendar: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        selectedDate = sdf.format(calendar.time)
        etTanggal.setText(selectedDate)
        
        // Let's populate the dropdown with all available schedules.
        // We will trust the API's schedule list and just format them nicely with Jurusan
        filterJadwalByDay()
    }

    private fun setupSearchSiswa() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        actvSiswa.setAdapter(adapter)

        actvSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.length >= 2) {
                    performSearch(query)
                }
            }
        })
        
        actvSiswa.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position) as String
            // Selection format: "Nama (NIS)"
            // We need to find the actual object. Since we only have strings here, 
            // we rely on the last search result or we store a map.
            // Simplified: we will just store the last list of students
        }
    }
    
    // Quick cache for search results
    private var lastSearchResults: List<SiswaItem> = emptyList()

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val token = getSharedPreferences("UserData", MODE_PRIVATE).getString("auth_token", "") ?: ""
            if (token.isEmpty()) return@launch

            repository.getSiswaList(token, page = 1, pageSize = 20, search = query).fold(
                onSuccess = { response ->
                    lastSearchResults = response.data
                    val suggestions = response.data.map { "${it.nama_siswa} (${it.nis})" }
                    
                    runOnUiThread {
                        val adapter = ArrayAdapter(this@InputIzinActivity, android.R.layout.simple_dropdown_item_1line, suggestions)
                        actvSiswa.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                        
                        // Re-set listener because setAdapter might clear it? No, but safe to keep logic consistent
                        actvSiswa.setOnItemClickListener { parent, _, position, _ ->
                            if (position < lastSearchResults.size) {
                                selectedSiswa = lastSearchResults[position]
                                tvSelectedSiswa.text = "Terpilih: ${selectedSiswa!!.nama_siswa} - Kelas ${selectedSiswa!!.kelas} ${selectedSiswa!!.jurusan}"
                                actvSiswa.dismissDropDown() // Hide dropdown
                            }
                        }
                        
                        actvSiswa.showDropDown()
                    }
                },
                onFailure = {
                    Log.e(TAG, "Search failed: ${it.message}")
                }
            )
        }
    }

    private fun loadJadwalSholat() {
        val token = getSharedPreferences("UserData", MODE_PRIVATE).getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    jadwalList = list
                    // Initial filter setup
                    filterJadwalByDay()
                },
                onFailure = {
                    Log.e(TAG, "Failed to load jadwal: ${it.message}")
                    Toast.makeText(this@InputIzinActivity, "Gagal memuat jadwal sholat", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    
    private fun filterJadwalByDay() {
        if (jadwalList.isEmpty()) return

        // We use all items returned by the API since they represent active/valid schedules.
        // Remove duplicates based on jenis_sholat + jurusan + waktu_mulai to be safe
        val uniqueJadwal = jadwalList.distinctBy { "${it.jenis_sholat}-${it.jurusan}-${it.jam_mulai}" }
        
        val options = uniqueJadwal.map { 
            val jurusanDisplay = if (it.jurusan.isNullOrEmpty()) "Umum" else it.jurusan
            "${it.jenis_sholat} - $jurusanDisplay (${it.jam_mulai})" 
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        
        runOnUiThread {
            actvJadwal.setAdapter(adapter)
            // if list not empty, maybe auto select first?
            if (options.isNotEmpty()) {
                // actvJadwal.setText(options[0], false)
                // selectedJadwalId = uniqueJadwal[0].id
            }
        }
        
        // Update click listener to use this filtered list (handled by global setupListeners)
    }

    private fun submitForm() {
        if (selectedSiswa == null) {
            actvSiswa.error = "Pilih siswa terlebih dahulu"
            return
        }
        if (selectedDate.isEmpty()) {
            etTanggal.error = "Pilih tanggal"
            return
        }
        if (selectedJadwalId == null) {
            actvJadwal.error = "Pilih jadwal sholat"
            Toast.makeText(this, "Silakan pilih jadwal sholat dari list", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedStatusId = rgStatus.checkedRadioButtonId
        if (selectedStatusId == -1) {
            Toast.makeText(this, "Pilih status kehadiran", Toast.LENGTH_SHORT).show()
            return
        }
        
        val status = when (selectedStatusId) {
            R.id.rbIzin -> "izin"
            R.id.rbSakit -> "sakit"
            R.id.rbAlpha -> "alpha"
            else -> "izin"
        }
        
        val deskripsi = etDeskripsi.text.toString()
        
        val request = CreateAbsensiRequest(
            id_jadwal = selectedJadwalId!!,
            status = status,
            tanggal = selectedDate,
            deskripsi = deskripsi
        )
        
        btnSimpan.isEnabled = false
        btnSimpan.text = "Menyimpan..."
        
        val token = getSharedPreferences("UserData", MODE_PRIVATE).getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            repository.createAbsensi(token, selectedSiswa!!.nis, request).fold(
                onSuccess = {
                    runOnUiThread {
                        Toast.makeText(this@InputIzinActivity, "Berhasil mencatat izin!", Toast.LENGTH_LONG).show()
                        finish() // Close activity
                    }
                },
                onFailure = {
                    runOnUiThread {
                        Toast.makeText(this@InputIzinActivity, "Gagal: ${it.message}", Toast.LENGTH_LONG).show()
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "Simpan Data"
                    }
                }
            )
        }
    }
}
