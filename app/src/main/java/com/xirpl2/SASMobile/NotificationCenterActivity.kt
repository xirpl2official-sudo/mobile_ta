package com.xirpl2.SASMobile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xirpl2.SASMobile.adapter.NotificationAdapter
import com.xirpl2.SASMobile.model.BulkReadRequest
import com.xirpl2.SASMobile.model.NotificationItem
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.utils.NotificationCounterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationCenterActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvMarkAllRead: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var adapter: NotificationAdapter

    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_center)

        val topBar = findViewById<View>(R.id.topBarContent)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        recycler = findViewById(R.id.recyclerNotifications)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        adapter = NotificationAdapter { item -> onNotificationClick(item) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadNotifications() }

        findViewById<View>(R.id.iconBack).setOnClickListener { finish() }
        tvMarkAllRead.setOnClickListener { markAllAsRead() }

        loadNotifications()
    }

    private fun loadNotifications() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getNotifications("Bearer $token", page = 1, limit = 50)
                if (response.isSuccessful) {
                    val body = response.body()
                    val items = body?.data ?: emptyList()

                    // Update last_seen so background worker won't re-show these
                    val maxId = items.maxOfOrNull { it.id } ?: 0
                    if (maxId > 0) {
                        getSharedPreferences("NotificationData", MODE_PRIVATE)
                            .edit().putInt("last_seen_notif_id", maxId).apply()
                    }

                    withContext(Dispatchers.Main) {
                        swipeRefresh.isRefreshing = false
                        adapter.submitList(items)

                        val hasUnread = items.any { !it.isRead }
                        tvMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.INVISIBLE
                        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        recycler.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        swipeRefresh.isRefreshing = false
                        tvEmpty.apply {
                            text = "Gagal memuat notifikasi"
                            visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("NotifCenter", "Failed to load notifications: ${e.message}")
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    tvEmpty.apply {
                        text = "Gagal memuat notifikasi"
                        visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun onNotificationClick(item: NotificationItem) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!item.isRead) {
                    RetrofitClient.apiService.markNotificationRead("Bearer $token", item.id)
                }
            } catch (e: Exception) {
                Log.w("NotifCenter", "Mark read failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                val updated = adapter.currentList.toMutableList()
                val idx = updated.indexOfFirst { it.id == item.id }
                if (idx >= 0) {
                    updated[idx] = item.copy(isRead = true)
                    adapter.submitList(updated)

                    val hasUnread = updated.any { !it.isRead }
                    tvMarkAllRead.visibility = if (hasUnread) View.VISIBLE else View.INVISIBLE
                }
                NotificationCounterManager.syncFromPreferences(this@NotificationCenterActivity)
            }
        }
    }

    private fun markAllAsRead() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        val unreadIds = adapter.currentList.filter { !it.isRead }.map { it.id }
        if (unreadIds.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.apiService.bulkReadNotifications("Bearer $token", BulkReadRequest(unreadIds))

                withContext(Dispatchers.Main) {
                    val updated = adapter.currentList.map { it.copy(isRead = true) }
                    adapter.submitList(updated)
                    tvMarkAllRead.visibility = View.INVISIBLE
                    NotificationCounterManager.clearCounter(this@NotificationCenterActivity)
                }
            } catch (e: Exception) {
                Log.w("NotifCenter", "Bulk read failed: ${e.message}")
            }
        }
    }

    private fun getAuthToken(): String {
        return com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""
    }
}
