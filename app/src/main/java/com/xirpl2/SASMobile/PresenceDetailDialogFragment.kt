package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
            tvTitle.text = "Detail Presensi Siswa"
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

        // Check student gender: female students see Zuhur (not Jumat) on Friday
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        val jenisKelamin = sharedPref.getString("jenis_kelamin", "L") ?: "L"
        val isMale = jenisKelamin.equals("L", ignoreCase = true)
        val showJumat = isFriday && isMale

        val itemDuha = view?.findViewById<View>(R.id.itemDuha)
        val itemZuhur = view?.findViewById<View>(R.id.itemZuhur)
        val itemJumat = view?.findViewById<View>(R.id.itemJumat)

        if (showJumat) {
            itemZuhur?.visibility = View.GONE
            itemJumat?.visibility = View.VISIBLE
        } else {
            itemZuhur?.visibility = View.VISIBLE
            itemJumat?.visibility = View.GONE
        }

        fetchJadwalAndSetupPrayers(itemDuha, itemZuhur, itemJumat, showJumat)
    }

    private fun setupPrayerItem(view: View, name: String, time: String) {
        view.findViewById<TextView>(R.id.tvPrayerName).text = name
        view.findViewById<TextView>(R.id.tvPrayerTime).text = time
        view.findViewById<TextView>(R.id.tvPresenceStatus).text = "Belum Sholat"
    }

    private fun fetchJadwalAndSetupPrayers(
        itemDuha: View?, itemZuhur: View?, itemJumat: View?, showJumat: Boolean
    ) {
        val token = getAuthToken()
        val major = if (studentMajor != null) studentMajor!! else {
            val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
            sharedPref.getString("jurusan", "") ?: ""
        }

        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { allJadwals ->
                    activity?.runOnUiThread {
                        // Filter schedules for today's day of week
                        val todayIndo = JadwalSholatHelper.getIndonesianDay()
                        val jadwals = allJadwals.filter { JadwalSholatHelper.isDayMatch(todayIndo, it.hari) }

                        // Duha
                        val isDuhaScheduled = jadwals.any {
                            it.jenis_sholat.equals("Duha", ignoreCase = true) &&
                                    (it.jurusan.equals("Semua Jurusan", ignoreCase = true) || it.jurusan.equals(major, ignoreCase = true))
                        }
                        if (isDuhaScheduled) {
                            itemDuha?.visibility = View.VISIBLE
                            val DuhaJadwal = jadwals.find { it.jenis_sholat.equals("Duha", true) }
                            val time = if (DuhaJadwal != null) "${DuhaJadwal.jam_mulai} - ${DuhaJadwal.jam_selesai}" else "06:30 - 09:00"
                            setupPrayerItem(itemDuha!!, "Salat Duha", time)
                        } else {
                            itemDuha?.visibility = View.GONE
                        }

                        // Zuhur / Jumat
                        if (showJumat) {
                            val jumatJadwal = jadwals.find { it.jenis_sholat.equals("Jumat", ignoreCase = true) }
                            val time = if (jumatJadwal != null) "${jumatJadwal.jam_mulai} - ${jumatJadwal.jam_selesai}" else "11:00 - 13:00"
                            setupPrayerItem(itemJumat!!, "Salat Jumat", time)
                        } else {
                            val ZuhurJadwal = jadwals.find { it.jenis_sholat.equals("Zuhur", ignoreCase = true) }
                            val time = if (ZuhurJadwal != null) "${ZuhurJadwal.jam_mulai} - ${ZuhurJadwal.jam_selesai}" else "11:30 - 13:00"
                            setupPrayerItem(itemZuhur!!, "Salat Zuhur", time)
                        }
                    }
                },
                onFailure = {
                    activity?.runOnUiThread {
                        itemDuha?.visibility = View.GONE
                        if (showJumat) {
                            setupPrayerItem(itemJumat!!, "Salat Jumat", "11:00 - 13:00")
                        } else {
                            setupPrayerItem(itemZuhur!!, "Salat Zuhur", "11:30 - 13:00")
                        }
                    }
                }
            )
        }
    }

    private fun loadAttendanceData() {
        val token = getAuthToken()
        
        lifecycleScope.launch {
            if (studentNis == null) {
                
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
                    onFailure = { e ->
                        Log.w("PresenceDetail", "Failed to load siswa history", e)
                        activity?.runOnUiThread {
                            tvHadirCount.text = "-"
                            tvIzinCount.text = "-"
                            tvSakitCount.text = "-"
                        }
                    }
                )
            } else {
                
                repository.getHistoryStaff(token, search = studentNis).fold(
                    onSuccess = { historyStaffData ->
                        activity?.runOnUiThread {
                            val absensiList = historyStaffData.absensi
                            tvHadirCount.text = absensiList.count { it.status.equals("HADIR", true) }.toString()
                            tvIzinCount.text = absensiList.count { it.status.equals("IZIN", true) }.toString()
                            tvSakitCount.text = absensiList.count { it.status.equals("SAKIT", true) }.toString()
                            
                            updatePrayerStatusesFromStaff(absensiList)
                        }
                    },
                    onFailure = { e ->
                        Log.w("PresenceDetail", "Failed to load staff history", e)
                        activity?.runOnUiThread {
                            tvHadirCount.text = "-"
                            tvIzinCount.text = "-"
                            tvSakitCount.text = "-"
                        }
                    }
                )
            }
        }
    }

    private fun updatePrayerStatusesFromHistory(absensiList: List<AbsensiHistoryItem>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAbsensi = absensiList.filter { it.tanggal == todayStr }

        updateStatus(view?.findViewById(R.id.itemDuha), "Duha", todayAbsensi.find { it.getPrayerName().equals("Duha", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemZuhur), "Zuhur", todayAbsensi.find { it.getPrayerName().equals("Zuhur", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemJumat), "Jumat", todayAbsensi.find { it.getPrayerName().equals("Jumat", true) }?.status)
    }

    private fun updatePrayerStatusesFromStaff(absensiList: List<com.xirpl2.SASMobile.model.AbsensiStaffItem>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAbsensi = absensiList.filter { it.tanggal == todayStr }

        updateStatus(view?.findViewById(R.id.itemDuha), "Duha", todayAbsensi.find { it.jenis_sholat.equals("Duha", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemZuhur), "Zuhur", todayAbsensi.find { it.jenis_sholat.equals("Zuhur", true) }?.status)
        updateStatus(view?.findViewById(R.id.itemJumat), "Jumat", todayAbsensi.find { it.jenis_sholat.equals("Jumat", true) }?.status)
    }

    private fun updateStatus(itemView: View?, type: String, status: String?) {
        itemView?.let { view ->
            val statusTv = view.findViewById<TextView>(R.id.tvPresenceStatus)
            when {
                status.equals("HADIR", true) -> {
                    statusTv.text = "Sudah Sholat ✅"
                    statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_success))
                }
                status.equals("IZIN", true) -> {
                    statusTv.text = "Izin"
                    statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                }
                status.equals("SAKIT", true) -> {
                    statusTv.text = "Sakit"
                    statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_warning))
                }
                status.equals("ALPHA", true) -> {
                    statusTv.text = "Alfa"
                    statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error))
                }
                else -> {
                    statusTv.text = "Belum Sholat"
                    statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_light))
                }
            }
        }
    }

    private fun getAuthToken(): String {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
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
                holder.tvDayName.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                holder.tvDateNumber.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                holder.itemView.findViewById<View>(R.id.dateContent).background = null
                holder.tvDayName.setTextColor(ContextCompat.getColor(requireContext(), R.color.slate_400))
                holder.tvDateNumber.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_background))
            }
        }

        override fun getItemCount(): Int = dates.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayName: TextView = view.findViewById(R.id.tvDayName)
            val tvDateNumber: TextView = view.findViewById(R.id.tvDateNumber)
        }
    }
}
