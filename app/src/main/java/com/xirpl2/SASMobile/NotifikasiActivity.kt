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
        if (notifikasiAdapter.currentList.isEmpty()) {
            binding.rvNotifikasi.visibility = android.view.View.GONE
            binding.emptyState.visibility = android.view.View.VISIBLE
        } else {
            binding.rvNotifikasi.visibility = android.view.View.VISIBLE
            binding.emptyState.visibility = android.view.View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnHapusSemua.setOnClickListener {
            val currentList = notifikasiAdapter.currentList
            if (currentList.isEmpty()) {
                finish()
                return@setOnClickListener
            }

            // Hapus semua via API
            lifecycleScope.launch {
                try {
                    val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@NotifikasiActivity)
                        .getString("auth_token", "") ?: ""

                    val idsToDelete = currentList.map { it.id }

                    // Delete all notifications in parallel, pairing each ID with its result
                    val results = idsToDelete.map { id ->
                        async {
                            try {
                                RetrofitClient.apiService.deleteNotification("Bearer $token", id)
                                id to true
                            } catch (_: Exception) { id to false }
                        }
                    }.awaitAll()

                    val successfulIds = results.filter { it.second }.map { it.first }.toSet()
                    val failedCount = idsToDelete.size - successfulIds.size

                    if (successfulIds.isNotEmpty()) {
                        notifikasiAdapter.submitList(currentList.filter { it.id !in successfulIds })
                        updateEmptyState()
                        clearNotificationCounter()
                        val msg = if (failedCount > 0) {
                            "${successfulIds.size} notifikasi dihapus, $failedCount gagal"
                        } else {
                            "${successfulIds.size} notifikasi dihapus"
                        }
                        Toast.makeText(this@NotifikasiActivity, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@NotifikasiActivity, "Gagal menghapus notifikasi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@NotifikasiActivity, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnTandaiSemua.setOnClickListener {
            val currentList = notifikasiAdapter.currentList
            if (currentList.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@NotifikasiActivity)
                        .getString("auth_token", "") ?: ""

                    val unreadIds = currentList.filter { !it.isRead }.map { it.id }

                    if (unreadIds.isNotEmpty()) {
                        RetrofitClient.apiService.bulkReadNotifications(
                            "Bearer $token",
                            BulkReadRequest(unreadIds)
                        )

                        notifikasiAdapter.submitList(currentList.map { it.copy(isRead = true) })
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
            onItemClick = { notif ->
                if (!notif.isRead) {
                    markNotificationRead(notif)
                }
            },
            onDeleteClick = { notif ->
                deleteNotification(notif)
            }
        )

        binding.rvNotifikasi.apply {
            layoutManager = LinearLayoutManager(this@NotifikasiActivity)
            adapter = notifikasiAdapter
        }
    }
    
    private fun markNotificationRead(notif: Notifikasi) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.markNotificationRead("Bearer $token", notif.id)
                val updated = notif.copy(isRead = true)
                notifikasiAdapter.submitList(notifikasiAdapter.currentList.map {
                    if (it.id == notif.id) updated else it
                })
            } catch (e: Exception) {
                // Silently fail — notification is still visible
            }
        }
    }
    
    private fun deleteNotification(notif: Notifikasi) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.deleteNotification("Bearer $token", notif.id)
                notifikasiAdapter.submitList(notifikasiAdapter.currentList.filter { it.id != notif.id })
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
                    val newList = apiNotifications.map { notif ->
                        Notifikasi(
                            id = notif.id,
                            title = notif.title,
                            message = notif.message,
                            time = formatTime(notif.createdAt),
                            type = notif.type,
                            isRead = notif.isRead
                        )
                    }

                    notifikasiAdapter.submitList(newList)
                    updateEmptyState()
                    if (newList.none { !it.isRead }) {
                        clearNotificationCounter()
                    }
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
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = parseIsoDate(isoString)
            if (date != null) outputFormat.format(date) else isoString
        } catch (e: Exception) {
            isoString
        }
    }

    private fun parseIsoDate(isoString: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(isoString)
            } catch (_: Exception) { }
        }
        return null
    }
}
