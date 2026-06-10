package com.xirpl2.SASMobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.utils.NotificationCounterManager
import com.xirpl2.SASMobile.utils.NotificationPollWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudentMainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_main)
        setupStatusBar()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setupWithNavController(navController)

        applyBottomNavColors(bottomNav)

        navController.addOnDestinationChangedListener { _, dest, _ -> }
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController

        if (navController != null && navController.currentDestination?.id != R.id.navigation_beranda) {
            navController.navigate(R.id.navigation_beranda)
        } else {
            moveTaskToBack(true)
        }
    }

    private fun applyBottomNavColors(bottomNav: BottomNavigationView) {
        val white = Color.WHITE
        val blueActive = ContextCompat.getColor(this, R.color.blue_theme)
        val slateInactive = ContextCompat.getColor(this, R.color.on_background)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        bottomNav.itemIconTintList = android.content.res.ColorStateList(states, intArrayOf(white, slateInactive))
        bottomNav.itemTextColor = android.content.res.ColorStateList(states, intArrayOf(blueActive, slateInactive))
    }

    fun handleLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                val token = getAuthTokenSiswa()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        if (token.isNotEmpty()) {
                            RetrofitClient.apiService.logout("Bearer $token")
                        }
                    } catch (e: Exception) {
                        Log.w("StudentMain", "Logout API call failed: ${e.message}")
                    }

                    launch(Dispatchers.Main) {
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@StudentMainActivity)
                            .edit().clear().apply()
                        com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@StudentMainActivity)
                            .edit().clear().apply()
                        getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        NotificationCounterManager.clearCounter(this@StudentMainActivity)
                        WorkManager.getInstance(this@StudentMainActivity)
                            .cancelUniqueWork(NotificationPollWorker.WORK_NAME)

                        Toast.makeText(this@StudentMainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@StudentMainActivity, MasukActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    fun getAuthTokenSiswa(): String {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", null)
        if (!token.isNullOrEmpty()) return token
        val sessionToken = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
            .getString("auth_token", null)
        if (!sessionToken.isNullOrEmpty()) return sessionToken
        val plainToken = getSharedPreferences("UserData", Context.MODE_PRIVATE)
            .getString("auth_token", null)
        return plainToken ?: ""
    }

    private fun setupStatusBar() {
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
    }
}
