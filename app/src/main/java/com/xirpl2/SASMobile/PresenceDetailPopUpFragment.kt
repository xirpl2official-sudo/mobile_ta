package com.xirpl2.SASMobile

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PresenceDetailPopUpFragment : DialogFragment() {

    private lateinit var rvDateStrip: RecyclerView
    private lateinit var rvPrayerData: RecyclerView
    private lateinit var tvHadirCount: TextView
    private lateinit var tvIzinCount: TextView
    private lateinit var tvSakitCount: TextView
    private lateinit var tvMonthLabel: TextView
    private lateinit var tvTitle: TextView

    private val repository = BerandaRepository()
    private var studentNis: String? = null
    private var studentName: String? = null
    
    private var allAbsensiList = mutableListOf<AbsensiStaffItem>()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    companion object {
        fun newInstance(nis: String?, jurusan: String?, name: String?): PresenceDetailPopUpFragment {
            val fragment = PresenceDetailPopUpFragment()
            val args = Bundle()
            args.putString("nis", nis)
            args.putString("name", name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return inflater.inflate(R.layout.dialog_presence_detail_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studentNis = arguments?.getString("nis")
        studentName = arguments?.getString("name")

        initializeViews(view)
        setupDateStrip()
        loadAttendanceData()

        view.findViewById<View>(R.id.btnTutup).setOnClickListener {
            dismiss()
        }
    }

    private fun initializeViews(view: View) {
        rvDateStrip = view.findViewById(R.id.rvDateStrip)
        rvPrayerData = view.findViewById(R.id.rvPrayerData)
        tvHadirCount = view.findViewById(R.id.tvHadirCount)
        tvIzinCount = view.findViewById(R.id.tvIzinCount)
        tvSakitCount = view.findViewById(R.id.tvSakitCount)
        tvMonthLabel = view.findViewById(R.id.tvMonthLabel)
        tvTitle = view.findViewById(R.id.tvTitle)

        if (studentName != null) {
            tvTitle.text = "Detail Absensi: $studentName"
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvMonthLabel.text = "Ringkasan ${monthFormat.format(Date())}"
        
        rvPrayerData.layoutManager = LinearLayoutManager(context)
    }

    private fun setupDateStrip() {
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        
        // Date strip for last 7 days
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        rvDateStrip.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = DateStripAdapterV2(dates) { date ->
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                filterPrayerByDate()
            }
            scrollToPosition(6)
        }
    }

    private fun loadAttendanceData() {
        val token = getAuthToken()
        val cal = Calendar.getInstance()
        
        // Calculate start and end of current month for accurate Ringkasan
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = sdf.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = sdf.format(cal.time)

        lifecycleScope.launch {
            // Fetch monthly data for the specific student
            repository.getHistoryStaff(
                token = token,
                startDate = startDate,
                endDate = endDate,
                nis = studentNis
            ).fold(
                onSuccess = { data ->
                    activity?.runOnUiThread {
                        allAbsensiList.clear()
                        allAbsensiList.addAll(data.absensi)
                        
                        // Use accurate statistics from backend instead of local counting
                        data.statistik?.let { stat ->
                            tvHadirCount.text = stat.total_hadir.toString()
                            tvIzinCount.text = stat.total_izin.toString()
                            tvSakitCount.text = stat.total_sakit.toString()
                        }
                        
                        filterPrayerByDate()
                    }
                },
                onFailure = { /* Handle error */ }
            )
        }
    }

    private fun filterPrayerByDate() {
        val filteredList = allAbsensiList.filter { it.tanggal == selectedDate }
        rvPrayerData.adapter = PrayerDataAdapter(filteredList)
    }

    private fun getAuthToken(): String {
        return requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
    }

    private inner class DateStripAdapterV2(
        private val dates: List<Date>,
        private val onDateSelected: (Date) -> Unit
    ) : RecyclerView.Adapter<DateStripAdapterV2.ViewHolder>() {

        private var selectedPos = 6

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_strip_v2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val date = dates[position]
            val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val numberFormat = SimpleDateFormat("d", Locale.getDefault())
            
            holder.tvDayName.text = dayFormat.format(date).uppercase()
            holder.tvDateNumber.text = numberFormat.format(date)

            val isSelected = position == selectedPos
            val cardView = holder.itemView as com.google.android.material.card.MaterialCardView
            
            if (isSelected) {
                cardView.setCardBackgroundColor(Color.BLACK)
                holder.tvDayName.setTextColor(Color.WHITE)
                holder.tvDateNumber.setTextColor(Color.WHITE)
                cardView.strokeWidth = 0
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
                holder.tvDayName.setTextColor(Color.parseColor("#999999"))
                holder.tvDateNumber.setTextColor(Color.parseColor("#1A1A1A"))
                cardView.strokeWidth = 1
            }

            holder.itemView.setOnClickListener {
                val oldPos = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPos)
                onDateSelected(date)
            }
        }

        override fun getItemCount(): Int = dates.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayName: TextView = view.findViewById(R.id.tvDayName)
            val tvDateNumber: TextView = view.findViewById(R.id.tvDateNumber)
        }
    }

    private inner class PrayerDataAdapter(
        private val items: List<AbsensiStaffItem>
    ) : RecyclerView.Adapter<PrayerDataAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_presence_prayer_v2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = "Sholat ${item.jenis_sholat ?: "Unknown"}"
            
            when (item.status.uppercase()) {
                "HADIR" -> {
                    holder.tvStatus.text = "Sudah Sholat ✅"
                    holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                "ALPHA" -> {
                    holder.tvStatus.text = "Belum Sholat"
                    holder.tvStatus.setTextColor(Color.parseColor("#999999"))
                }
                "SAKIT" -> {
                    holder.tvStatus.text = "Sakit"
                    holder.tvStatus.setTextColor(Color.parseColor("#EF6C00"))
                }
                "IZIN" -> {
                    holder.tvStatus.text = "Izin"
                    holder.tvStatus.setTextColor(Color.parseColor("#1565C0"))
                }
                else -> {
                    holder.tvStatus.text = item.status
                    holder.tvStatus.setTextColor(Color.parseColor("#999999"))
                }
            }
            
            val timeRange = when (item.jenis_sholat?.uppercase() ?: "") {
                "DHUHA" -> "06:30:00 - 08:00:00"
                "DZUHUR" -> "11:30:00 - 13:00:00"
                "ASHAR" -> "15:15:00 - 16:00:00"
                "JUMAT" -> "11:00:00 - 13:00:00"
                else -> "--:-- - --:--"
            }
            holder.tvTime.text = timeRange
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPrayerName)
            val tvStatus: TextView = view.findViewById(R.id.tvPresenceStatus)
            val tvTime: TextView = view.findViewById(R.id.tvPrayerTime)
        }
    }
}
