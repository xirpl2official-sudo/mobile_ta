package com.xirpl2.SASMobile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistorySiswaDialogFragment : DialogFragment() {

    private lateinit var student: SiswaItem
    private val repository = BerandaRepository()
    private val TAG = "HistorySiswaDialog"

    private lateinit var recyclerHistory: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvPeriode: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        student = arguments?.getSerializable("student") as SiswaItem
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_history_siswa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        initViews(view)
        setupRecyclerView()
        loadHistory()
    }

    private fun initViews(view: View) {
        val tvStudentInfo = view.findViewById<TextView>(R.id.tvStudentInfo)
        tvStudentInfo.text = "NIS: ${student.nis} | Nama: ${student.nama_siswa}"

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dismiss() }
        view.findViewById<MaterialButton>(R.id.btnTutup).setOnClickListener { dismiss() }

        recyclerHistory = view.findViewById(R.id.recyclerHistory)
        progressLoading = view.findViewById(R.id.progressLoading)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvPeriode = view.findViewById(R.id.tvPeriode)
    }

    private fun setupRecyclerView() {
        recyclerHistory.layoutManager = LinearLayoutManager(context)
    }

    private fun loadHistory() {
        val token = context?.getSharedPreferences("UserData", 0)?.getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerHistory.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        // Calculate Monday to Sunday of current week
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = sdf.format(calendar.time)

        tvPeriode.text = "Periode: ${startDate} s/d ${endDate}"

        lifecycleScope.launch {
            repository.getHistoryStaff(
                token = token,
                startDate = startDate,
                endDate = endDate,
                nis = student.nis
            ).fold(
                onSuccess = { data ->
                    activity?.runOnUiThread {
                        progressLoading.visibility = View.GONE
                        val list = data.absensi
                        if (list.isNullOrEmpty()) {
                            tvEmptyState.visibility = View.VISIBLE
                        } else {
                            recyclerHistory.visibility = View.VISIBLE
                            recyclerHistory.adapter = HistoryAdapter(list)
                        }
                    }
                },
                onFailure = { error ->
                    activity?.runOnUiThread {
                        progressLoading.visibility = View.GONE
                        tvEmptyState.text = "Gagal memuat data: ${error.message}"
                        tvEmptyState.visibility = View.VISIBLE
                    }
                }
            )
        }
    }

    private inner class HistoryAdapter(private val historyList: List<AbsensiStaffItem>) :
        RecyclerView.Adapter<HistoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_detail, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(historyList[position])
        }

        override fun getItemCount(): Int = historyList.size
    }

    private inner class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTanggal = view.findViewById<TextView>(R.id.tvTanggal)
        private val tvJenisSholat = view.findViewById<TextView>(R.id.tvJenisSholat)
        private val tvStatus = view.findViewById<TextView>(R.id.tvStatus)

        fun bind(item: AbsensiStaffItem) {
            tvTanggal.text = "${item.hari}, ${item.tanggal}"
            tvJenisSholat.text = item.jenis_sholat
            tvStatus.text = item.status.uppercase()
            
            // Set status color
            when (item.status.lowercase()) {
                "hadir" -> tvStatus.setBackgroundResource(R.drawable.bg_status_hadir)
                "izin" -> tvStatus.setBackgroundResource(R.drawable.bg_status_izin)
                "alpha" -> tvStatus.setBackgroundResource(R.drawable.bg_status_alpha)
            }
        }
    }

    companion object {
        fun newInstance(student: SiswaItem): HistorySiswaDialogFragment {
            val fragment = HistorySiswaDialogFragment()
            val args = Bundle()
            args.putSerializable("student", student)
            fragment.arguments = args
            return fragment
        }
    }
}
