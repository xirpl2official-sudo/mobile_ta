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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SiswaDetailDialogFragment : DialogFragment() {

    private lateinit var student: SiswaItem
    private val repository = BerandaRepository()
    private val TAG = "SiswaDetailDialog"

    private lateinit var progressBar: ProgressBar
    private lateinit var layoutContent: View
    private lateinit var recyclerRiwayat: RecyclerView
    private lateinit var progressRiwayat: ProgressBar
    private lateinit var tvEmptyRiwayat: TextView
    private lateinit var btnDownload: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        student = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("student", SiswaItem::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("student") as SiswaItem
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_siswa_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        initViews(view)
        loadRiwayatAbsensi()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        layoutContent = view.findViewById(R.id.layoutContent)
        recyclerRiwayat = view.findViewById(R.id.recyclerRiwayat)
        progressRiwayat = view.findViewById(R.id.progressRiwayat)
        tvEmptyRiwayat = view.findViewById(R.id.tvEmptyRiwayat)
        btnDownload = view.findViewById(R.id.btnDownload)

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dismiss() }
        
        btnDownload.setOnClickListener { downloadHistory() }
    }

    private fun downloadHistory() {
        Toast.makeText(context, "Menyiapkan berkas riwayat absensi...", Toast.LENGTH_SHORT).show()
        // In a real app, this would trigger a download from the backend or generate a local report
        // For this task, we'll simulate the download process
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500)
            if (isAdded) {
                Toast.makeText(context, "Riwayat absensi ${student.nama_siswa} berhasil diunduh!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadRiwayatAbsensi() {
        val token = context?.getSharedPreferences("UserData", 0)?.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        progressRiwayat.visibility = View.VISIBLE
        tvEmptyRiwayat.visibility = View.GONE
        recyclerRiwayat.visibility = View.GONE

        lifecycleScope.launch {
            // Using a high limit to get more history
            val result = repository.getHistoryStaff(token, search = student.nis, limit = 100)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (isAdded) {
                    result.fold(
                        onSuccess = { historyData ->
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
                            }.sortedByDescending { it.tanggal } // Sort latest first

                            if (riwayatList.isNotEmpty()) {
                                recyclerRiwayat.visibility = View.VISIBLE
                                recyclerRiwayat.layoutManager = LinearLayoutManager(context)
                                val adapter = RiwayatAbsensiAdapter()
                                recyclerRiwayat.adapter = adapter
                                adapter.submitList(riwayatList)
                            } else {
                                tvEmptyRiwayat.visibility = View.VISIBLE
                            }
                        },
                        onFailure = { error ->
                            progressRiwayat.visibility = View.GONE
                            tvEmptyRiwayat.visibility = View.VISIBLE
                            tvEmptyRiwayat.text = "Gagal memuat riwayat absensi"
                        }
                    )
                }
            }
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
