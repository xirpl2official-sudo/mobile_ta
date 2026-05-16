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

abstract class BaseAdminActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var sidebarView: View

    enum class AdminMenuItem {
        BERANDA,
        JADWAL_SHOLAT,
        DATA_SISWA,
        KELOLA_SISWA,
        KELOLA_KELAS,
        PRESENSI,
        PENGAJUAN_IZIN,
        LAPORAN,
        QR_CODE,
        SISWA_BELUM_TERDAFTAR,
        PENGATURAN,
        LOGOUT
    }

    abstract fun getCurrentMenuItem(): AdminMenuItem

    open fun getDrawerLayoutId(): Int = R.id.drawerLayout

    open fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    protected fun setupDrawerAndSidebar() {
        val drawer = findViewById<DrawerLayout>(getDrawerLayoutId())
        val sidebar = findViewById<View>(getNavigationViewId())
        
        if (drawer != null && sidebar != null) {
            drawerLayout = drawer
            sidebarView = sidebar

            setupAdminProfile()
            setupCloseSidebarButton()
            applyRoleBasedFiltering()
            setupMenuItems()
        } else {
            android.util.Log.e("BaseAdminActivity", "Drawer or Sidebar view not found!")
        }
    }

    private fun applyRoleBasedFiltering() {
        val role = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getString("user_role", "")?.lowercase() ?: ""
            
        val isWali = role.contains("wali")
        val isAdmin = role.contains("admin")
        val isGuru = role == "guru"

        if (!isAdmin) {
            
            
            if (isWali) {
                sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.VISIBLE
                sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.VISIBLE
            } else {
                sidebarView.findViewById<View>(R.id.menuJadwalSholat)?.visibility = View.GONE
                sidebarView.findViewById<View>(R.id.menuDataSiswa)?.visibility = View.GONE
            }
            
            if (isGuru && !isWali) {
                
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
                    val profile = response.body()?.data
                    runOnUiThread {
                        val tvAdminName = sidebarView.findViewById<TextView>(R.id.tvAdminName)
                        val tvAdminRole = sidebarView.findViewById<TextView>(R.id.tvAdminRole)
                        
                        tvAdminName?.text = profile?.nama_siswa ?: profile?.nama ?: "Admin"
                        
                        
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
            val role = getSharedPreferences("user_session", Context.MODE_PRIVATE)
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

        
        setupMenuItem(R.id.menuKelolaSiswa, AdminMenuItem.KELOLA_SISWA, currentItem) {
            
        }

        
        setupMenuItem(R.id.menuKelolaKelas, AdminMenuItem.KELOLA_KELAS, currentItem) {
            navigateTo(KelolaKelasActivity::class.java)
        }

        
        setupMenuItem(R.id.menuPresensi, AdminMenuItem.PRESENSI, currentItem) {
            navigateTo(PresensiSholatAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuPengajuanIzin, AdminMenuItem.PENGAJUAN_IZIN, currentItem) {
            
        }

        
        setupMenuItem(R.id.menuLaporan, AdminMenuItem.LAPORAN, currentItem) {
            navigateTo(LaporanAdminActivity::class.java)
        }

        
        setupMenuItem(R.id.menuQRCode, AdminMenuItem.QR_CODE, currentItem) {
            
        }

        setupMenuItem(R.id.menuSiswaBelumTerdaftar, AdminMenuItem.SISWA_BELUM_TERDAFTAR, currentItem) {
            
        }

        
        sidebarView.findViewById<LinearLayout>(R.id.menuPengaturan)?.setOnClickListener {
            closeSidebar()
            startActivity(Intent(this, PengaturanAkunActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
            
            menuView.setBackgroundColor(0xFF2886D6.toInt()) 
            
            
            for (i in 0 until menuView.childCount) {
                val child = menuView.getChildAt(i)
                if (child is ImageView) {
                    child.setColorFilter(0xFFFFFFFF.toInt())
                } else if (child is TextView) {
                    child.setTextColor(0xFFFFFFFF.toInt())
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

    protected fun navigateTo(activityClass: Class<out BaseAdminActivity>) {
        if (this::class.java == activityClass) {
            closeSidebar()
            return
        }

        if (!::drawerLayout.isInitialized || isFinishing || isDestroyed) {
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, activityClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            return
        }

        try {
            drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    try {
                        drawerLayout.removeDrawerListener(this)

                        if (isFinishing || isDestroyed) return

                        val intent = Intent(this@BaseAdminActivity, activityClass)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

                        window.decorView.postDelayed({
                            try {
                                if (!isFinishing && !isDestroyed) {
                                    finish()
                                }
                            } catch (e: android.os.DeadObjectException) {
                                android.util.Log.e("BaseAdminActivity", "DeadObjectException during finish: ${e.message}")
                            } catch (e: Exception) {
                                android.util.Log.e("BaseAdminActivity", "Error during delayed finish: ${e.message}")
                            }
                        }, 50)
                    } catch (e: android.os.DeadObjectException) {
                        android.util.Log.e("BaseAdminActivity", "DeadObjectException in onDrawerClosed: ${e.message}")
                    } catch (e: Exception) {
                        android.util.Log.e("BaseAdminActivity", "Error in onDrawerClosed: ${e.message}")
                    }
                }

                override fun onDrawerOpened(drawerView: View) {}
            })
            closeSidebar()
        } catch (e: android.os.DeadObjectException) {
            android.util.Log.e("BaseAdminActivity", "DeadObjectException during navigation: ${e.message}")
            try {
                val intent = Intent(this, activityClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("BaseAdminActivity", "Fallback navigation failed: ${e2.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("BaseAdminActivity", "Error during navigation: ${e.message}")
            try {
                val intent = Intent(this, activityClass)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            } catch (e2: Exception) {
                android.util.Log.e("BaseAdminActivity", "Fallback navigation failed: ${e2.message}")
            }
        }
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
                
                getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    .edit().clear().apply()

                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

                
                val intent = Intent(this, MasukActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    protected fun setupStatusBar() {
        window.statusBarColor = 0xFF000000.toInt()
    }

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
