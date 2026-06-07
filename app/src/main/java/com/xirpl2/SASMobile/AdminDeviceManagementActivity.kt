package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.xirpl2.SASMobile.adapter.DeviceAdapter
import com.xirpl2.SASMobile.adapter.DeviceListItem
import com.xirpl2.SASMobile.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdminDeviceManagementActivity : BaseAdminActivity() {

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.MANAJEMEN_PERANGKAT

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var adapter: DeviceAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etSearch: EditText
    private val repository = DeviceRepository()
    private var currentTab = 0
    private var searchQuery = ""
    private var allDeviceItems = listOf<DeviceListItem>()
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_device_management)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupTabs()
        
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvDevices = findViewById(R.id.rvDevices)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        emptyStateContainer = findViewById(R.id.emptyState)
        etSearch = findViewById(R.id.etSearch)

        toolbar.setNavigationOnClickListener { openSidebar() }
        setupSearch()
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    searchQuery = s?.toString()?.trim() ?: ""
                    applySearchFilter()
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })
    }

    private fun applySearchFilter() {
        val filtered = if (searchQuery.isEmpty()) {
            allDeviceItems
        } else {
            allDeviceItems.filter { item ->
                when (item) {
                    is DeviceListItem.Device -> {
                        item.item.hardware_id.contains(searchQuery, ignoreCase = true) ||
                        item.item.device_name.orEmpty().contains(searchQuery, ignoreCase = true) ||
                        item.item.device_model.orEmpty().contains(searchQuery, ignoreCase = true)
                    }
                    is DeviceListItem.ChangeRequest -> {
                        item.item.hardware_id_lama.contains(searchQuery, ignoreCase = true) ||
                        item.item.hardware_id_baru.contains(searchQuery, ignoreCase = true) ||
                        item.item.reason.orEmpty().contains(searchQuery, ignoreCase = true)
                    }
                }
            }
        }
        adapter.updateData(filtered)
        emptyStateContainer.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter { item, action ->
            processRequest(item.item.id, action)
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        showLoading(true)
        lifecycleScope.launch {
            if (currentTab == 0) {
                repository.getAdminDeviceManagement(token).fold(
                    onSuccess = { devices ->
                        if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                        runOnUiThread {
                            showLoading(false)
                            allDeviceItems = devices.map { DeviceListItem.Device(it) }
                            applySearchFilter()
                        }
                    },
                    onFailure = { error ->
                        if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                        runOnUiThread {
                            showLoading(false)
                            Log.e("AdminDeviceManagement", "Request failed", error)
                            Toast.makeText(this@AdminDeviceManagementActivity, "Terjadi kesalahan, silakan coba lagi", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                repository.getDeviceChangeRequests(token).fold(
                    onSuccess = { requests ->
                        if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                        runOnUiThread {
                            showLoading(false)
                            allDeviceItems = requests.map { DeviceListItem.ChangeRequest(it) }
                            applySearchFilter()
                        }
                    },
                    onFailure = { error ->
                        if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                        runOnUiThread {
                            showLoading(false)
                            Log.e("AdminDeviceManagement", "Request failed", error)
                            Toast.makeText(this@AdminDeviceManagementActivity, "Terjadi kesalahan, silakan coba lagi", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    private fun processRequest(id: Int, action: String) {
        val actionLabel = when (action) {
            "approve" -> "menyetujui"
            "reject" -> "menolak"
            else -> action
        }
        val changeRequestItem = allDeviceItems.filterIsInstance<DeviceListItem.ChangeRequest>()
            .firstOrNull { it.item.id == id }
        val reason = changeRequestItem?.item?.reason ?: "-"
        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi")
            .setMessage("Alasan: $reason\n\nApakah Anda yakin ingin $actionLabel permintaan perubahan perangkat ini?")
            .setPositiveButton("Ya") { _, _ ->
                executeProcessRequest(id, action)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeProcessRequest(id: Int, action: String) {
        val token = getAuthToken()
        showLoading(true)
        lifecycleScope.launch {
            repository.processDeviceChangeRequest(token, id, action).fold(
                onSuccess = { message ->
                    runOnUiThread {
                        Toast.makeText(this@AdminDeviceManagementActivity, message, Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        showLoading(false)
                        Log.e("AdminDeviceManagement", "Process request failed", error)
                        Toast.makeText(this@AdminDeviceManagementActivity, "Terjadi kesalahan, silakan coba lagi", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvDevices.visibility = if (isLoading) View.GONE else View.VISIBLE
        if (isLoading) emptyStateContainer.visibility = View.GONE
    }
}
