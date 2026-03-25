package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.xirpl2.SASMobile.model.OtpVerificationRequest
import com.xirpl2.SASMobile.model.PasswordResetRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch
import android.os.CountDownTimer
import java.util.concurrent.TimeUnit
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class VerifikasiOtpActivity : AppCompatActivity() {

    private var resendTimer: CountDownTimer? = null
    private lateinit var otpBoxes: Array<EditText>
    private var progressBar: ProgressBar? = null
    private var btnVerifikasi: Button? = null
    private var userNis: String = ""
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verifikasi_otp)
        window.statusBarColor = 0xFFE48134.toInt()
        
        // Set statusbar to dark themed (dark icons)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get data from intent
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        userNis = intent.getStringExtra("USER_NIS") ?: ""

        val tvEmailInfo = findViewById<TextView>(R.id.tvEmailInfo)
        if (userEmail.isNotEmpty()) {
            tvEmailInfo.text = getString(R.string.email_verifikasi, userEmail)
        }

        progressBar = findViewById(R.id.progressBar)
        btnVerifikasi = findViewById(R.id.btnVerifikasi)

        // Setup OTP Input
        setupOtpInput()

        // Setup Kirim Ulang OTP
        val btnKirimUlang = findViewById<TextView>(R.id.btnKirimUlang)
        btnKirimUlang.isClickable = true
        btnKirimUlang.setOnClickListener {
            kirimUlangOtpKeServer()
            startResendTimer(btnKirimUlang)
        }

        // Mulai timer jika perlu (misal setelah pertama kali masuk halaman)
        startResendTimer(btnKirimUlang)

        // Setup Verify Button
        btnVerifikasi?.setOnClickListener {
            val otp = getOtpFromBoxes()
            if (otp.length < 6) {
                MotionToast.createColorToast(
                    this,
                    "Gagal",
                    "Masukkan kode OTP 6 digit",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
                return@setOnClickListener
            }
            verifyOtp(otp)
        }
    }

    private fun getOtpFromBoxes(): String {
        return otpBoxes.joinToString("") { it.text.toString() }
    }

    private fun verifyOtp(otp: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val request = OtpVerificationRequest(nis = userNis, otp = otp)
                val response = RetrofitClient.apiService.verifyOtp(request)

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status) {
                        MotionToast.createColorToast(
                            this@VerifikasiOtpActivity,
                            "Berhasil",
                            apiResponse.message ?: "Kode OTP terverifikasi",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )

                        // Navigate to reset password screen
                        val intent = Intent(this@VerifikasiOtpActivity, BuatSandiBaruActivity::class.java)
                        intent.putExtra("USER_NIS", userNis)
                        intent.putExtra("USER_OTP", otp)
                        startActivity(intent)
                        finish()
                    } else {
                        MotionToast.createColorToast(
                            this@VerifikasiOtpActivity,
                            "Gagal",
                            apiResponse.message ?: "Kode OTP tidak valid",
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                        clearOtpBoxes()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        if (!errorBody.isNullOrEmpty()) {
                            val jsonObject = org.json.JSONObject(errorBody)
                            jsonObject.optString("message", "Gagal memverifikasi OTP")
                        } else {
                            when (response.code()) {
                                400 -> "Kode OTP tidak valid atau sudah kadaluarsa"
                                404 -> "Data tidak ditemukan"
                                else -> "Gagal memverifikasi OTP. Silakan coba lagi"
                            }
                        }
                    } catch (e: Exception) {
                        when (response.code()) {
                            400 -> "Kode OTP tidak valid atau sudah kadaluarsa"
                            404 -> "Data tidak ditemukan"
                            else -> "Gagal memverifikasi OTP. Silakan coba lagi"
                        }
                    }
                    
                    MotionToast.createColorToast(
                        this@VerifikasiOtpActivity,
                        "Gagal",
                        errorMessage,
                        MotionToastStyle.ERROR,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                    
                    // Clear OTP boxes on error
                    clearOtpBoxes()
                }
            } catch (e: Exception) {
                setLoading(false)
                
                MotionToast.createColorToast(
                    this@VerifikasiOtpActivity,
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

    private fun clearOtpBoxes() {
        otpBoxes.forEach { it.text.clear() }
        otpBoxes[0].requestFocus()
    }

    private fun setLoading(isLoading: Boolean) {
        btnVerifikasi?.isEnabled = !isLoading
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        otpBoxes.forEach { it.isEnabled = !isLoading }
    }

    private fun startResendTimer(view: TextView) {
        // Batalkan timer sebelumnya (opsional, cegah multiple timer)
        resendTimer?.cancel()

        view.isEnabled = false

        resendTimer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(1), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                view.text = "Kirim Ulang (${seconds}s)"
            }

            override fun onFinish() {
                view.text = getString(R.string.KirimUlgKode)
                view.isEnabled = true
            }
        }.start()
    }

    private fun kirimUlangOtpKeServer() {
        if (userNis.isEmpty() || userEmail.isEmpty()) {
            MotionToast.createColorToast(
                this,
                "Error",
                "Data tidak lengkap. Silakan kembali dan coba lagi",
                MotionToastStyle.ERROR,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
            return
        }

        lifecycleScope.launch {
            try {
                val request = PasswordResetRequest(nis = userNis, email = userEmail)
                val response = RetrofitClient.apiService.forgotPassword(request)

                if (response.isSuccessful) {
                    MotionToast.createColorToast(
                        this@VerifikasiOtpActivity,
                        "Berhasil",
                        "Kode OTP baru telah dikirim ke email Anda",
                        MotionToastStyle.SUCCESS,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                } else {
                    MotionToast.createColorToast(
                        this@VerifikasiOtpActivity,
                        "Gagal",
                        "Gagal mengirim ulang OTP",
                        MotionToastStyle.ERROR,
                        Gravity.CENTER,
                        MotionToast.LONG_DURATION,
                        null
                    )
                }
            } catch (e: Exception) {
                MotionToast.createColorToast(
                    this@VerifikasiOtpActivity,
                    "Error",
                    "Terjadi kesalahan jaringan",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
            }
        }
    }

    private fun setupOtpInput() {
        val otpBox1 = findViewById<EditText>(R.id.otpBox1)
        val otpBox2 = findViewById<EditText>(R.id.otpBox2)
        val otpBox3 = findViewById<EditText>(R.id.otpBox3)
        val otpBox4 = findViewById<EditText>(R.id.otpBox4)
        val otpBox5 = findViewById<EditText>(R.id.otpBox5)
        val otpBox6 = findViewById<EditText>(R.id.otpBox6)

        otpBoxes = arrayOf(otpBox1, otpBox2, otpBox3, otpBox4, otpBox5, otpBox6)

        for (i in otpBoxes.indices) {
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < otpBoxes.size - 1) {
                        otpBoxes[i + 1].requestFocus()
                    }
                    
                    // Auto verify when all 6 digits are entered
                    if (i == otpBoxes.size - 1 && s?.length == 1) {
                        val otp = getOtpFromBoxes()
                        if (otp.length == 6) {
                            verifyOtp(otp)
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            otpBoxes[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (otpBoxes[i].text.isEmpty() && i > 0) {
                        otpBoxes[i - 1].requestFocus()
                        otpBoxes[i - 1].text.clear()
                    }
                }
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}