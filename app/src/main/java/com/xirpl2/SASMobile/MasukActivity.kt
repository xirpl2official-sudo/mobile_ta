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

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "MasukActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_masuk)
            
            // UI Styling
            window.statusBarColor = 0xFF2886D6.toInt()
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
            }

            findViewById<android.view.View>(R.id.main)?.let { mainView ->
                ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            initializeViews()
            setupNonBlockingInputHandling()
            setHintTextColors()
            checkCameraPermission()
            
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

    private fun initializeViews() {
        etNis = findViewById(R.id.et_nis)
        etPassword = findViewById(R.id.et_password)
        btnMasuk = findViewById(R.id.btn_masuk)
        textBuatAkun = findViewById(R.id.textBuatAkun)
        textLupaPassword = findViewById(R.id.textLupaPassword)
        nisLayout = findViewById(R.id.nisLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
    }

    /**
     * Prevents ANR by handling IME operations outside the immediate UI focus call.
     */
    private fun setupNonBlockingInputHandling() {
        val inputViews = listOf(etNis, etPassword)
        inputViews.forEach { view ->
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    AnrDetector.recordUiInteraction()
                    inputHandler.postDelayed({
                        try {
                            if (!isFinishing && !isDestroyed) {
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Keyboard focus handling error", e)
                        }
                    }, 100)
                }
            }
        }
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
        val nisOrUsername = etNis.text.toString().trim()
        val password = etPassword.text.toString()

        if (nisOrUsername.isEmpty() || password.isEmpty()) {
            showToast("Gagal", "NIS/Username dan Password wajib diisi!", MotionToastStyle.ERROR)
            return
        }

        // Cancel auto login if manual login is started
        autoLoginJob?.cancel()

        btnMasuk.isEnabled = false
        btnMasuk.text = "Masuk..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(identifier = nisOrUsername, password = password))

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    
                    btnMasuk.isEnabled = true
                    btnMasuk.text = "Masuk"

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.data != null) {
                            val userData = body.data
                            showToast("Berhasil", "Selamat datang, ${userData.getDisplayName()}!", MotionToastStyle.SUCCESS)
                            saveUserSession(userData)
    
                            // Reset lock sebelum navigasi apapun (toast bisa trigger onPause)
                            isTransitioning.set(false)      // ← TAMBAHKAN
                            anrWatchdogActive.set(false)    // ← TAMBAHKAN
                            
                            val token = userData.token
                            if (userData.is_verified == false) {
                                safeNavigateTo(VerifyAccountActivity::class.java)
                            } else if (token != null) {
                                checkDeviceAuth(token)
                            } else {
                                navigateToHome()
                            }
                        }
                    } else {
                        showToast("Gagal", "NIS atau password salah", MotionToastStyle.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    btnMasuk.isEnabled = true
                    btnMasuk.text = "Masuk"
                    showToast("Error", "Error: ${e.message}", MotionToastStyle.ERROR)
                }
            }
        }
    }

    private fun saveUserSession(user: com.xirpl2.SASMobile.model.AkunLoginResponse?) {
        if (user == null) return

        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_id", user.id?.toString())
            putString("user_nis", user.nis ?: "")
            putString("user_nip", user.nip ?: "")
            putString("user_name", user.getDisplayName())
            putString("user_jk", user.jk ?: "")
            putString("user_kelas", user.kelas ?: "")
            putString("user_jurusan", user.jurusan ?: "")
            putString("user_role", user.role)
            putBoolean("is_verified", user.is_verified ?: true)
            putString("auth_token", user.token)
            apply()
        }
        
        val userDataPref = getSharedPreferences("UserData", MODE_PRIVATE)
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
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
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
                                else -> BerandaActivity::class.java
                            }
                        }
                        
                        val intent = Intent(this@MasukActivity, targetActivity)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        clearUserSession()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Auto-login token validation failed (expected on physical devices without API): ${e.message}")
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) clearUserSession()
                }
            }
        }
    }

    private fun checkDeviceAuth(token: String) {
        val deviceRepo = com.xirpl2.SASMobile.repository.DeviceRepository()
        val hardwareId = com.xirpl2.SASMobile.utils.DeviceHelper.getHardwareId(this)
        val imei = com.xirpl2.SASMobile.utils.DeviceHelper.getImei(this)
        val deviceName = com.xirpl2.SASMobile.utils.DeviceHelper.getDeviceName()
        val deviceModel = com.xirpl2.SASMobile.utils.DeviceHelper.getDeviceModel()
        val osVersion = com.xirpl2.SASMobile.utils.DeviceHelper.getOsVersion()

        Log.d(TAG, "checkDeviceAuth: hardware_id=$hardwareId, imei=$imei, device=$deviceName ($deviceModel), os=$osVersion")

        val authRequest = com.xirpl2.SASMobile.model.HardwareAuthRequest(
            hardwareId = hardwareId,
            imei = imei,
            deviceName = deviceName,
            deviceModel = deviceModel,
            osVersion = osVersion
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val verifyResult = deviceRepo.verifyDevice(token, authRequest)
            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                verifyResult.fold(
                    onSuccess = { navigateToHome() },
                    onFailure = { e ->
                        Log.d(TAG, "checkDeviceAuth: verify failed (${e.message}), trying register")
                        registerNewDevice(token, authRequest)
                    }
                )
            }
        }
    }

    private fun registerNewDevice(token: String, request: com.xirpl2.SASMobile.model.HardwareAuthRequest) {
        val deviceRepo = com.xirpl2.SASMobile.repository.DeviceRepository()
        lifecycleScope.launch(Dispatchers.IO) {
            val registerResult = deviceRepo.registerDevice(token, request)
            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                registerResult.fold(
                    onSuccess = {
                        Log.d(TAG, "registerNewDevice: success")
                        showToast("Perangkat Terikat", "Akun Anda telah berhasil dikunci ke perangkat ini.", MotionToastStyle.INFO)
                        navigateToHome()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "registerNewDevice: failed: ${e.message}")
                        showToast("Keamanan Perangkat", "Gagal mendaftarkan perangkat: ${e.message}", MotionToastStyle.ERROR)
                        clearUserSession()
                    }
                )
            }
        }
    }

    private fun navigateToHome() {
        isTransitioning.set(false)
        anrWatchdogActive.set(false)  // ← TAMBAHKAN INI (line baru setelah 168)

        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val role = sharedPref.getString("user_role", "siswa")
        
        val roleLower = role?.lowercase()?.trim()
        val targetActivity = when (roleLower) {
            "guru", "wali_kelas", "wali kelas" -> BerandaGuruActivity::class.java
            "admin" -> BerandaAdminActivity::class.java
            else -> BerandaActivity::class.java
        }
        
        val intent = Intent(this, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(title: String, message: String, style: MotionToastStyle) {
        if (isFinishing || isDestroyed) return
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun clearUserSession() {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}
