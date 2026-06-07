package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ManualPresensiSiswaDialogFragment : DialogFragment() {

    private val repository = BerandaRepository()

    private lateinit var tvNis: TextView
    private lateinit var tvNama: TextView
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

    private var selectedDate: String = ""
    private var jadwalList: List<JadwalSholatData> = emptyList()
    private var selectedJadwalId: Int? = null
    private var selectedJenisSholat: String? = null
    private var studentNis: String = ""
    private var studentNama: String = ""
    private var studentJurusan: String = ""

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_manual_presensi_siswa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadStudentData()
        setupListeners()

        val calendar = Calendar.getInstance()
        updateDate(calendar)

        loadJadwalSholat()
    }

    private fun initViews(view: View) {
        tvNis = view.findViewById(R.id.tvNis)
        tvNama = view.findViewById(R.id.tvNama)
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

    private fun loadStudentData() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(requireContext())
        studentNis = session.getString("user_nis", "") ?: ""
        studentNama = session.getString("user_name", "") ?: ""
        studentJurusan = session.getString("user_jurusan", "") ?: ""

        tvNis.text = studentNis.ifEmpty { "-" }
        tvNama.text = studentNama.ifEmpty { "-" }
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
            if (checkedId == R.id.rbHadir) {
                etDeskripsi.hint = "Opsional (Keterangan tambahan)"
            } else {
                etDeskripsi.hint = "Wajib diisi (Alasan perizinan)"
            }
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

    private fun loadJadwalSholat() {
        val token = getToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { list ->
                    jadwalList = list
                    filterJadwalByDay()
                },
                onFailure = {
                    Toast.makeText(context, "Gagal memuat jadwal sholat", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun filterJadwalByDay() {
        if (jadwalList.isEmpty() || !isAdded) return
        var uniqueTypes = jadwalList.map { it.jenis_sholat }.distinct().toMutableList()

        val currentDay = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(selectedDate)
            val cal = Calendar.getInstance()
            cal.time = date!!
            cal.get(Calendar.DAY_OF_WEEK)
        } catch (e: Exception) {
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        }

        val isFriday = currentDay == Calendar.FRIDAY

        if (isFriday) {
            val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(requireContext())
            val gender = session.getString("user_jk", "L") ?: "L"
            if (gender.equals("L", ignoreCase = true)) {
                uniqueTypes.removeAll { it.equals("Dzuhur", ignoreCase = true) }
            } else if (gender.equals("P", ignoreCase = true)) {
                uniqueTypes.removeAll { it.equals("Jumat", ignoreCase = true) }
            }
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, uniqueTypes)
        actvJadwal.setAdapter(adapter)
        updateSelectedJadwalId()
    }

    private fun updateSelectedJadwalId() {
        if (selectedJenisSholat == null || jadwalList.isEmpty()) {
            selectedJadwalId = null
            return
        }

        val targetJurusan = studentJurusan.lowercase()

        val match = jadwalList.find {
            val isSameType = it.jenis_sholat.equals(selectedJenisSholat, ignoreCase = true)
            val scheduleJurusan = (it.jurusan ?: "").lowercase()
            isSameType && (scheduleJurusan == targetJurusan || scheduleJurusan == "umum" || scheduleJurusan.isEmpty())
        }

        selectedJadwalId = match?.id
    }

    private fun submitForm() {
        if (studentNis.isEmpty()) {
            Toast.makeText(context, "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
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

        val token = getToken()

        lifecycleScope.launch {
            repository.createAbsensi(token, studentNis, request).fold(
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

    private fun getToken(): String {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        return session.getString("auth_token", "") ?: ""
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(onDismiss: (() -> Unit)? = null): ManualPresensiSiswaDialogFragment {
            return ManualPresensiSiswaDialogFragment().apply {
                onDismissCallback = onDismiss
            }
        }
    }
}
