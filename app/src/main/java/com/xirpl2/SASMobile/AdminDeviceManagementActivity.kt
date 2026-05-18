package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.xirpl2.SASMobile.adapter.DeviceAdapter
import com.xirpl2.SASMobile.model.DeviceChangeRequestItem
import com.xirpl2.SASMobile.repository.DeviceRepository
import kotlinx.coroutines.launch

class AdminDeviceManagementActivity : BaseAdminActivity() {

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.MANAJEMEN_PERANGKAT

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var rvDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: DeviceAdapter

    private val repository = DeviceRepository()
    private var currentTab = 0

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
        
        loadData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        rvDevices = findViewById(R.id.rvDevices)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        toolbar.setNavigationOnClickListener { openSidebar() }
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter(emptyList()) { item, action ->
            if (item is DeviceChangeRequestItem) {
                processRequest(item.id, action)
            }
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
                        runOnUiThread {
                            showLoading(false)
                            adapter.updateData(devices)
                            tvEmpty.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            showLoading(false)
                            Toast.makeText(this@AdminDeviceManagementActivity, error.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                repository.getDeviceChangeRequests(token).fold(
                    onSuccess = { requests ->
                        runOnUiThread {
                            showLoading(false)
                            adapter.updateData(requests)
                            tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            showLoading(false)
                            Toast.makeText(this@AdminDeviceManagementActivity, error.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    private fun processRequest(id: Int, action: String) {
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
                        Toast.makeText(this@AdminDeviceManagementActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        rvDevices.visibility = if (isLoading) View.GONE else View.VISIBLE
        if (isLoading) tvEmpty.visibility = View.GONE
    }
}
