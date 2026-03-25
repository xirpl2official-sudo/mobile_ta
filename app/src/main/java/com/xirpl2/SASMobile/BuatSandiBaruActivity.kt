package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.ResetPasswordRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class BuatSandiBaruActivity : AppCompatActivity() {
    private lateinit var passwordLayoutBaru: TextInputLayout
    private lateinit var passwordlayoutKonfirm: TextInputLayout
    private lateinit var etPasswordBaru: EditText
    private lateinit var etKonfirmPassword: EditText
    private lateinit var btnBuatSandi: Button
    private var progressBar: ProgressBar? = null
    
    private var userNis: String = ""
    private var userOtp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_buat_sandi_baru)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        // Set statusbar to dark themed (dark icons)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get data from intent
        userNis = intent.getStringExtra("USER_NIS") ?: ""
        userOtp = intent.getStringExtra("USER_OTP") ?: ""

        passwordLayoutBaru = findViewById(R.id.passwordLayoutBaru)
        passwordlayoutKonfirm = findViewById(R.id.passwordLayoutKonfirm)
        etPasswordBaru = findViewById(R.id.et_passwordbaru)
        etKonfirmPassword = findViewById(R.id.et_konfirmpassword)
        btnBuatSandi = findViewById(R.id.buttonBuatSandi)
        progressBar = findViewById(R.id.progressBar)

        setHintTextColors()

        btnBuatSandi.setOnClickListener {
            val passwordBaru = etPasswordBaru.text.toString()
            val konfirmPassword = etKonfirmPassword.text.toString()

            // Validate empty password
            if (passwordBaru.isEmpty()) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Password baru tidak boleh kosong",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etPasswordBaru.requestFocus()
                return@setOnClickListener
            }

            // Validate password length
            if (passwordBaru.length < 6) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Password minimal 6 karakter",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etPasswordBaru.requestFocus()
                return@setOnClickListener
            }

            // Validate confirm password
            if (konfirmPassword.isEmpty()) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Konfirmasi password tidak boleh kosong",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etKonfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // Validate passwords match
            if (passwordBaru != konfirmPassword) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Password dan konfirmasi password tidak sama",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etKonfirmPassword.requestFocus()
                return@setOnClickListener
            }

            // Call reset password API
            resetPassword(passwordBaru)
        }
    }

    private fun resetPassword(newPassword: String) {
        if (userNis.isEmpty() || userOtp.isEmpty()) {
            MotionToast.createColorToast(
                this,
                "Error",
                "Data tidak lengkap. Silakan ulangi proses reset password",
                MotionToastStyle.ERROR,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
            // Navigate back to forgot password screen
            val intent = Intent(this, GantiKataSandi::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = ResetPasswordRequest(
                    nis = userNis,
                    otp = userOtp,
                    newPassword = newPassword
                )
                val response = RetrofitClient.apiService.resetPassword(request)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status) {
                        MotionToast.createColorToast(
                            this@BuatSandiBaruActivity,
                            "Berhasil",
                            apiResponse.message ?: "Password berhasil diubah. Silakan login dengan password baru",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )

                        // Navigate to login screen
                        val intent = Intent(this@BuatSandiBaruActivity, MasukActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        MotionToast.createColorToast(
                            this@BuatSandiBaruActivity,
                            "Gagal",
                            apiResponse.message ?: "Gagal mengubah password",
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        if (!errorBody.isNullOrEmpty()) {
                            val jsonObject = org.json.JSONObject(errorBody)
                            jsonObject.optString("message", "Gagal mengubah password")
                        } else {
                            when (response.code()) {
                                400 -> "OTP tidak valid atau sudah kadaluarsa"
                                404 -> "Data tidak ditemukan"
                                else -> "Gagal mengubah password. Silakan coba lagi"
                            }
                        }
                    } catch (e: Exception) {
                        when (response.code()) {
                            400 -> "OTP tidak valid atau sudah kadaluarsa"
                            404 -> "Data tidak ditemukan"
                            else -> "Gagal mengubah password. Silakan coba lagi"
                        }
                    }
                    
                    MotionToast.createColorToast(
                        this@BuatSandiBaruActivity,
                        "Gagal",
                        errorMessage,
                        MotionToastStyle.ERROR,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                }
            } catch (e: Exception) {
                setLoading(false)
                
                MotionToast.createColorToast(
                    this@BuatSandiBaruActivity,
                    "Error",
                    "Terjadi kesalahan jaringan. Periksa koneksi Anda",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnBuatSandi.isEnabled = !isLoading
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        etPasswordBaru.isEnabled = !isLoading
        etKonfirmPassword.isEnabled = !isLoading
        
        if (isLoading) {
            btnBuatSandi.text = "Menyimpan..."
        } else {
            btnBuatSandi.text = getString(R.string.ButtonBuatSandi)
        }
    }

    private fun setHintTextColors() {
        val hintColor = ContextCompat.getColorStateList(this, R.color.hint_and_floating)
        passwordLayoutBaru.defaultHintTextColor = hintColor
        passwordlayoutKonfirm.defaultHintTextColor = hintColor
    }
}