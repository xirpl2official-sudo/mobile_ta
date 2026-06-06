package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.AbsensiHistoryItem
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DetailAbsensiActivity : BaseActivity() {

    private lateinit var rvDateStrip: RecyclerView
    private lateinit var tvHadirCount: TextView
    private lateinit var tvIzinCount: TextView
    private lateinit var tvSakitCount: TextView
    private lateinit var tvDateTitle: TextView
    private lateinit var tvTitle: TextView

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val repository = BerandaRepository()
    
    private var studentNis: String? = null
    private var studentMajor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_absensi)

        initializeViews()

        // Setup back button
        findViewById<android.widget.ImageView>(R.id.iconBack)?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        studentNis = intent.getStringExtra("student_nis")
        studentMajor = intent.getStringExtra("student_major")
        val studentName = intent.getStringExtra("student_name")
        
        if (studentName != null) {
            tvTitle.text = "Detail Absensi: $studentName"
        }

        setupDateStrip()
        setupPrayerList()
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadAttendanceData() }

        loadAttendanceData()

        findViewById<View>(R.id.btnTutup).setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
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
            itemJumat?.let { setupPrayerItem(it, "Sholat Jumat", "11:00 - 13:00") }
        } else {
            itemDzuhur?.visibility = View.VISIBLE
            itemJumat?.visibility = View.GONE
            itemDzuhur?.let { setupPrayerItem(it, "Sholat Dzuhur", "11:30 - 13:00") }
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
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    val isDhuhaScheduled = jadwals.any { 
                        it.jenis_sholat.equals("Dhuha", ignoreCase = true) && 
                                (it.jurusan.equals("Semua Jurusan", ignoreCase = true) || it.jurusan.equals(major, ignoreCase = true))
                    }
                    
                    runOnUiThread {
                        if (isDhuhaScheduled) {
                            itemDhuha?.visibility = View.VISIBLE
                            val dhuhaJadwal = jadwals.find { it.jenis_sholat.equals("Dhuha", true) }
                            val time = if (dhuhaJadwal != null) "${dhuhaJadwal.jam_mulai} - ${dhuhaJadwal.jam_selesai}" else "06:30 - 09:00"
                            itemDhuha?.let { setupPrayerItem(it, "Sholat Dhuha", time) }
                        } else {
                            itemDhuha?.visibility = View.GONE
                        }
                    }
                },
                onFailure = {
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    runOnUiThread { itemDhuha?.visibility = View.GONE }
                }
            )
        }
    }

    private fun loadAttendanceData() {
        val token = getAuthToken()
        
        
        
        
        
        lifecycleScope.launch {
            val filtersMap: Map<String, String>? = studentNis?.let { mapOf("nis" to it) }
            repository.getHistoryStaff(
                token = token,
                filters = filtersMap
            ).fold(
                onSuccess = { historyStaffData ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    runOnUiThread {
                        val absensiList = historyStaffData.absensi
                        tvHadirCount.text = absensiList.count { it.status.equals("HADIR", true) }.toString()
                        tvIzinCount.text = absensiList.count { it.status.equals("IZIN", true) }.toString()
                        tvSakitCount.text = absensiList.count { it.status.equals("SAKIT", true) }.toString()
                        
                        updatePrayerStatuses(absensiList)
                    }
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    runOnUiThread {
                        Toast.makeText(this@DetailAbsensiActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
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
                statusTv.setTextColor(ContextCompat.getColor(this, R.color.status_success))
            } else {
                statusTv.text = "Belum Sholat"
                statusTv.setTextColor(ContextCompat.getColor(this, R.color.gray_light))
            }
        }
    }

    private fun getAuthToken(): String {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
        return sharedPref.getString("auth_token", "") ?: ""
    }

    private class DateStripAdapter(private val dates: List<Date>) : 
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
                holder.tvDayName.setTextColor(holder.itemView.resources.getColor(android.R.color.white))
                holder.tvDateNumber.setTextColor(holder.itemView.resources.getColor(android.R.color.white))
            } else {
                holder.itemView.findViewById<View>(R.id.dateContent).background = null
                holder.tvDayName.setTextColor(holder.itemView.resources.getColor(R.color.slate_400))
                holder.tvDateNumber.setTextColor(holder.itemView.resources.getColor(R.color.on_background))
            }
        }

        override fun getItemCount(): Int = dates.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayName: TextView = view.findViewById(R.id.tvDayName)
            val tvDateNumber: TextView = view.findViewById(R.id.tvDateNumber)
        }
    }
}
