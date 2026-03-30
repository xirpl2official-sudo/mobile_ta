package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
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

class TambahAbsensiDialogFragment : DialogFragment() {

    private val TAG = "TambahAbsensiDialog"
    private val repository = BerandaRepository()

    // Views
    private lateinit var actvSiswa: AutoCompleteTextView
    private lateinit var tvSelectedSiswa: TextView
    private lateinit var etTanggal: TextInputEditText
    private lateinit var actvJadwal: AutoCompleteTextView
    private lateinit var rgStatus: RadioGroup
    private lateinit var rbHadir: RadioButton
    private lateinit var rbIzin: RadioButton
    private lateinit var rbSakit: RadioButton
    private lateinit var rbAlpha: RadioButton
    private lateinit var etDeskripsi: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnBatal: MaterialButton
    private lateinit var btnClose: ImageView

    // Data
    private var selectedSiswa: SiswaItem? = null
    private var selectedDate: String = "" // YYYY-MM-DD
    private var jadwalList: List<JadwalSholatData> = emptyList()
    private var selectedJadwalId: Int? = null
    private var selectedJenisSholat: String? = null

    // Search
    private var searchJob: Job? = null
    private var lastSearchResults: List<SiswaItem> = emptyList()

    // Callback for when data is saved
    var onDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make dialog background transparent to show the rounded card
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_tambah_presensi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        setupSearchSiswa()
        
        // Default date = today
        val calendar = Calendar.getInstance()
        updateDate(calendar)
        
