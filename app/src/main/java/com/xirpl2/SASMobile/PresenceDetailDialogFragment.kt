package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xirpl2.SASMobile.model.AbsensiHistoryItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PresenceDetailDialogFragment : BottomSheetDialogFragment() {

    private lateinit var rvDateStrip: RecyclerView
    private lateinit var tvHadirCount: TextView
    private lateinit var tvIzinCount: TextView
    private lateinit var tvSakitCount: TextView
    private lateinit var tvDateTitle: TextView
    private lateinit var tvTitle: TextView

    private val repository = BerandaRepository()
    
    private var studentNis: String? = null
    private var studentMajor: String? = null

    companion object {
        fun newInstance(nis: String? = null, jurusan: String? = null): PresenceDetailDialogFragment {
            val fragment = PresenceDetailDialogFragment()
            val args = Bundle()
            args.putString("nis", nis)
            args.putString("jurusan", jurusan)
            fragment.arguments = args
            return fragment
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentNis = arguments?.getString("nis")
        studentMajor = arguments?.getString("jurusan")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_presence_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupDateStrip()
        setupPrayerList()
        loadAttendanceData()

        view.findViewById<View>(R.id.btnTutup).setOnClickListener {
            dismiss()
        }
    }

    private fun initializeViews(view: View) {
        rvDateStrip = view.findViewById(R.id.rvDateStrip)
        tvHadirCount = view.findViewById(R.id.tvHadirCount)
        tvIzinCount = view.findViewById(R.id.tvIzinCount)
        tvSakitCount = view.findViewById(R.id.tvSakitCount)
        tvDateTitle = view.findViewById(R.id.tvDateTitle)
        tvTitle = view.findViewById(R.id.tvTitle)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvDateTitle.text = monthFormat.format(Date())
        
        if (studentNis != null) {
            tvTitle.text = "Detail Absensi Siswa"
        }
    }

    private fun setupDateStrip() {
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        for (i in 0 until 7) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        rvDateStrip.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = DateStripAdapter(dates)
        }
    }

    private fun setupPrayerList() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isFriday = currentDay == Calendar.FRIDAY

        val itemDhuha = view?.findViewById<View>(R.id.itemDhuha)
        val itemDzuhur = view?.findViewById<View>(R.id.itemDzuhur)
        val itemJumat = view?.findViewById<View>(R.id.itemJumat)

        if (isFriday) {
            itemDzuhur?.visibility = View.GONE
            itemJumat?.visibility = View.VISIBLE
            setupPrayerItem(itemJumat!!, "Sholat Jumat", "11:00 - 13:00")
        } else {
            itemDzuhur?.visibility = View.VISIBLE
            itemJumat?.visibility = View.GONE
            setupPrayerItem(itemDzuhur!!, "Sholat Dzuhur", "11:30 - 13:00")
        }

        checkDhuhaVisibility(itemDhuha)
    }

    private fun setupPrayerItem(view: View, name: String, time: String) {
        view.findViewById<TextView>(R.id.tvPrayerName).text = name
        view.findViewById<TextView>(R.id.tvPrayerTime).text = time
        view.findViewById<TextView>(R.id.tvPresenceStatus).text = "Belum Sholat"
    }

    private fun checkDhuhaVisibility(itemDhuha: View?) {
        val major = if (studentMajor != null) studentMajor!! else {
            val sharedPref = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
            sharedPref.getString("jurusan", "") ?: ""
        }
        
        val token = getAuthToken()
        
        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { jadwals ->
                    val isDhuhaScheduled = jadwals.any { 
                        it.jenis_sholat.equals("Dhuha", ignoreCase = true) && 
                                (it.jurusan.equals("Semua Jurusan", ignoreCase = true) || it.jurusan.equals(major, ignoreCase = true))
                    }
                    
                    activity?.runOnUiThread {
                        if (isDhuhaScheduled) {
                            itemDhuha?.visibility = View.VISIBLE
                            val dhuhaJadwal = jadwals.find { it.jenis_sholat.equals("Dhuha", true) }
                            val time = if (dhuhaJadwal != null) "${dhuhaJadwal.jam_mulai} - ${dhuhaJadwal.jam_selesai}" else "06:30 - 09:00"
                            setupPrayerItem(itemDhuha!!, "Sholat Dhuha", time)
                        } else {
                            itemDhuha?.visibility = View.GONE
                        }
                    }
                },
                onFailure = {
                    activity?.runOnUiThread { itemDhuha?.visibility = View.GONE }
                }
            )
        }
    }

    private fun loadAttendanceData() {
        val token = getAuthToken()
        
        lifecycleScope.launch {
            if (studentNis == null) {
                // Own history (student view)
                repository.getHistorySiswa(token, 0).fold(
                    onSuccess = { history ->
                        activity?.runOnUiThread {
                            val absensiList = history.absensi ?: emptyList()
                            tvHadirCount.text = absensiList.count { it.status.equals("HADIR", true) }.toString()
                            tvIzinCount.text = absensiList.count { it.status.equals("IZIN", true) }.toString()
                            tvSakitCount.text = absensiList.count { it.status.equals("SAKIT", true) }.toString()
                            
                            updatePrayerStatusesFromHistory(absensiList)
                        }
                    },
                    onFailure = { /* Handle error */ }
                )
            } else {
                // Other student history (admin view)
                repository.getHistoryStaff(token, nis = studentNis).fold(
                    onSuccess = { historyStaffData ->
                        activity?.runOnUiThread {
                            val absensiList = historyStaffData.absensi
                            tvHadirCount.text = absensiList.count { it.status.equals("HADIR", true) }.toString()
                            tvIzinCount.text = absensiList.count { it.status.equals("IZIN", true) }.toString()
                            tvSakitCount.text = absensiList.count { it.status.equals("SAKIT", true) }.toString()
                            
                            updatePrayerStatusesFromStaff(absensiList)
                        }
                    },
                    onFailure = { /* Handle error */ }
                )
            }
        }
    }

    private fun updatePrayerStatusesFromHistory(absensiList: List<AbsensiHistoryItem>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAbsensi = absensiList.filter { it.tanggal == todayStr }

        updateStatus(view?.findViewById(R.id.itemDhuha), "Dhuha", todayAbsensi.find { it.getPrayerName().equals("Dhuha", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemDzuhur), "Dzuhur", todayAbsensi.find { it.getPrayerName().equals("Dzuhur", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemJumat), "Jumat", todayAbsensi.find { it.getPrayerName().equals("Jumat", true) }?.status)
    }

    private fun updatePrayerStatusesFromStaff(absensiList: List<com.xirpl2.SASMobile.model.AbsensiStaffItem>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAbsensi = absensiList.filter { it.tanggal == todayStr }

        updateStatus(view?.findViewById(R.id.itemDhuha), "Dhuha", todayAbsensi.find { it.jenis_sholat.equals("Dhuha", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemDzuhur), "Dzuhur", todayAbsensi.find { it.jenis_sholat.equals("Dzuhur", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemJumat), "Jumat", todayAbsensi.find { it.jenis_sholat.equals("Jumat", true) }?.status)
    }

    private fun updateStatus(itemView: View?, type: String, status: String?) {
        itemView?.let { view ->
            val statusTv = view.findViewById<TextView>(R.id.tvPresenceStatus)
            if (status.equals("HADIR", true)) {
                statusTv.text = "Sudah Sholat ✅"
                statusTv.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            } else {
                statusTv.text = "Belum Sholat"
                statusTv.setTextColor(resources.getColor(android.R.color.darker_gray))
            }
        }
    }

    private fun getAuthToken(): String {
        val sharedPref = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    private inner class DateStripAdapter(private val dates: List<Date>) : 
        RecyclerView.Adapter<DateStripAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_strip, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val date = dates[position]
            val dayFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val numberFormat = SimpleDateFormat("d", Locale.getDefault())
            
            holder.tvDayName.text = dayFormat.format(date).uppercase()
            holder.tvDateNumber.text = numberFormat.format(date)

            val isToday = SimpleDateFormat("yyyyMMdd").format(date) == SimpleDateFormat("yyyyMMdd").format(Date())
            if (isToday) {
                holder.itemView.findViewById<View>(R.id.dateContent).setBackgroundResource(R.drawable.bg_date_selected)
                holder.tvDayName.setTextColor(resources.getColor(android.R.color.white))
                holder.tvDateNumber.setTextColor(resources.getColor(android.R.color.white))
            }
        }

        override fun getItemCount(): Int = dates.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayName: TextView = view.findViewById(R.id.tvDayName)
            val tvDateNumber: TextView = view.findViewById(R.id.tvDateNumber)
        }
    }
}
