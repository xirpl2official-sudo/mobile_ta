package com.xirpl2.SASMobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.utils.NotificationCounterManager
import com.xirpl2.SASMobile.utils.NotificationPollWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BaseAdminActivity inherits from BaseActivity to provide role-specific sidebar navigation
 * and administrative layout features while maintaining core system stability.
 */
abstract class BaseAdminActivity : BaseActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var sidebarView: View

    enum class AdminMenuItem {
        BERANDA,
        JADWAL_SHOLAT,
        DATA_SISWA,
        KELOLA_KELAS,
        KELOLA_GURU,
        PRESENSI,
        PENGAJUAN_IZIN,
        LAPORAN,
        QR_CODE,
        SISWA_BELUM_TERDAFTAR,
        MANAJEMEN_PERANGKAT,
        KENAIKAN_KELAS,
        PENGATURAN,
        LOGOUT
    }

    abstract fun getCurrentMenuItem(): AdminMenuItem

    open fun getDrawerLayoutId(): Int = R.id.drawerLayout

    open fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle back press with modern API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    closeSidebar()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    overridePendingTransition(0, 0)
                }
            }
        })
    }

    protected fun setupDrawerAndSidebar() {
        val drawer = findViewById<DrawerLayout>(getDrawerLayoutId())
        val sidebar = findViewById<View>(getNavigationViewId())
        
        if (drawer != null && sidebar != null) {
            drawerLayout = drawer
            sidebarView = sidebar

            val sidebarHeader = sidebar.findViewById<View>(R.id.sidebarHeader)
            if (sidebarHeader != null) {
                applyEdgeToEdge(sidebarHeader)
            }

            setupAdminProfile()
            setupCloseSidebarButton()
            applyRoleBasedFiltering()
            setupMenuItems()

            // Account card → Pengaturan
            sidebarView.findViewById<CardView>(R.id.cardUserProfile)?.setOnClickListener {
                closeSidebar()
                drawerLayout.postDelayed({
                    startActivity(Intent(this, PengaturanActivity::class.java))
                    overridePendingTransition(0, 0)
                }, 250)
            }
        } else {
            android.util.Log.e("BaseAdminActivity", "Drawer or Sidebar view not found!")
        }
    }

    private fun applyRoleBasedFiltering() {
        val role = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
            .getString("user_role", "")?.lowercase() ?: ""

        val isWali = role.contains("wali")
        val isAdmin = role.contains("admin")
        val isGuru = role == "guru"

        // Feature deprecated: hide Kenaikan Kelas for all roles
        sidebarView.findViewById<View>(R.id.menuKenaikanKelas)?.visibility = View.GONE

        if (!isAdmin) {
            // Non-admin: hide admin-only items
            sidebarView.findViewById<View>(R.id.menuKelolaKelas)?.visibility = View.GONE
            sidebarView.findViewById<View>(R.id.menuKelolaGuru)?.visibility = View.GONE
            sidebarView.findViewById<View>(R.id.menuQRCode)?.visibility = View.GONE
            sidebarView.findViewById<View>(R.id.menuManajemenPerangkat)?.visibility = View.GONE

            if (isWali) {
                sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.VISIBLE
                sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.VISIBLE
            } else if (isGuru) {
                sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.VISIBLE
                sidebarView.findViewById<View>(R.id.menuPresensi)?.visibility = View.VISIBLE
                sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.GONE
                sidebarView.findViewById<View>(R.id.menuSiswaBelumTerdaftar)?.visibility = View.GONE
            } else {
                sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.GONE
                sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.GONE
                sidebarView.findViewById<View>(R.id.menuPresensi)?.visibility = View.GONE
            }
        }
    }

    private fun setupAdminProfile() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            try {
                val response = com.xirpl2.SASMobile.network.RetrofitClient.apiService.getProfile("Bearer $token")
                if (response.isSuccessful) {
                    val authData = response.body()?.data
                    if (authData != null && !isFinishing && !isDestroyed) {
                        runOnUiThread {
                            val tvAdminName = sidebarView.findViewById<TextView>(R.id.tvAdminName)
                            val tvAdminRole = sidebarView.findViewById<TextView>(R.id.tvAdminRole)
                            
                            tvAdminName?.text = authData.getDisplayName()
                            
                            val roleRaw = authData.role?.lowercase() ?: ""
                            tvAdminRole?.text = when {
                                roleRaw.contains("admin") -> "Administrator"
                                roleRaw.contains("wali") -> "Wali Kelas"
                                roleRaw.contains("guru") -> "Guru"
                                else -> "Staff"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("BaseAdminActivity", "Failed to load admin profile: ${e.message}")
            }
        }
    }

    private fun setupCloseSidebarButton() {
        sidebarView.findViewById<ImageView>(R.id.btnCloseSidebar)?.setOnClickListener {
            closeSidebar()
        }
    }

    private fun setupMenuItems() {
        val currentItem = getCurrentMenuItem()

        
        setupMenuItem(R.id.menuBeranda, AdminMenuItem.BERANDA, currentItem) {
            val role = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
                .getString("user_role", "")?.lowercase() ?: ""

            val targetClass = if (role.contains("wali") || role == "guru") {
                BerandaGuruActivity::class.java
            } else {
                BerandaAdminActivity::class.java
            }
            navigateTo(targetClass)
        }

        
        setupMenuItem(R.id.menuJadwalSholat, AdminMenuItem.JADWAL_SHOLAT, currentItem) {
            navigateTo(JadwalSholatAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuDataSiswa, AdminMenuItem.DATA_SISWA, currentItem) {
            navigateTo(DataSiswaAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuKelolaKelas, AdminMenuItem.KELOLA_KELAS, currentItem) {
            navigateTo(KelolaKelasActivity::class.java)
        }

        setupMenuItem(R.id.menuKelolaGuru, AdminMenuItem.KELOLA_GURU, currentItem) {
            navigateTo(KelolaGuruAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuPresensi, AdminMenuItem.PRESENSI, currentItem) {
            navigateTo(PresensiSholatAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuPengajuanIzin, AdminMenuItem.PENGAJUAN_IZIN, currentItem) {
            navigateTo(PengajuanIzinAdminActivity::class.java)
        }

        setupMenuItem(R.id.menuLaporan, AdminMenuItem.LAPORAN, currentItem) {
            navigateTo(LaporanAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuQRCode, AdminMenuItem.QR_CODE, currentItem) {
            navigateTo(QRCodeAdminActivity::class.java)
        }

        setupMenuItem(R.id.menuSiswaBelumTerdaftar, AdminMenuItem.SISWA_BELUM_TERDAFTAR, currentItem) {
            navigateTo(SiswaBelumTerdaftarAdminActivity::class.java)
        }

        setupMenuItem(R.id.menuManajemenPerangkat, AdminMenuItem.MANAJEMEN_PERANGKAT, currentItem) {
            navigateTo(AdminDeviceManagementActivity::class.java)
        }

        setupMenuItem(R.id.menuKenaikanKelas, AdminMenuItem.KENAIKAN_KELAS, currentItem) {
            navigateTo(StudentPromotionActivity::class.java)
        }

        sidebarView.findViewById<LinearLayout>(R.id.menuFAQ)?.setOnClickListener {
            closeSidebar()
            drawerLayout.postDelayed({
                startActivity(Intent(this, FAQActivity::class.java))
                overridePendingTransition(0, 0)
            }, 250)
        }

        sidebarView.findViewById<LinearLayout>(R.id.menuPengaturan)?.setOnClickListener {
            closeSidebar()
            drawerLayout.postDelayed({
                startActivity(Intent(this, PengaturanActivity::class.java))
                overridePendingTransition(0, 0)
            }, 250)
        }

        
        sidebarView.findViewById<LinearLayout>(R.id.menuLogout)?.setOnClickListener {
            closeSidebar()
            handleLogout()
        }
    }

    private fun setupMenuItem(
        menuId: Int,
        targetItem: AdminMenuItem,
        currentItem: AdminMenuItem,
        onClick: () -> Unit
    ) {
        val menuView = sidebarView.findViewById<LinearLayout>(menuId) ?: return
        val isActive = targetItem == currentItem

        if (isActive) {
            // Active item: blue theme background, white text/icon
            menuView.setBackgroundResource(R.drawable.bg_sidebar_item_active)
            menuView.backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme))
            
            for (i in 0 until menuView.childCount) {
                val child = menuView.getChildAt(i)
                if (child is ImageView) {
                    child.setColorFilter(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
                } else if (child is TextView) {
                    child.setTextColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.white))
                    child.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }

        menuView.setOnClickListener {
            if (isActive) {
                closeSidebar()
            } else {
                onClick()
            }
        }
    }

    protected fun setupStatusBar() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Ensure status bar icons are light (white)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    protected fun applyEdgeToEdge(view: View) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    protected fun navigateTo(activityClass: Class<out BaseAdminActivity>) {
        if (this::class.java == activityClass) {
            closeSidebar()
            return
        }

        // Use UniversalSafeNavigator for cross-platform stability
        com.xirpl2.SASMobile.utils.UniversalSafeNavigator.navigateTo(
            this,
            activityClass,
            finishCurrent = true,
            intentModifier = { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        
        closeSidebar()
    }

    override fun onDestroy() {
        // Prevent Memory Leaks and excessive Binder transactions
        try {
            if (::sidebarView.isInitialized) {
                // Clear all click listeners to release callback references
                sidebarView.findViewById<LinearLayout>(R.id.menuBeranda)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuJadwalSholat)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuDataSiswa)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuKelolaKelas)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuKelolaGuru)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuPresensi)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuPengajuanIzin)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuLaporan)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuQRCode)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuSiswaBelumTerdaftar)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuManajemenPerangkat)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuKenaikanKelas)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuFAQ)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuPengaturan)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuLogout)?.setOnClickListener(null)
                sidebarView.findViewById<CardView>(R.id.cardUserProfile)?.setOnClickListener(null)
            }
            
            if (::drawerLayout.isInitialized) {
                drawerLayout.setDrawerListener(null)
            }
        } catch (e: Exception) {
            // Safe destruction
        }
        
        super.onDestroy()
    }

    protected fun openSidebar() {
        if (::drawerLayout.isInitialized) {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    protected fun closeSidebar() {
        if (::drawerLayout.isInitialized) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    protected fun setupMenuIcon() {
        findViewById<ImageView>(R.id.iconMenu)?.setOnClickListener {
            openSidebar()
        }
    }

    protected fun handleLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                val token = getAuthToken()
                
                lifecycleScope.launch(Dispatchers.IO) {
                    // Call logout API to invalidate token on server
                    try {
                        if (token.isNotEmpty()) {
                            RetrofitClient.apiService.logout("Bearer $token")
                        }
                    } catch (e: Exception) {
                        Log.w("BaseAdminActivity", "Logout API call failed (non-critical): ${e.message}")
                    }
                    
                    launch(Dispatchers.Main) {
                        // Clear ALL SharedPreferences stores
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@BaseAdminActivity)
                            .edit().clear().apply()
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@BaseAdminActivity)
                            .edit().clear().apply()
                        getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        NotificationCounterManager.clearCounter(this@BaseAdminActivity)
                        WorkManager.getInstance(this@BaseAdminActivity)
                            .cancelUniqueWork(NotificationPollWorker.WORK_NAME)

                        Toast.makeText(this@BaseAdminActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@BaseAdminActivity, MasukActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    protected fun getAuthToken(): String {
        return com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
