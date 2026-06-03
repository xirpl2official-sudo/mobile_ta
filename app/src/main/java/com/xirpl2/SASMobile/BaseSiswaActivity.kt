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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BaseSiswaActivity provides sidebar navigation for student users.
 */
abstract class BaseSiswaActivity : BaseActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var sidebarView: View

    enum class SiswaMenuItem {
        BERANDA,
        QR_CODE,
        PENGAJUAN_IZIN
    }

    abstract fun getCurrentMenuItem(): SiswaMenuItem

    open fun getDrawerLayoutId(): Int = R.id.drawerLayout

    open fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle back press
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

            setupStudentProfile()
            setupCloseSidebarButton()
            setupMenuItems()

            // Account card → Pengaturan
            sidebarView.findViewById<androidx.cardview.widget.CardView>(R.id.cardUserProfile)?.setOnClickListener {
                closeSidebar()
                drawerLayout.postDelayed({
                    startActivity(Intent(this, PengaturanActivity::class.java))
                    overridePendingTransition(0, 0)
                }, 250)
            }
        } else {
            Log.e("BaseSiswaActivity", "Drawer or Sidebar view not found!")
        }
    }

    private fun setupStudentProfile() {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
        val nama = sharedPref.getString("nama_siswa", "Nama Siswa") ?: "Nama Siswa"
        val nis = sharedPref.getString("nis", "0000000000") ?: "0000000000"
        val kelas = sharedPref.getString("user_kelas", "") ?: ""

        val tvStudentName = sidebarView.findViewById<TextView>(R.id.tvStudentName)
        val tvStudentNIS = sidebarView.findViewById<TextView>(R.id.tvStudentNIS)
        val tvStudentClass = sidebarView.findViewById<TextView>(R.id.tvStudentClass)

        tvStudentName?.text = nama
        tvStudentNIS?.text = nis
        if (kelas.isNotBlank()) {
            tvStudentClass?.text = kelas
            tvStudentClass?.visibility = View.VISIBLE
        }
    }

    private fun setupCloseSidebarButton() {
        sidebarView.findViewById<ImageView>(R.id.btnCloseSidebar)?.setOnClickListener {
            closeSidebar()
        }
    }

    private fun setupMenuItems() {
        val currentItem = getCurrentMenuItem()

        setupMenuItem(R.id.menuBeranda, SiswaMenuItem.BERANDA, currentItem) {
            navigateTo(BerandaActivity::class.java)
        }

        setupMenuItem(R.id.menuQRCode, SiswaMenuItem.QR_CODE, currentItem) {
            navigateTo(ScanQrActivity::class.java)
        }

        setupMenuItem(R.id.menuPengajuanIzin, SiswaMenuItem.PENGAJUAN_IZIN, currentItem) {
            navigateTo(PengajuanIzinActivity::class.java)
        }
    }

    private fun setupMenuItem(
        menuId: Int,
        targetItem: SiswaMenuItem,
        currentItem: SiswaMenuItem,
        onClick: () -> Unit
    ) {
        val menuView = sidebarView.findViewById<LinearLayout>(menuId) ?: return
        val isActive = targetItem == currentItem

        if (isActive) {
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

    protected fun applyEdgeToEdge(view: View) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    protected fun setupStatusBar() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Ensure status bar icons are light (white)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }

    protected fun navigateTo(activityClass: Class<out BaseActivity>) {
        if (this::class.java == activityClass) {
            closeSidebar()
            return
        }

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
        try {
            if (::sidebarView.isInitialized) {
                sidebarView.findViewById<LinearLayout>(R.id.menuBeranda)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuPengajuanIzin)?.setOnClickListener(null)
                sidebarView.findViewById<LinearLayout>(R.id.menuQRCode)?.setOnClickListener(null)
                sidebarView.findViewById<androidx.cardview.widget.CardView>(R.id.cardUserProfile)?.setOnClickListener(null)
            }
            if (::drawerLayout.isInitialized) {
                drawerLayout.setDrawerListener(null)
            }
        } catch (e: Exception) {}
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
                    try {
                        if (token.isNotEmpty()) {
                            RetrofitClient.apiService.logout("Bearer $token")
                        }
                    } catch (e: Exception) {
                        Log.w("BaseSiswaActivity", "Logout API call failed: ${e.message}")
                    }
                    
                    launch(Dispatchers.Main) {
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@BaseSiswaActivity)
                            .edit().clear().apply()
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@BaseSiswaActivity)
                            .edit().clear().apply()
                        getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
                            .edit().clear().apply()

                        Toast.makeText(this@BaseSiswaActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@BaseSiswaActivity, MasukActivity::class.java)
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
