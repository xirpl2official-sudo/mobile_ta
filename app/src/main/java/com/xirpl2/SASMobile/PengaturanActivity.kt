package com.xirpl2.SASMobile

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.utils.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengaturanActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var cardAccount: CardView
    private lateinit var tvAccountName: TextView
    private lateinit var tvAccountRole: TextView
    private lateinit var tvAccountEmail: TextView
    private lateinit var tvInitial: TextView
    private lateinit var btnTerang: MaterialButton
    private lateinit var btnGelap: MaterialButton
    private lateinit var btnSistem: MaterialButton

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val KEY_THEME = "theme_mode"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge + transparent status bar (matching admin pages)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false // white icons on blue header
        }

        setContentView(R.layout.activity_pengaturan)

        // Apply top padding to header for status bar inset
        val headerContent = findViewById<View>(R.id.headerContent)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(headerContent) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBar.top, v.paddingRight, v.paddingBottom)
            insets
        }

        initializeViews()
        loadAccountInfo()
        setupThemeButtons()
        setupListeners()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        cardAccount = findViewById(R.id.cardAccount)
        tvAccountName = findViewById(R.id.tvAccountName)
        tvAccountRole = findViewById(R.id.tvAccountRole)
        tvAccountEmail = findViewById(R.id.tvAccountEmail)
        tvInitial = findViewById(R.id.tvInitial)
        btnTerang = findViewById(R.id.btnTerang)
        btnGelap = findViewById(R.id.btnGelap)
        btnSistem = findViewById(R.id.btnSistem)
    }

    private fun loadAccountInfo() {
        // Load from SharedPreferences first (fast)
        val sessionPref = SecurePreferences.getUserSession(this)
        val nama = sessionPref.getString("user_name", "") ?: ""
        val role = sessionPref.getString("user_role", "") ?: ""
        val email = sessionPref.getString("user_email", "") ?: ""

        if (nama.isNotEmpty()) {
            tvAccountName.text = nama
            tvInitial.text = nama.first().uppercase()
        }

        tvAccountRole.text = when {
            role.contains("admin") -> "Administrator"
            role.contains("wali") -> "Wali Kelas"
            role.contains("guru") -> "Guru"
            role.contains("siswa") -> "Siswa"
            else -> "Pengguna"
        }

        if (email.isNotEmpty()) {
            tvAccountEmail.text = email
        }

        // Refresh from API
        val token = SecurePreferences.getUserData(this).getString("auth_token", "") ?: ""
        if (token.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.getProfile("Bearer $token")
                    if (response.isSuccessful) {
                        val profile = response.body()?.data
                        if (profile != null) {
                            withContext(Dispatchers.Main) {
                                val displayName = profile.getDisplayName()
                                tvAccountName.text = displayName
                                if (displayName.isNotEmpty()) {
                                    tvInitial.text = displayName.first().uppercase()
                                }

                                val roleRaw = profile.role?.lowercase() ?: ""
                                tvAccountRole.text = when {
                                    roleRaw.contains("admin") -> "Administrator"
                                    roleRaw.contains("wali") -> "Wali Kelas"
                                    roleRaw.contains("guru") -> "Guru"
                                    roleRaw.contains("siswa") -> "Siswa"
                                    else -> "Pengguna"
                                }

                                profile.email?.let { tvAccountEmail.text = it }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Use cached data
                }
            }
        }
    }

    private fun setupThemeButtons() {
        val currentTheme = getSavedTheme()
        updateButtonStates(currentTheme)

        btnTerang.setOnClickListener {
            saveTheme(THEME_LIGHT)
            applyTheme(THEME_LIGHT)
            updateButtonStates(THEME_LIGHT)
        }

        btnGelap.setOnClickListener {
            saveTheme(THEME_DARK)
            applyTheme(THEME_DARK)
            updateButtonStates(THEME_DARK)
        }

        btnSistem.setOnClickListener {
            saveTheme(THEME_SYSTEM)
            applyTheme(THEME_SYSTEM)
            updateButtonStates(THEME_SYSTEM)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        cardAccount.setOnClickListener {
            startActivity(Intent(this, PengaturanAkunActivity::class.java))
        }
    }

    private fun getSavedTheme(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    private fun saveTheme(theme: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun updateButtonStates(activeTheme: String) {
        // Reset all buttons to outline style
        btnTerang.setStyleOutline()
        btnGelap.setStyleOutline()
        btnSistem.setStyleOutline()

        // Set active button to filled style
        when (activeTheme) {
            THEME_LIGHT -> btnTerang.setStyleFilled()
            THEME_DARK -> btnGelap.setStyleFilled()
            THEME_SYSTEM -> btnSistem.setStyleFilled()
        }
    }

    private fun MaterialButton.setStyleOutline() {
        this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PengaturanActivity, android.R.color.transparent))
        this.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this@PengaturanActivity, R.color.blue_theme))
        this.strokeWidth = 3
        this.setTextColor(ContextCompat.getColor(this@PengaturanActivity, R.color.blue_theme))
    }

    private fun MaterialButton.setStyleFilled() {
        this.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@PengaturanActivity, R.color.blue_theme))
        this.strokeWidth = 0
        this.setTextColor(ContextCompat.getColor(this@PengaturanActivity, R.color.white))
    }
}
