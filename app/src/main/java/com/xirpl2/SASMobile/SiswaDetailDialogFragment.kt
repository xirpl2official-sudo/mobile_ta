package com.xirpl2.SASMobile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.xirpl2.SASMobile.model.SiswaItem

class SiswaDetailDialogFragment : DialogFragment() {

    private lateinit var student: SiswaItem

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

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dismiss() }

        populateStudentDetails(view)
    }

    private fun populateStudentDetails(view: View) {
        view.findViewById<TextView>(R.id.tvNis).text = student.nis.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tvNama).text = student.nama_siswa.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tvJenisKelamin).text = when (student.jenis_kelamin.uppercase()) {
            "L" -> "Laki-laki"
            "P" -> "Perempuan"
            else -> student.jenis_kelamin.ifEmpty { "-" }
        }
        view.findViewById<TextView>(R.id.tvKelas).text = student.kelas.ifEmpty { "-" }
        view.findViewById<TextView>(R.id.tvJurusan).text = student.jurusan.ifEmpty { "-" }

        val tvDeviceStatus = view.findViewById<TextView>(R.id.tvDeviceStatus)
        val status = student.deviceStatus?.lowercase()
        when {
            status == "verified" || status == "terdaftar" -> {
                tvDeviceStatus.text = "Terverifikasi"
                tvDeviceStatus.setTextColor(Color.parseColor("#10B981"))
            }
            status == "pending" -> {
                tvDeviceStatus.text = "Menunggu Verifikasi"
                tvDeviceStatus.setTextColor(Color.parseColor("#F59E0B"))
            }
            else -> {
                tvDeviceStatus.text = "Belum Terdaftar"
                tvDeviceStatus.setTextColor(Color.parseColor("#9CA3AF"))
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
