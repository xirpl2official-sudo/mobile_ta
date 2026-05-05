package com.xirpl2.SASMobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.LoginRequest

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
                
            } else {
                
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

        
        btnMasuk.isEnabled = false
        btnMasuk.text = "Masuk..."

        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                
                val response = RetrofitClient.apiService.login(LoginRequest(identifier = nisOrUsername, password = password))

                withContext(Dispatchers.Main) {
                    btnMasuk.isEnabled = true
                    btnMasuk.text = "Masuk"

                    if (response.isSuccessful) {
                        val body = response.body()
                        val respCode = response.code()

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

                            
                            saveUserSession(userData)

                            
                            val roleLower = userData?.role?.lowercase()?.trim()
                            val targetActivity = when (roleLower) {
                                "guru", "wali_kelas", "wali kelas" -> BerandaGuruActivity::class.java
                                "admin" -> BerandaAdminActivity::class.java
                                "siswa" -> BerandaActivity::class.java
                                else -> {
                                    if (userData?.isStaff() == true) {
                                        if (roleLower?.contains("wali") == true || roleLower?.contains("guru") == true) 
                                            BerandaGuruActivity::class.java
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

        if (token.isNullOrEmpty()) {
            return
        }

        
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.code() == 200) {
                        
                        
                        val roleLower = role?.lowercase()?.trim()
                        val targetActivity = when (roleLower) {
                            "guru", "wali_kelas", "wali kelas" -> BerandaGuruActivity::class.java
                            "admin" -> BerandaAdminActivity::class.java
                            else -> BerandaActivity::class.java
                        }
                        
                        startActivity(Intent(this@MasukActivity, targetActivity))
                        finish()
                    } else {
                        
                        clearUserSession()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    
                    clearUserSession()
                }
            }
        }
    }

    private fun clearUserSession() {
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
    }
}
