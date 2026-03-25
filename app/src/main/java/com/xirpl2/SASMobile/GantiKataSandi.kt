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
import com.google.gson.Gson
import com.xirpl2.SASMobile.model.ApiResponse
import com.xirpl2.SASMobile.model.PasswordResetRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class GantiKataSandi : AppCompatActivity() {
    private lateinit var nisLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var etNis: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnKirim: Button
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lupa_katasandi)
        window.statusBarColor = 0xFF2886D6.toInt()
        
        // Set statusbar to dark themed (dark icons)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        nisLayout = findViewById(R.id.nisLayout)
        emailLayout = findViewById(R.id.emailLayout)

        etNis = findViewById(R.id.et_nis)
        etEmail = findViewById(R.id.et_email)
        btnKirim = findViewById(R.id.buttonKirim)
        progressBar = findViewById(R.id.progressBar)

        setHintTextColors()
        
        btnKirim.setOnClickListener {
            val nis = etNis.text.toString().trim()
            val email = etEmail.text.toString().trim()

            // Validasi NIS/Username tidak boleh kosong
            if (nis.isEmpty()) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "NIS/Username tidak boleh kosong",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etNis.requestFocus()
                return@setOnClickListener
            }

            // Validasi email tidak boleh kosong
            if (email.isEmpty()) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Email tidak boleh kosong",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // Validasi format email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Format email tidak valid",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // Call forgot-password API
            requestOtp(nis, email)
        }
    }

    private fun requestOtp(nis: String, email: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                android.util.Log.d("GantiKataSandi", "Mengirim request OTP untuk NIS: $nis, Email: $email")
                
                val request = PasswordResetRequest(nis = nis, email = email)
                val response = RetrofitClient.apiService.forgotPassword(request)

                android.util.Log.d("GantiKataSandi", "Response code: ${response.code()}")
                android.util.Log.d("GantiKataSandi", "Response body: ${response.body()}")
                android.util.Log.d("GantiKataSandi", "Response error body: ${response.errorBody()?.string()}")

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    
                    if (apiResponse.status) {
                        android.util.Log.d("GantiKataSandi", "OTP berhasil dikirim: ${apiResponse.message}")
                        
                        MotionToast.createColorToast(
                            this@GantiKataSandi,
                            "Berhasil",
                            apiResponse.message ?: "Kode OTP telah dikirim ke email Anda",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )

                        // Navigate to OTP verification screen
                        val intent = Intent(this@GantiKataSandi, VerifikasiOtpActivity::class.java)
                        intent.putExtra("USER_EMAIL", email)
                        intent.putExtra("USER_NIS", nis)
                        startActivity(intent)
                    } else {
                        android.util.Log.e("GantiKataSandi", "API Response status false: ${apiResponse.message}")
                        MotionToast.createColorToast(
                            this@GantiKataSandi,
                            "Gagal",
                            apiResponse.message ?: "Gagal mengirim OTP",
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                } else {
                    // Parse error body ONCE
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("GantiKataSandi", "Error response: $errorBody")
                    
                    val errorMessage = try {
                        if (!errorBody.isNullOrEmpty()) {
                            val jsonObject = JSONObject(errorBody)
                            jsonObject.optString("message", "Gagal mengirim OTP")
                        } else {
                            when (response.code()) {
                                404 -> "NIS/Username atau email tidak ditemukan"
                                400 -> "Data yang dikirim tidak valid"
                                500 -> "Server sedang bermasalah. Silakan coba lagi nanti"
                                else -> "Gagal mengirim OTP. Kode error: ${response.code()}"
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GantiKataSandi", "Error parsing error body: ${e.message}")
                        when (response.code()) {
                            404 -> "NIS/Username atau email tidak ditemukan"
                            400 -> "Data yang dikirim tidak valid"
                            500 -> "Server sedang bermasalah. Silakan coba lagi nanti"
                            else -> "Gagal mengirim OTP. Kode error: ${response.code()}"
                        }
                    }
                    
                    MotionToast.createColorToast(
                        this@GantiKataSandi,
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
                
                android.util.Log.e("GantiKataSandi", "Exception terjadi: ${e.message}", e)
                android.util.Log.e("GantiKataSandi", "Stack trace: ${e.stackTraceToString()}")
                
                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda"
                    is java.net.SocketTimeoutException -> "Koneksi timeout. Periksa koneksi internet Anda"
                    is javax.net.ssl.SSLException -> "Masalah keamanan koneksi SSL"
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                
                MotionToast.createColorToast(
                    this@GantiKataSandi,
                    "Error",
                    errorMsg,
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnKirim.isEnabled = !isLoading
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        if (isLoading) {
            btnKirim.text = "Mengirim..."
        } else {
            btnKirim.text = getString(R.string.KirimOTP)
        }
    }

    private fun setHintTextColors() {
        val hintColor = ContextCompat.getColorStateList(this, R.color.hint_and_floating)
        nisLayout.defaultHintTextColor = hintColor
        emailLayout.defaultHintTextColor = hintColor
    }
}