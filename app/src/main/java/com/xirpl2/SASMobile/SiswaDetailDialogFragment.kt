package com.xirpl2.SASMobile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.model.RiwayatAbsensi
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.model.StatusAbsensi
import com.xirpl2.SASMobile.repository.BerandaRepository
import com.xirpl2.SASMobile.repository.DeviceRepository
import kotlinx.coroutines.launch

class SiswaDetailDialogFragment : DialogFragment() {

    private lateinit var student: SiswaItem
    private val repository = BerandaRepository()
    private val deviceRepository = DeviceRepository()
    private val TAG = "SiswaDetailDialog"

    private lateinit var tvNis: TextView
    private lateinit var tvNama: TextView
    private lateinit var tvJurusan: TextView
    private lateinit var tvKelas: TextView
    private lateinit var tvJenisKelamin: TextView
    private lateinit var tvAgama: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutContent: View
    private lateinit var recyclerRiwayat: RecyclerView
    private lateinit var progressRiwayat: ProgressBar
    private lateinit var tvEmptyRiwayat: TextView
    private lateinit var btnResetDevice: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        student = arguments?.getSerializable("student") as SiswaItem
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_siswa_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        initViews(view)
        loadStudentDetail()
        loadRiwayatAbsensi()
    }

    private fun initViews(view: View) {
        tvNis = view.findViewById(R.id.tvNis)
        tvNama = view.findViewById(R.id.tvNama)
        tvJurusan = view.findViewById(R.id.tvJurusan)
        tvKelas = view.findViewById(R.id.tvKelas)
        tvJenisKelamin = view.findViewById(R.id.tvJenisKelamin)
        tvAgama = view.findViewById(R.id.tvAgama)
        progressBar = view.findViewById(R.id.progressBar)
        layoutContent = view.findViewById(R.id.layoutContent)
        recyclerRiwayat = view.findViewById(R.id.recyclerRiwayat)
        progressRiwayat = view.findViewById(R.id.progressRiwayat)
        tvEmptyRiwayat = view.findViewById(R.id.tvEmptyRiwayat)
        btnResetDevice = view.findViewById(R.id.btnResetDevice)

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dismiss() }
        view.findViewById<MaterialButton>(R.id.btnTutup).setOnClickListener { dismiss() }

        btnResetDevice.setOnClickListener { resetStudentDevice() }
        
        checkAdminRole()
        
        // Initial data from the item
        tvNis.text = student.nis
        tvNama.text = student.nama_siswa
        tvJurusan.text = student.jurusan
        tvKelas.text = student.kelas
        tvJenisKelamin.text = when(student.jenis_kelamin.uppercase()) {
            "L" -> "Laki-laki"
            "P" -> "Perempuan"
            else -> student.jenis_kelamin
        }
    }

    private fun checkAdminRole() {
        val role = context?.getSharedPreferences("user_session", 0)?.getString("user_role", "")?.lowercase() ?: ""
        if (role.contains("admin")) {
            btnResetDevice.visibility = View.VISIBLE
        } else {
            btnResetDevice.visibility = View.GONE
        }
    }

    private fun resetStudentDevice() {
        val token = context?.getSharedPreferences("UserData", 0)?.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset Perangkat")
            .setMessage("Apakah Anda yakin ingin melepas tautan perangkat untuk siswa ini? Siswa dapat melakukan login kembali di perangkat baru.")
            .setPositiveButton("Reset") { _, _ ->
                btnResetDevice.isEnabled = false
                btnResetDevice.text = "Memproses..."
                
                lifecycleScope.launch {
                    deviceRepository.resetDeviceByNIS(token, student.nis).fold(
                        onSuccess = { message ->
                            activity?.runOnUiThread {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                btnResetDevice.isEnabled = true
                                btnResetDevice.text = "Reset Kunci Perangkat"
                            }
                        },
                        onFailure = { error ->
                            activity?.runOnUiThread {
                                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                btnResetDevice.isEnabled = true
                                btnResetDevice.text = "Reset Kunci Perangkat"
                            }
                        }
                    )
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadStudentDetail() {
        val token = context?.getSharedPreferences("UserData", 0)?.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        progressBar.visibility = View.VISIBLE
        layoutContent.visibility = View.INVISIBLE

        lifecycleScope.launch {
            repository.getStudentDetail(token, student.nis).fold(
                onSuccess = { data ->
                    activity?.runOnUiThread {
                        progressBar.visibility = View.GONE
                        layoutContent.visibility = View.VISIBLE
                        
                        // Binding dynamic data from API
                        try {
                            tvNis.text = if (data.has("nis")) data.get("nis").asString else student.nis
                            tvNama.text = if (data.has("nama_siswa")) data.get("nama_siswa").asString else student.nama_siswa
                            tvJurusan.text = if (data.has("jurusan")) data.get("jurusan").asString else student.jurusan
                            tvKelas.text = if (data.has("kelas")) data.get("kelas").asString else student.kelas
                            
                            val jk = if (data.has("jk")) data.get("jk").asString else student.jenis_kelamin
                            tvJenisKelamin.text = when(jk.uppercase()) {
                                "L" -> "Laki-laki"
                                "P" -> "Perempuan"
                                else -> jk
                            }
                            
                            tvAgama.text = if (data.has("agama")) data.get("agama").asString else "-"
                        } catch (e: Exception) {
                            // Fallback to basic info if JSON parsing fails
                        }
                    }
                },
                onFailure = { error ->
                    activity?.runOnUiThread {
                        progressBar.visibility = View.GONE
                        layoutContent.visibility = View.VISIBLE
                        Toast.makeText(context, "Gagal memuat detail siswa. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun loadRiwayatAbsensi() {
        val token = context?.getSharedPreferences("UserData", 0)?.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        progressRiwayat.visibility = View.VISIBLE
        tvEmptyRiwayat.visibility = View.GONE
        recyclerRiwayat.visibility = View.GONE

        lifecycleScope.launch {
            repository.getHistoryStaff(token, search = student.nis).fold(
                onSuccess = { historyData ->
                    activity?.runOnUiThread {
                        progressRiwayat.visibility = View.GONE
                        
                        val riwayatList = historyData.absensi.map { absen ->
                            RiwayatAbsensi(
                                tanggal = absen.tanggal,
                                namaSholat = absen.jenis_sholat ?: "Unknown",
                                status = when(absen.status.uppercase()) {
                                    "HADIR" -> StatusAbsensi.HADIR
                                    "SAKIT" -> StatusAbsensi.SAKIT
                                    "IZIN" -> StatusAbsensi.IZIN
                                    else -> StatusAbsensi.ALPHA
                                },
                                waktuAbsen = absen.deskripsi
                            )
                        }

                        if (riwayatList.isNotEmpty()) {
                            recyclerRiwayat.visibility = View.VISIBLE
                            recyclerRiwayat.layoutManager = LinearLayoutManager(context)
                            recyclerRiwayat.adapter = RiwayatAbsensiAdapter(riwayatList)
                        } else {
                            tvEmptyRiwayat.visibility = View.VISIBLE
                        }
                    }
                },
                onFailure = { error ->
                    activity?.runOnUiThread {
                        progressRiwayat.visibility = View.GONE
                        tvEmptyRiwayat.visibility = View.VISIBLE
                        tvEmptyRiwayat.text = "Gagal memuat riwayat absensi"
                    }
                }
            )
        }
    }

    companion object {
        fun newInstance(student: SiswaItem): SiswaDetailDialogFragment {
            val fragment = SiswaDetailDialogFragment()
            val args = Bundle()
            args.putSerializable("student", student)
            fragment.arguments = args
            return fragment
        }
    }
}
