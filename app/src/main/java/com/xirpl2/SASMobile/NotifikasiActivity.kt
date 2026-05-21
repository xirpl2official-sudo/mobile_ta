package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xirpl2.SASMobile.databinding.ActivityNotifikasiBinding
import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class NotifikasiActivity : BaseActivity() {

    private lateinit var binding: ActivityNotifikasiBinding
    private lateinit var notifikasiAdapter: NotifikasiAdapter
    private var notificationList = mutableListOf<Notifikasi>()
    private var isLoading = false
    private var currentPage = 1
    private val limit = 20

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
        loadNotifications()

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
            
            // Hapus semua via API
            lifecycleScope.launch {
                try {
                    val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@NotifikasiActivity)
                        .getString("auth_token", "") ?: ""

                    val idsToDelete = notificationList.map { it.id }

                    // Delete all notifications in parallel
                    val results = idsToDelete.map { id ->
                        async {
                            try {
                                RetrofitClient.apiService.deleteNotification("Bearer $token", id)
                                1
                            } catch (_: Exception) { 0 }
                        }
                    }.awaitAll()

                    val successCount = results.sum()
                    
                    if (successCount > 0) {
                        notificationList.removeAll { it.id in idsToDelete }
                        notifikasiAdapter.notifyDataSetChanged()
                        updateEmptyState()
                        clearNotificationCounter()
                        Toast.makeText(this@NotifikasiActivity, "$successCount notifikasi dihapus", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@NotifikasiActivity, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnTandaiSemua.setOnClickListener {
            if (notificationList.isEmpty()) return@setOnClickListener
            
            lifecycleScope.launch {
                try {
                    val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@NotifikasiActivity)
                        .getString("auth_token", "") ?: ""

                    val unreadIds = notificationList.filter { !it.isRead }.map { it.id }
                    
                    if (unreadIds.isNotEmpty()) {
                        RetrofitClient.apiService.bulkReadNotifications(
                            "Bearer $token",
                            BulkReadRequest(unreadIds)
                        )
                        
                        notificationList.forEach { it.isRead = true }
                        notifikasiAdapter.notifyDataSetChanged()
                        clearNotificationCounter()
                        Toast.makeText(this@NotifikasiActivity, "Semua notifikasi ditandai sebagai dibaca", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@NotifikasiActivity, "Gagal menandai: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        notifikasiAdapter = NotifikasiAdapter(
            notificationList,
            onItemClick = { notif, position ->
                if (!notif.isRead) {
                    markNotificationRead(notif, position)
                }
            },
            onDeleteClick = { notif, position ->
                deleteNotification(notif, position)
            }
        )

        binding.rvNotifikasi.apply {
            layoutManager = LinearLayoutManager(this@NotifikasiActivity)
            adapter = notifikasiAdapter
        }
    }
    
    private fun markNotificationRead(notif: Notifikasi, position: Int) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.markNotificationRead("Bearer $token", notif.id)
                notif.isRead = true
                notifikasiAdapter.notifyItemChanged(position)
            } catch (e: Exception) {
                // Silently fail — notification is still visible
            }
        }
    }
    
    private fun deleteNotification(notif: Notifikasi, position: Int) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteNotification("Bearer $token", notif.id)
                notificationList.removeAt(position)
                notifikasiAdapter.notifyItemRemoved(position)
                notifikasiAdapter.notifyItemRangeChanged(position, notificationList.size)
                updateEmptyState()
            } catch (e: Exception) {
                Toast.makeText(this@NotifikasiActivity, "Gagal menghapus notifikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNotifications() {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            try {
                val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@NotifikasiActivity)
                    .getString("auth_token", "") ?: ""

                val response = RetrofitClient.apiService.getNotifications(
                    "Bearer $token",
                    currentPage,
                    limit
                )

                isLoading = false

                if (response.isSuccessful && response.body() != null) {
                    val apiNotifications = response.body()!!.data
                    notificationList.clear()
                    
                    for (notif in apiNotifications) {
                        notificationList.add(
                            Notifikasi(
                                id = notif.id,
                                title = notif.title,
                                message = notif.message,
                                time = formatTime(notif.createdAt),
                                type = notif.type,
                                isRead = notif.isRead
                            )
                        )
                    }
                    
                    notifikasiAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } else {
                    Toast.makeText(this@NotifikasiActivity, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(this@NotifikasiActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatTime(isoString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(isoString)
            if (date != null) outputFormat.format(date) else isoString
        } catch (e: Exception) {
            isoString
        }
    }
}
