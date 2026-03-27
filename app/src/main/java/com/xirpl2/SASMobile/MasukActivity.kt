package com.xirpl2.SASMobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import com.xirpl2.SASMobile.model.LoginRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout


class MasukActivity : AppCompatActivity() {

    private lateinit var etNis: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnMasuk: Button
    private lateinit var textBuatAkun: TextView
    private lateinit var textLupaPassword: TextView
    private lateinit var nisLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_masuk)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        // Set statusbar to dark themed (dark icons)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etNis = findViewById(R.id.et_nis)
        etPassword = findViewById(R.id.et_password)
        btnMasuk = findViewById(R.id.btn_masuk)
        textBuatAkun = findViewById(R.id.textBuatAkun)
        textLupaPassword = findViewById(R.id.textLupaPassword)
        nisLayout = findViewById(R.id.nisLayout)
        passwordLayout = findViewById(R.id.passwordLayout)

        setHintTextColors()

        checkCameraPermission()

        // Check for existing token and auto-login
        checkAndValidateExistingToken()

        btnMasuk.setOnClickListener {
            loginUser()
        }

        textBuatAkun.setOnClickListener {
            val intent = Intent(this@MasukActivity, DaftarActivity::class.java)
            startActivity(intent)
        }

        textLupaPassword.setOnClickListener {
            val intent = Intent(this@MasukActivity, GantiKataSandi::class.java)
            startActivity(intent)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
            } else {
                // Camera permission denied
            }
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

        // Validasi input
        if (nisOrUsername.isEmpty() || password.isEmpty()) {
            MotionToast.createColorToast(
                this,
                "Gagal",
                "NIS/Username dan Password wajib diisi!",
                MotionToastStyle.ERROR,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
            return
        }

        // Disable button saat proses
        btnMasuk.isEnabled = false
        btnMasuk.text = "Masuk..."

        // Panggil API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Log parameters being sent
                Log.d("LoadDataDebug", "Logging in with: identifier=$nisOrUsername")
                val response = RetrofitClient.apiService.login(LoginRequest(identifier = nisOrUsername, password = password))

                withContext(Dispatchers.Main) {
                    btnMasuk.isEnabled = true
                    btnMasuk.text = "Masuk"

                    if (response.isSuccessful) {
                        val body = response.body()
                        val respCode = response.code()
                        Log.d("MasukActivity", "Response: ${response.code()} - $body")

                        if (respCode == 200) {
                            val userData = body!!.data
                            MotionToast.createColorToast(
                                this@MasukActivity,
                                "Berhasil",
                                "Selamat datang, ${userData?.getDisplayName() ?: ""}!",
                                MotionToastStyle.SUCCESS,
                                Gravity.CENTER,
                                MotionToast.LONG_DURATION,
                                null
                            )

                            // Simpan data user ke SharedPreferences
                            saveUserSession(userData)

                            // Normalize role for navigation and subsequent use
                            val roleNormalized = userData?.role?.lowercase()?.replace(' ', '_')?.trim() ?: ""
                            
                            val targetActivity = when (roleNormalized) {
                                "guru", "wali_kelas" -> BerandaGuruActivity::class.java
                                "admin" -> BerandaAdminActivity::class.java
                                "siswa" -> BerandaActivity::class.java
                                else -> {
                                    if (userData?.isStaff() == true) {
                                        if (roleNormalized.contains("wali")) BerandaGuruActivity::class.java
                                        else BerandaAdminActivity::class.java
                                    } else BerandaActivity::class.java
                                }
                            }
                            
                            startActivity(Intent(this@MasukActivity, targetActivity))
                            finish()
                        } else {
                            MotionToast.createColorToast(
                                this@MasukActivity,
                                "Gagal",
                                body?.message ?: "Login gagal",
                                MotionToastStyle.ERROR,
                                Gravity.CENTER,
                                MotionToast.LONG_DURATION,
                                null
                            )
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("MasukActivity", "Error: ${response.code()} - $errorBody")
                        MotionToast.createColorToast(
                            this@MasukActivity,
                            "Gagal",
                            "NIS atau password salah",
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MasukActivity", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    btnMasuk.isEnabled = true
                    btnMasuk.text = "Masuk"
                    MotionToast.createColorToast(
                        this@MasukActivity,
                        "Error",
                        "Error: ${e.message}",
                        MotionToastStyle.ERROR,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                }
            }
        }
    }

    private fun saveUserSession(user: com.xirpl2.SASMobile.model.AkunLoginResponse?) {
        if (user == null) return

        val roleNormalized = user.role?.lowercase()?.replace(' ', '_')?.trim() ?: ""

        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_id", user.id?.toString())
            putString("user_nis", user.nis ?: "")
            putString("user_nip", user.nip ?: "")
            putString("user_name", user.getDisplayName())
            putString("user_jk", user.jk ?: "")
            putString("user_kelas", user.kelas ?: "")
            putString("user_jurusan", user.jurusan ?: "")
            putString("user_role", roleNormalized)
            putString("auth_token", user.token)
            apply()
        }
        
        // Also save to UserData prefs for consistency with other activities
        val userDataPref = getSharedPreferences("UserData", MODE_PRIVATE)
        with(userDataPref.edit()) {
            putString("auth_token", user.token)
            putString("nama_siswa", user.getDisplayName())
            putString("nis", user.getIdentifier())
            putString("jenis_kelamin", user.jk ?: "L")
            putString("user_role", roleNormalized)
            apply()
        }
    }

    /**
     * Check if token exists and validate it with auth/me endpoint
     */
    private fun checkAndValidateExistingToken() {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", null)
        val role = sharedPref.getString("user_role", "siswa")

        if (token.isNullOrEmpty()) {
            Log.d("MasukActivity", "No existing token found")
            return
        }

        Log.d("MasukActivity", "Validating existing token...")
        
        // Validate token by calling auth/me
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.code() == 200) {
                        Log.d("MasukActivity", "Token validation successful, auto-login")
                        
                        // Navigate based on saved role
                        val targetActivity = when (role) {
                            "guru" -> BerandaGuruActivity::class.java
                            "admin", "wali_kelas" -> BerandaAdminActivity::class.java
                            else -> BerandaActivity::class.java
                        }
                        
                        startActivity(Intent(this@MasukActivity, targetActivity))
                        finish()
                    } else {
                        Log.e("MasukActivity", "Token validation failed: ${response.code()}")
                        // Token is invalid, clear it
                        clearUserSession()
                    }
                }
            } catch (e: Exception) {
                Log.e("MasukActivity", "Token validation error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // Clear token on error
                    clearUserSession()
                }
            }
        }
    }

    /**
     * Clear user session from SharedPreferences
     */
    private fun clearUserSession() {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        Log.d("MasukActivity", "User session cleared")
    }
}
