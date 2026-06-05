package com.xirpl2.SASMobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.LoginRequest
import com.xirpl2.SASMobile.utils.AnrDetector
import kotlinx.coroutines.delay

class MasukActivity : BaseActivity() {

    private lateinit var etNis: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnMasuk: Button
    private lateinit var textBuatAkun: TextView
    private lateinit var textLupaPassword: TextView
    private lateinit var nisLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    
    private val inputHandler = Handler(Looper.getMainLooper())
    private var autoLoginJob: kotlinx.coroutines.Job? = null

    private var loginCooldownJob: kotlinx.coroutines.Job? = null
    private companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "MasukActivity"
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val COOLDOWN_SECONDS = 30
        private const val BRUTE_FORCE_PREFS = "login_brute_force"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "cooldown_until"
    }

    private fun getBruteForcePrefs() = com.xirpl2.SASMobile.utils.SecurePreferences.getBruteForceData(this)

    private fun getFailedAttempts(): Int = getBruteForcePrefs().getInt(KEY_FAILED_ATTEMPTS, 0)

    private fun setFailedAttempts(count: Int) {
        getBruteForcePrefs().edit().putInt(KEY_FAILED_ATTEMPTS, count).apply()
    }

    private fun getCooldownUntil(): Long = getBruteForcePrefs().getLong(KEY_COOLDOWN_UNTIL, 0)

    private fun setCooldownUntil(timestamp: Long) {
        getBruteForcePrefs().edit().putLong(KEY_COOLDOWN_UNTIL, timestamp).apply()
    }

    private fun clearBruteForceState() {
        getBruteForcePrefs().edit().clear().apply()
    }

    private fun dismissKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun clearFieldErrors() {
        nisLayout.error = null
        passwordLayout.error = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_masuk)
            window.statusBarColor = 0xFF2886D6.toInt()
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
            }

            initializeViews()
            setHintTextColors()
            checkCameraPermission()

            // Restore cooldown if still active after activity recreation
            restoreCooldownIfActive()

            // Delay auto-login check using lifecycleScope for automatic cancellation
            autoLoginJob = lifecycleScope.launch {
                delay(500)
                if (!isFinishing && !isDestroyed) {
                    checkAndValidateExistingToken()
                }
            }

            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Crash in onCreate: ${e.message}")
        }
    }

    private fun restoreCooldownIfActive() {
        val cooldownUntil = getCooldownUntil()
        val now = System.currentTimeMillis()
        if (cooldownUntil > now) {
            val secondsRemaining = ((cooldownUntil - now) / 1000).toInt()
            btnMasuk.isEnabled = false
            loginCooldownJob = lifecycleScope.launch {
                for (s in secondsRemaining downTo 1) {
                    if (isFinishing || isDestroyed) return@launch
                    btnMasuk.text = "Tunggu ${s}s..."
                    delay(1000)
                }
                clearBruteForceState()
                btnMasuk.isEnabled = true
                btnMasuk.text = "Masuk"
            }
        } else if (getFailedAttempts() >= MAX_LOGIN_ATTEMPTS) {
            // Cooldown expired but attempts not reset yet
            clearBruteForceState()
        }
    }

    private fun initializeViews() {
        etNis = findViewById(R.id.et_nis)
        etPassword = findViewById(R.id.et_password)
        btnMasuk = findViewById(R.id.btn_masuk)
        textBuatAkun = findViewById(R.id.textBuatAkun)
        textLupaPassword = findViewById(R.id.textLupaPassword)
        nisLayout = findViewById(R.id.nisLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
    }

    private fun setupClickListeners() {
        btnMasuk.setOnClickListener {
            AnrDetector.recordUiInteraction()
            loginUser()
        }

        textBuatAkun.setOnClickListener {
            AnrDetector.recordUiInteraction()
            safeNavigateTo(DaftarActivity::class.java)
        }

        textLupaPassword.setOnClickListener {
            AnrDetector.recordUiInteraction()
            safeNavigateTo(GantiKataSandi::class.java)
        }

        // IME "Done" action on password field triggers login
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                loginUser()
                true
            } else false
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun setHintTextColors() {
        val hintColor = ContextCompat.getColorStateList(this, R.color.hint_and_floating)
        nisLayout.defaultHintTextColor = hintColor
        passwordLayout.defaultHintTextColor = hintColor
    }

    private fun loginUser() {
        // Block login if cooldown is active (check persisted timestamp)
        val cooldownUntil = getCooldownUntil()
        if (cooldownUntil > System.currentTimeMillis()) return

        clearFieldErrors()

        val nisOrUsername = etNis.text.toString().trim()
        val password = etPassword.text.toString()

        // Inline field-level validation errors
        if (nisOrUsername.isEmpty()) {
            nisLayout.error = "NIS/Username wajib diisi"
            etNis.requestFocus()
            return
        }
        if (password.isEmpty()) {
            passwordLayout.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }

        // Cancel auto login if manual login is started
        autoLoginJob?.cancel()

        dismissKeyboard()
        btnMasuk.isEnabled = false
        btnMasuk.text = "Masuk..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(identifier = nisOrUsername, password = password))

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.data != null) {
                            // Reset failed attempts on success
                            clearBruteForceState()
                            btnMasuk.isEnabled = true
                            btnMasuk.text = "Masuk"

                            val userData = body.data
                            val token = userData.token

                            // Guard: reject login if server returned no token
                            if (token.isNullOrEmpty()) {
                                onLoginFailed()
                                showToast("Gagal", "Token autentikasi tidak diterima dari server. Silakan coba lagi.", MotionToastStyle.ERROR)
                                return@withContext
                            }

                            showToast("Berhasil", "Selamat datang, ${userData.getDisplayName()}!", MotionToastStyle.SUCCESS)
                            saveUserSession(userData)

                            // Reset lock sebelum navigasi apapun (toast bisa trigger onPause)
                            isTransitioning.set(false)
                            anrWatchdogActive.set(false)

                            if (userData.is_verified == false) {
                                safeNavigateTo(VerifyAccountActivity::class.java)
                            } else {
                                navigateToHome()
                            }
                        } else {
                            onLoginFailed()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                val jsonObject = JSONObject(errorBody)
                                jsonObject.optString("message", "Login gagal. Kode error: ${response.code()}")
                            } else {
                                when (response.code()) {
                                    401 -> "NIS/Username atau password salah"
                                    404 -> "Akun tidak ditemukan"
                                    400 -> "Data yang dikirim tidak valid"
                                    500 -> "Server sedang bermasalah. Silakan coba lagi nanti"
                                    else -> "Login gagal. Kode error: ${response.code()}"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing error body: ${e.message}")
                            when (response.code()) {
                                401 -> "NIS/Username atau password salah"
                                404 -> "Akun tidak ditemukan"
                                400 -> "Data yang dikirim tidak valid"
                                500 -> "Server sedang bermasalah. Silakan coba lagi nanti"
                                else -> "Login gagal. Kode error: ${response.code()}"
                            }
                        }
                        onLoginFailed()
                        showToast("Gagal", errorMessage, MotionToastStyle.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    onLoginFailed()
                    showToast("Kesalahan", "Terjadi kesalahan jaringan. Periksa koneksi Anda.", MotionToastStyle.ERROR)
                }
            }
        }
    }

    private fun onLoginFailed() {
        val newCount = getFailedAttempts() + 1
        setFailedAttempts(newCount)
        if (newCount >= MAX_LOGIN_ATTEMPTS) {
            startLoginCooldown()
        } else {
            btnMasuk.isEnabled = true
            btnMasuk.text = "Masuk"
        }
    }

    private fun startLoginCooldown() {
        btnMasuk.isEnabled = false
        val cooldownUntil = System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000L)
        setCooldownUntil(cooldownUntil)
        loginCooldownJob = lifecycleScope.launch {
            for (secondsRemaining in COOLDOWN_SECONDS downTo 1) {
                if (isFinishing || isDestroyed) return@launch
                btnMasuk.text = "Tunggu ${secondsRemaining}s..."
                delay(1000)
            }
            clearBruteForceState()
            btnMasuk.isEnabled = true
            btnMasuk.text = "Masuk"
        }
    }

    /**
     * Saves auth token and user data to BOTH SharedPreferences stores
     * ("user_session" and "UserData"). Code paths that read from either store
     * (TokenAuthenticator, activities, logout) must keep both in sync.
     * See also: TokenAuthenticator.authenticate() and clearUserSession().
     */
    private fun saveUserSession(user: com.xirpl2.SASMobile.model.AkunLoginResponse?) {
        if (user == null) return

        // Store 1: "user_session" — full session data, read by most activities
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        with(sharedPref.edit()) {
            putString("user_id", user.id?.toString())
            putString("user_nis", user.nis ?: "")
            putString("user_nip", user.nip ?: "")
            putString("user_name", user.getDisplayName())
            putString("user_jk", user.jk ?: "")
            putString("user_kelas", user.kelas ?: "")
            putString("user_jurusan", user.jurusan ?: "")
            putString("user_email", user.email ?: "")
            putString("user_role", user.role)
            putBoolean("is_verified", user.is_verified ?: true)
            putString("auth_token", user.token)
            putString("refresh_token", user.refresh_token)
            apply()
        }

        // Store 2: "UserData" — subset read by some activities and TokenAuthenticator
        val userDataPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
        with(userDataPref.edit()) {
            putString("auth_token", user.token)
            putString("nama_siswa", user.getDisplayName())
            putString("nis", user.getIdentifier())
            putString("jenis_kelamin", user.jk ?: "L")
            putString("user_role", user.role)
            apply()
        }
    }

    private fun checkAndValidateExistingToken() {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val token = sharedPref.getString("auth_token", null)
        val role = sharedPref.getString("user_role", "siswa")

        if (token.isNullOrEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    
                    if (response.isSuccessful && response.code() == 200) {
                        val isVerified = sharedPref.getBoolean("is_verified", true)
                        
                        val targetActivity = if (!isVerified) {
                            VerifyAccountActivity::class.java
                        } else {
                            val roleLower = role?.lowercase()?.trim()
                            when (roleLower) {
                                "guru", "wali_kelas", "wali kelas" -> BerandaGuruActivity::class.java
                                "admin" -> BerandaAdminActivity::class.java
                                else -> StudentMainActivity::class.java
                            }
                        }
                        
                        val intent = Intent(this@MasukActivity, targetActivity)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else if (response.code() == 401 || response.code() == 403) {
                        // Only clear session if token is explicitly rejected by server
                        clearUserSession()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto-login token validation failed: ${e.message}")
                // For network errors or other exceptions, we DON'T clear the session.
                // This allows the user to try again later or work offline if supported.
            }
        }
    }

    private fun navigateToHome() {
        isTransitioning.set(false)
        anrWatchdogActive.set(false)  // ← TAMBAHKAN INI (line baru setelah 168)

        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        val role = sharedPref.getString("user_role", "siswa")
        
        val roleLower = role?.lowercase()?.trim()
        val targetActivity = when (roleLower) {
            "guru", "wali_kelas", "wali kelas" -> BerandaGuruActivity::class.java
            "admin" -> BerandaAdminActivity::class.java
            else -> StudentMainActivity::class.java
        }
        
        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(title: String, message: String, style: MotionToastStyle) {
        if (isFinishing || isDestroyed) return
        try {
            MotionToast.createColorToast(
                this,
                title,
                message,
                style,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
        } catch (e: Exception) {
            Log.w(TAG, "MotionToast failed, falling back to Toast: ${e.message}")
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Clears ALL SharedPreferences stores to prevent stale token/data after logout.
     * Must match the dual-store pattern used by saveUserSession(), TokenAuthenticator,
     * and the logout handlers in BaseAdminActivity / BerandaActivity.
     */
    override fun onDestroy() {
        inputHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun clearUserSession() {
        com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
            .edit().clear().apply()
        com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .edit().clear().apply()
        getSharedPreferences("NotificationData", Context.MODE_PRIVATE)
            .edit().clear().apply()
        com.xirpl2.SASMobile.utils.NotificationCounterManager.clearCounter(this)
        androidx.work.WorkManager.getInstance(this)
            .cancelUniqueWork(com.xirpl2.SASMobile.utils.NotificationPollWorker.WORK_NAME)
    }
}
