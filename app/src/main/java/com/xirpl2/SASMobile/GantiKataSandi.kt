package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.ApiResponse
import com.xirpl2.SASMobile.model.PasswordResetRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class GantiKataSandi : BaseActivity() {
    private lateinit var nisLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var etNis: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnKirim: Button
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NAMING MISMATCH: Layout file is activity_lupa_katasandi.xml but class is GantiKataSandi.
        // Conventional name would be activity_ganti_kata_sandi.xml. Not renamed to avoid breaking R.layout references.
        setContentView(R.layout.activity_lupa_katasandi)
        window.statusBarColor = 0xFF2886D6.toInt()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        nisLayout = findViewById(R.id.nisLayout)
        emailLayout = findViewById(R.id.emailLayout)

        etNis = findViewById(R.id.et_nis)
        etEmail = findViewById(R.id.et_email)
        btnKirim = findViewById(R.id.buttonKirim)
        progressBar = findViewById(R.id.progressBar)

        findViewById<TextView>(R.id.tvKembaliMasuk).setOnClickListener {
            startActivity(Intent(this, MasukActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.tvVerifikasiOtp).setOnClickListener {
            val resetPrefs = com.xirpl2.SASMobile.utils.SecurePreferences.getPasswordResetData(this)
            resetPrefs.edit()
                .putString("reset_email", etEmail.text.toString().trim())
                .putString("reset_nis", etNis.text.toString().trim())
                .apply()
            startActivity(Intent(this, VerifikasiOtpActivity::class.java))
        }

        setHintTextColors()
        
        btnKirim.setOnClickListener {
            val nis = etNis.text.toString().trim()
            val email = etEmail.text.toString().trim()

            
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

            
            requestOtp(nis, email)
        }
    }

    private fun requestOtp(nis: String, email: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = PasswordResetRequest(nis = nis, email = email)
                val response = RetrofitClient.apiService.forgotPassword(request)

                setLoading(false)

                val genericMessage = "Jika akun terdaftar, OTP akan dikirim ke email Anda"

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    // ApiResponse<MessageResponse> has message field directly
                    val isSuccessByMessage = !apiResponse.message.isNullOrEmpty() &&
                        (apiResponse.message.contains("berhasil", ignoreCase = true) ||
                         apiResponse.message.contains("dikirim", ignoreCase = true) ||
                         apiResponse.message.contains("sukses", ignoreCase = true) ||
                         apiResponse.message.contains("success", ignoreCase = true))
                    val isHttpSuccess = response.code() == 200

                    android.util.Log.d("GantiKataSandi", "OTP Response - http: $isHttpSuccess, msgContainsSuccess: $isSuccessByMessage")

                    val shouldNavigate = isHttpSuccess && isSuccessByMessage

                    if (shouldNavigate) {
                        android.util.Log.d("GantiKataSandi", "OTP berhasil dikirim, navigasi ke VerifikasiOtpActivity")

                        MotionToast.createColorToast(
                            this@GantiKataSandi,
                            "Berhasil",
                            genericMessage,
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )

                        val resetPrefs = com.xirpl2.SASMobile.utils.SecurePreferences.getPasswordResetData(this@GantiKataSandi)
                        resetPrefs.edit()
                            .putString("reset_email", email)
                            .putString("reset_nis", nis)
                            .apply()
                        startActivity(Intent(this@GantiKataSandi, VerifikasiOtpActivity::class.java))
                    } else {
                        // Show generic message to prevent user enumeration
                        MotionToast.createColorToast(
                            this@GantiKataSandi,
                            "Informasi",
                            genericMessage,
                            MotionToastStyle.INFO,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                } else {
                    // On any error (404, 400, 500, etc.), show generic message to prevent user enumeration
                    android.util.Log.w("GantiKataSandi", "OTP request failed with code: ${response.code()}")
                    MotionToast.createColorToast(
                        this@GantiKataSandi,
                        "Informasi",
                        genericMessage,
                        MotionToastStyle.INFO,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                }
            } catch (e: Exception) {
                setLoading(false)
                
                if (com.xirpl2.SASMobile.BuildConfig.DEBUG) {
                    android.util.Log.e("GantiKataSandi", "Exception terjadi: ${e.message}", e)
                    android.util.Log.e("GantiKataSandi", "Stack trace: ${e.stackTraceToString()}")
                }
                
                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda"
                    is java.net.SocketTimeoutException -> "Koneksi timeout. Periksa koneksi internet Anda"
                    is javax.net.ssl.SSLException -> "Masalah keamanan koneksi SSL"
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                
                MotionToast.createColorToast(
                    this@GantiKataSandi,
                    "Kesalahan",
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
        btnKirim.alpha = if (isLoading) 0.6f else 1.0f
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        etNis.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading

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