        // Load initial jadwal
        loadJadwalSholat()
    }

    private fun initViews(view: View) {
        actvSiswa = view.findViewById(R.id.actvSiswa)
        tvSelectedSiswa = view.findViewById(R.id.tvSelectedSiswa)
        etTanggal = view.findViewById(R.id.etTanggal)
        actvJadwal = view.findViewById(R.id.actvJadwal)
        rgStatus = view.findViewById(R.id.rgStatus)
        rbHadir = view.findViewById(R.id.rbHadir)
        rbIzin = view.findViewById(R.id.rbIzin)
        rbSakit = view.findViewById(R.id.rbSakit)
        rbAlpha = view.findViewById(R.id.rbAlpha)
        etDeskripsi = view.findViewById(R.id.etDeskripsi)
        btnSimpan = view.findViewById(R.id.btnSimpan)
        btnBatal = view.findViewById(R.id.btnBatal)
        btnClose = view.findViewById(R.id.btnClose)
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnBatal.setOnClickListener { dismiss() }

        etTanggal.setOnClickListener { showDatePicker() }

        btnSimpan.setOnClickListener { submitForm() }
        
        actvJadwal.setOnItemClickListener { parent, _, position, _ ->
            selectedJenisSholat = parent.getItemAtPosition(position) as String
            updateSelectedJadwalId()
        }

        rgStatus.setOnCheckedChangeListener { _, checkedId ->
            // In premium theme, description might be optional for "Hadir"
            if (checkedId == R.id.rbHadir) {
                etDeskripsi.hint = "Opsional (Keterangan tambahan)"
            } else {
                etDeskripsi.hint = "Wajib diisi (Alasan perizinan)"
            }
        }
    }

    private fun updateSelectedJadwalId() {
        if (selectedJenisSholat == null || selectedSiswa == null || jadwalList.isEmpty()) {
            selectedJadwalId = null
            return
        }

        val targetJurusan = selectedSiswa?.jurusan?.lowercase() ?: ""
        
        // Better matching logic: check specific jurusan then general/null
        val match = jadwalList.find { 
            val isSameType = it.jenis_sholat.equals(selectedJenisSholat, ignoreCase = true)
            val scheduleJurusan = (it.jurusan ?: "").lowercase()
            
            isSameType && (scheduleJurusan == targetJurusan || scheduleJurusan == "umum" || scheduleJurusan.isEmpty())
        }
        
        selectedJadwalId = match?.id
        if (selectedJadwalId == null) {
            Log.w(TAG, "No matching schedule for $selectedJenisSholat and jurusan $targetJurusan")
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDate(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDate(calendar: Calendar) {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        selectedDate = sdf.format(calendar.time)
        etTanggal.setText(selectedDate)
        filterJadwalByDay()
    }

    private fun setupSearchSiswa() {
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
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val token = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
                .getString("auth_token", "") ?: ""
                
            if (token.isEmpty()) return@launch

            repository.getSiswaList(token, page = 1, pageSize = 20, search = query).fold(
                onSuccess = { response ->
                    lastSearchResults = response.data
                    val suggestions = response.data.map { "${it.nama_siswa} (${it.nis})" }
                    
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions)
                        actvSiswa.setAdapter(adapter)
                        
                        actvSiswa.setOnItemClickListener { parent, _, position, _ ->
                            if (position < lastSearchResults.size) {
                                selectedSiswa = lastSearchResults[position]
                                tvSelectedSiswa.apply {
                                    visibility = View.VISIBLE
                                    text = "Terpilih: ${selectedSiswa!!.nama_siswa} - ${selectedSiswa!!.kelas} ${selectedSiswa!!.jurusan}"
                                }
                                actvSiswa.setText(selectedSiswa!!.nama_siswa, false)
                                actvSiswa.dismissDropDown()
                                updateSelectedJadwalId()
                            }
                        }
                        actvSiswa.showDropDown()
                    }
                },
                onFailure = { Log.e(TAG, "Search failed: ${it.message}") }
            )
        }
    }

    private fun loadJadwalSholat() {
        val token = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
            
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    jadwalList = list
                    filterJadwalByDay()
                },
                onFailure = { Log.e(TAG, "Failed to load jadwal: ${it.message}") }
            )
        }
    }
    
    private fun filterJadwalByDay() {
        if (jadwalList.isEmpty() || !isAdded) return
        val uniqueTypes = jadwalList.map { it.jenis_sholat }.distinct()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, uniqueTypes)
        actvJadwal.setAdapter(adapter)
        updateSelectedJadwalId()
    }

    private fun submitForm() {
        if (selectedSiswa == null) {
            Toast.makeText(context, "Pilih siswa terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(context, "Pilih tanggal", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedJadwalId == null) {
            Toast.makeText(context, "Pilih jadwal sholat yang sesuai", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedStatusId = rgStatus.checkedRadioButtonId
        if (selectedStatusId == -1) {
            Toast.makeText(context, "Pilih status kehadiran", Toast.LENGTH_SHORT).show()
            return
        }
        
        val status = when (selectedStatusId) {
            R.id.rbHadir -> "hadir"
            R.id.rbIzin -> "izin"
            R.id.rbSakit -> "sakit"
            R.id.rbAlpha -> "alpha"
            else -> "hadir"
        }
        
        val deskripsi = etDeskripsi.text.toString().trim()
        if (status != "hadir" && deskripsi.isEmpty()) {
            Toast.makeText(context, "Keterangan wajib diisi untuk perizinan/alpha", Toast.LENGTH_SHORT).show()
            return
        }
        
        val request = CreateAbsensiRequest(
            id_jadwal = selectedJadwalId!!,
            status = status,
            tanggal = selectedDate,
            deskripsi = if (deskripsi.isEmpty()) "Mencatat presensi manual" else deskripsi
        )
        
        btnSimpan.isEnabled = false
        btnSimpan.text = "MENYIMPAN..."
        
        val token = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            repository.createAbsensi(token, selectedSiswa!!.nis, request).fold(
                onSuccess = {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Berhasil mencatat presensi!", Toast.LENGTH_LONG).show()
                        onDismissCallback?.invoke()
                        dismiss()
                    }
                },
                onFailure = { error ->
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Gagal: ${error.message}", Toast.LENGTH_LONG).show()
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "SIMPAN PRESENSI"
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to match parent with margins
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(onDismiss: (() -> Unit)? = null): TambahAbsensiDialogFragment {
            return TambahAbsensiDialogFragment().apply {
                onDismissCallback = onDismiss
            }
        }
    }
}
