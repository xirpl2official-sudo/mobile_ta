package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xirpl2.SASMobile.databinding.ActivityNotifikasiBinding
import com.xirpl2.SASMobile.model.Notifikasi
import com.xirpl2.SASMobile.model.StatusSholat
import com.xirpl2.SASMobile.model.JadwalSholat


class NotifikasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotifikasiBinding
    private lateinit var notifikasiAdapter: NotifikasiAdapter
    private var notificationList = mutableListOf<Notifikasi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotifikasiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        
        window.statusBarColor = 0xFF2886D6.toInt()
        
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupButtons()
        updateEmptyState()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun updateEmptyState() {
        if (notificationList.isEmpty()) {
            binding.rvNotifikasi.visibility = android.view.View.GONE
            binding.emptyState.visibility = android.view.View.VISIBLE
        } else {
            binding.rvNotifikasi.visibility = android.view.View.VISIBLE
            binding.emptyState.visibility = android.view.View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnHapusSemua.setOnClickListener {
            
            if (notificationList.isEmpty()) {
                
                finish()
                return@setOnClickListener
            }
            
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Hapus semua notifikasi")
                .setMessage("Apakah Anda yakin ingin menghapus semua notifikasi? Pastikan Anda telah membaca semuanya.")
                .setPositiveButton("Ya, hapus semua") { _, _ ->
                    notificationList.clear()
                    notifikasiAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    clearNotificationCounter()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
        binding.btnTandaiSemua.setOnClickListener {
            
            if (notificationList.isNotEmpty()) {
                clearNotificationCounter()
            }
        }
    }
    
    private fun clearNotificationCounter() {
        
        val sharedPref = getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
        sharedPref.edit().putInt("notification_count", 0).apply()
        
        
        val intent = android.content.Intent("com.xirpl2.SASMobile.NOTIFICATION_COUNT_CHANGED")
        intent.putExtra("count", 0)
        sendBroadcast(intent)
    }

    private fun setupRecyclerView() {
        val jenisKelamin = getJenisKelaminFromStorage()
        val jadwalList = JadwalSholatHelper.generateJadwalSholat(jenisKelamin)

        val upcomingPrayers = jadwalList.filter { it.status == StatusSholat.AKAN_DATANG }
        val missedPrayers = jadwalList.filter { it.status == StatusSholat.SELESAI }

        for (jadwal in upcomingPrayers) {
            notificationList.add(
                Notifikasi(
                    "Sholat ${jadwal.namaSholat}",
                    "Waktu sholat ${jadwal.namaSholat} akan segera tiba",
                    jadwal.jamMulai
                )
            )
        }

        for (jadwal in missedPrayers) {
            notificationList.add(
                Notifikasi(
                    "Sholat ${jadwal.namaSholat}",
                    "Waktu sholat ${jadwal.namaSholat} telah lewat",
                    jadwal.jamMulai
                )
            )
        }
        
        notifikasiAdapter = NotifikasiAdapter(notificationList)

        binding.rvNotifikasi.apply {
            layoutManager = LinearLayoutManager(this@NotifikasiActivity)
            adapter = notifikasiAdapter
        }
    }

    private fun getJenisKelaminFromStorage(): JadwalSholatHelper.JenisKelamin {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val jenisKelaminStr = sharedPref.getString("jenis_kelamin", "L") ?: "L"

        return if (jenisKelaminStr == "P") {
            JadwalSholatHelper.JenisKelamin.PEREMPUAN
        } else {
            JadwalSholatHelper.JenisKelamin.LAKI_LAKI
        }
    }
}
