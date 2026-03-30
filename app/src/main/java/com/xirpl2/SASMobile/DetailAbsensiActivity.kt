package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.AbsensiHistoryItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DetailAbsensiActivity : AppCompatActivity() {

    private lateinit var rvDateStrip: RecyclerView
    private lateinit var tvHadirCount: TextView
    private lateinit var tvIzinCount: TextView
    private lateinit var tvSakitCount: TextView
    private lateinit var tvDateTitle: TextView
    private lateinit var tvTitle: TextView

    private val repository = BerandaRepository()
    
    private var studentNis: String? = null
    private var studentMajor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_presence_detail)
        
        // Change background to solid white for activity
        findViewById<View>(R.id.handleBar)?.visibility = View.GONE
        findViewById<View>(R.id.main_container)?.setBackgroundColor(resources.getColor(android.R.color.white))

        // Get data from Intent
        studentNis = intent.getStringExtra("student_nis")
        studentMajor = intent.getStringExtra("student_major")
        val studentName = intent.getStringExtra("student_name")

        initializeViews()
        
        if (studentName != null) {
            tvTitle.text = "Detail Absensi: $studentName"
        }

        setupDateStrip()
        setupPrayerList()
        loadAttendanceData()

        findViewById<View>(R.id.btnTutup).setOnClickListener {
            finish()
            overridePendingTransition(0, R.anim.slide_out_bottom)
        }
    }

    private fun initializeViews() {
        rvDateStrip = findViewById(R.id.rvDateStrip)
        tvHadirCount = findViewById(R.id.tvHadirCount)
        tvIzinCount = findViewById(R.id.tvIzinCount)
        tvSakitCount = findViewById(R.id.tvSakitCount)
        tvDateTitle = findViewById(R.id.tvDateTitle)
        tvTitle = findViewById(R.id.tvTitle)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvDateTitle.text = monthFormat.format(Date())
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
            layoutManager = LinearLayoutManager(this@DetailAbsensiActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = DateStripAdapter(dates)
        }
    }

    private fun setupPrayerList() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val isFriday = currentDay == Calendar.FRIDAY

        val itemDhuha = findViewById<View>(R.id.itemDhuha)
        val itemDzuhur = findViewById<View>(R.id.itemDzuhur)
        val itemJumat = findViewById<View>(R.id.itemJumat)

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
        val major = studentMajor ?: ""
        val token = getAuthToken()
        
        lifecycleScope.launch {
            repository.getJadwalSholat(token).fold(
                onSuccess = { jadwals ->
                    val isDhuhaScheduled = jadwals.any { 
                        it.jenis_sholat.equals("Dhuha", ignoreCase = true) && 
                                (it.jurusan.equals("Semua Jurusan", ignoreCase = true) || it.jurusan.equals(major, ignoreCase = true))
                    }
                    
                    runOnUiThread {
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
                    runOnUiThread { itemDhuha?.visibility = View.GONE }
                }
            )
        }
    }

    private fun loadAttendanceData() {
        val token = getAuthToken()
        // Here we might need to use a different repository method if we are viewing ANOTHER student's history
        // But for now, let's assume getHistorySiswa can take a student NIS somehow or the admin uses a different endpoint.
        // Looking at BerandaRepository, getHistorySiswa doesn't take NIS.
        // We should use getHistoryStaff with NIS filter.
        
        lifecycleScope.launch {
            repository.getHistoryStaff(token, nis = studentNis).fold(
                onSuccess = { historyStaffData ->
                    runOnUiThread {
                        val absensiList = historyStaffData.absensi
                        tvHadirCount.text = absensiList.count { it.status.equals("HADIR", true) }.toString()
                        tvIzinCount.text = absensiList.count { it.status.equals("IZIN", true) }.toString()
                        tvSakitCount.text = absensiList.count { it.status.equals("SAKIT", true) }.toString()
                        
                        updatePrayerStatuses(absensiList)
                    }
                },
                onFailure = { /* Handle error */ }
            )
        }
    }

    private fun updatePrayerStatuses(absensiList: List<com.xirpl2.SASMobile.model.AbsensiStaffItem>) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayAbsensi = absensiList.filter { it.tanggal == todayStr }

        updateStatus(findViewById(R.id.itemDhuha), "Dhuha", todayAbsensi)
        updateStatus(findViewById(R.id.itemDzuhur), "Dzuhur", todayAbsensi)
        updateStatus(findViewById(R.id.itemJumat), "Jumat", todayAbsensi)
    }

    private fun updateStatus(itemView: View?, type: String, todayAbsensi: List<com.xirpl2.SASMobile.model.AbsensiStaffItem>) {
        itemView?.let { view ->
            val status = todayAbsensi.find { it.jenis_sholat.equals(type, true) }?.status
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
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
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
