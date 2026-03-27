package com.xirpl2.SASMobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Base Activity for Admin screens that provides shared sidebar functionality.
 * Eliminates code duplication and ensures consistent navigation experience.
 */
abstract class BaseAdminActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var sidebarView: View

    /**
     * Enum representing sidebar menu items
     */
    enum class AdminMenuItem {
        BERANDA,
        JADWAL_SHOLAT,
        DATA_SISWA,
        PRESENSI,
        LAPORAN,
        PENGATURAN,
        LOGOUT
    }

    /**
     * Override this to specify which menu item is currently active
     */
    abstract fun getCurrentMenuItem(): AdminMenuItem

    /**
     * Get the DrawerLayout resource ID for this activity
     */
    open fun getDrawerLayoutId(): Int = R.id.drawerLayout

    /**
     * Get the navigation view (sidebar) resource ID for this activity
     */
    open fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply enter animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Setup the drawer and sidebar with all menu handlers.
     * Call this in your Activity's onCreate after setContentView.
     */
    protected fun setupDrawerAndSidebar() {
        drawerLayout = findViewById(getDrawerLayoutId())
        sidebarView = findViewById(getNavigationViewId())

        setupAdminProfile()
        setupCloseSidebarButton()
        applyRoleBasedFiltering()
        setupMenuItems()
    }

    /**
     * Filter sidebar menu items based on user role.
     * Admin: All items
     * Wali Kelas: Beranda, Presensi, Laporan
     * Guru: Beranda, Laporan
     */
    private fun applyRoleBasedFiltering() {
        val role = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_role", "")?.lowercase() ?: ""
            
        val isWali = role.contains("wali")
        val isGuru = role == "guru"
        val isAdmin = role.contains("admin")

        if (!isAdmin) {
            // Hide administrative modules for non-admins
            sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.GONE
            sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.GONE
            
            if (isGuru && !isWali) {
                // Guru cannot even access Presensi (input)
                sidebarView.findViewById<View>(R.id.menuPresensi)?.visibility = View.GONE
            }
        }
    }

    /**
     * Setup admin profile display in sidebar header
     * Loads username dynamically from user_staf via auth/me API
     */
    private fun setupAdminProfile() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            try {
                val response = com.xirpl2.SASMobile.network.RetrofitClient.apiService.getProfile("Bearer $token")
                if (response.isSuccessful) {
                    val profile = response.body()?.data
                    runOnUiThread {
                        val tvAdminName = sidebarView.findViewById<TextView>(R.id.tvAdminName)
                        val tvAdminRole = sidebarView.findViewById<TextView>(R.id.tvAdminRole)
                        
                        tvAdminName?.text = profile?.nama_siswa ?: profile?.name ?: profile?.username ?: "Admin"
                        
                        // Map role to friendly name
                        val roleRaw = profile?.role?.lowercase() ?: ""
                        tvAdminRole?.text = when {
                            roleRaw.contains("admin") -> "Administrator"
                            roleRaw.contains("wali") -> "Wali Kelas"
                            roleRaw.contains("guru") -> "Guru"
                            else -> "Staff"
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - admin name will show default
            }
        }
    }

    /**
     * Setup close sidebar button
     */
    private fun setupCloseSidebarButton() {
        sidebarView.findViewById<ImageView>(R.id.btnCloseSidebar)?.setOnClickListener {
            closeSidebar()
        }
    }

    /**
     * Setup all menu item click handlers with proper highlighting
     */
    private fun setupMenuItems() {
        val currentItem = getCurrentMenuItem()

        // Beranda menu - may be CardView (active) or wrapped in CardView
        setupMenuItem(
            menuId = R.id.menuBeranda,
            targetItem = AdminMenuItem.BERANDA,
            currentItem = currentItem,
            isCardView = currentItem == AdminMenuItem.BERANDA
        ) {
            val role = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                .getString("user_role", "")?.lowercase() ?: ""
                
            val targetClass = if (role.contains("wali") || role == "guru") {
                BerandaGuruActivity::class.java
            } else {
                BerandaAdminActivity::class.java
            }
            navigateTo(targetClass)
        }

        // Jadwal Sholat menu
        setupMenuItem(
            menuId = R.id.menuJadwalSholat,
            targetItem = AdminMenuItem.JADWAL_SHOLAT,
            currentItem = currentItem,
            isCardView = currentItem == AdminMenuItem.JADWAL_SHOLAT
        ) {
            navigateTo(JadwalSholatAdminActivity::class.java)
        }

        // Data Siswa menu
        setupMenuItem(
            menuId = R.id.menuDataSiswa,
            targetItem = AdminMenuItem.DATA_SISWA,
            currentItem = currentItem,
            isCardView = currentItem == AdminMenuItem.DATA_SISWA
        ) {
            navigateTo(DataSiswaAdminActivity::class.java)
        }

        // Presensi menu
        setupMenuItem(
            menuId = R.id.menuPresensi,
            targetItem = AdminMenuItem.PRESENSI,
            currentItem = currentItem,
            isCardView = currentItem == AdminMenuItem.PRESENSI
        ) {
            navigateTo(PresensiSholatAdminActivity::class.java)
        }

        // Laporan menu
        setupMenuItem(
            menuId = R.id.menuLaporan,
            targetItem = AdminMenuItem.LAPORAN,
            currentItem = currentItem,
            isCardView = currentItem == AdminMenuItem.LAPORAN
        ) {
            navigateTo(LaporanAdminActivity::class.java)
        }

        // Pengaturan menu
        sidebarView.findViewById<LinearLayout>(R.id.menuPengaturan)?.setOnClickListener {
            closeSidebar()
            startActivity(Intent(this, PengaturanAkunActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Logout menu
        sidebarView.findViewById<LinearLayout>(R.id.menuLogout)?.setOnClickListener {
            closeSidebar()
            handleLogout()
        }
    }

    /**
     * Setup individual menu item with proper click handling
     */
    private fun setupMenuItem(
        menuId: Int,
        targetItem: AdminMenuItem,
        currentItem: AdminMenuItem,
        isCardView: Boolean,
        onClick: () -> Unit
    ) {
        val menuView: View? = if (isCardView) {
            sidebarView.findViewById<CardView>(menuId)
        } else {
            sidebarView.findViewById<LinearLayout>(menuId)
        }

        menuView?.setOnClickListener {
            if (targetItem == currentItem) {
                // Already on this screen, just close drawer
                closeSidebar()
            } else {
                onClick()
            }
        }
    }

    /**
     * Navigate to another admin activity with smooth animation
     */
    protected fun navigateTo(activityClass: Class<out BaseAdminActivity>) {
        // Close drawer first for smoother experience
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                drawerLayout.removeDrawerListener(this)
                
                val intent = Intent(this@BaseAdminActivity, activityClass)
                // Use FLAG_ACTIVITY_REORDER_TO_FRONT to reuse existing instance if available
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                
                // Only finish if not going back to an existing instance
                if (activityClass != this@BaseAdminActivity::class.java) {
                    finish()
                }
            }
        })
        closeSidebar()
    }

    /**
     * Open the sidebar drawer
     */
    protected fun openSidebar() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    /**
     * Close the sidebar drawer
     */
    protected fun closeSidebar() {
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    /**
     * Setup menu icon (hamburger) to open sidebar
     * Call this with your menu icon view
     */
    protected fun setupMenuIcon() {
        findViewById<ImageView>(R.id.iconMenu)?.setOnClickListener {
            openSidebar()
        }
    }

    /**
     * Handle logout with confirmation dialog
     */
    protected fun handleLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Clear auth token and user data
                getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    .edit().clear().apply()

                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

                // Navigate back to Login Activity
                val intent = Intent(this, MasukActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    /**
     * Configure common status bar styling for admin screens
     */
    protected fun setupStatusBar() {
        window.statusBarColor = 0xFF000000.toInt()
    }

    /**
     * Get auth token from SharedPreferences
     */
    protected fun getAuthToken(): String {
        return getSharedPreferences("UserData", Context.MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeSidebar()
        } else {
            super.onBackPressed()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
