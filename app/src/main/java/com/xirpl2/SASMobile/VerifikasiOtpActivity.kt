package com.xirpl2.SASMobile

import android.content.Context
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.xirpl2.SASMobile.model.OtpVerificationRequest
import com.xirpl2.SASMobile.model.PasswordResetRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.CountDownTimer
import java.util.concurrent.TimeUnit
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class VerifikasiOtpActivity : BaseActivity() {

    companion object {
        private const val MAX_OTP_ATTEMPTS = 5
        private const val COOLDOWN_SECONDS = 60
        private const val BRUTE_FORCE_PREFS = "otp_brute_force"
        private const val KEY_FAILED_ATTEMPTS = "otp_failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "otp_cooldown_until"

        /** In-memory OTP holder to avoid passing sensitive data via Intent extras. */
        private var pendingOtp: String? = null

        fun consumePendingOtp(): String? {
            val otp = pendingOtp
            pendingOtp = null
            return otp
        }

        private fun setPendingOtp(otp: String) {
            pendingOtp = otp
        }
    }

    private var resendTimer: CountDownTimer? = null
    private var otpCooldownJob: kotlinx.coroutines.Job? = null
    private lateinit var otpBoxes: Array<EditText>
    private var progressBar: ProgressBar? = null
    private var btnVerifikasi: Button? = null
    private var userNis: String = ""
    private var userEmail: String = ""

    private fun getBruteForcePrefs() = getSharedPreferences(BRUTE_FORCE_PREFS, Context.MODE_PRIVATE)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verifikasi_otp)
        window.statusBarColor = 0xFFE48134.toInt()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        val resetPrefs = com.xirpl2.SASMobile.utils.SecurePreferences.getPasswordResetData(this)
        userEmail = resetPrefs.getString("reset_email", "") ?: ""
        userNis = resetPrefs.getString("reset_nis", "") ?: ""

        val tvEmailInfo = findViewById<TextView>(R.id.tvEmailInfo)
        if (userEmail.isNotEmpty()) {
            tvEmailInfo.text = getString(R.string.email_verifikasi, userEmail)
        }

        progressBar = findViewById(R.id.progressBar)
        btnVerifikasi = findViewById(R.id.btnVerifikasi)

        
        setupOtpInput()

        
        val btnKirimUlang = findViewById<TextView>(R.id.btnKirimUlang)
        btnKirimUlang.isClickable = true
        btnKirimUlang.setOnClickListener {
            kirimUlangOtpKeServer()
            startResendTimer(btnKirimUlang)
        }


        startResendTimer(btnKirimUlang)

        // Restore OTP cooldown if still active after activity recreation
        val cooldownUntil = getCooldownUntil()
        val now = System.currentTimeMillis()
        if (cooldownUntil > now) {
            val secondsRemaining = ((cooldownUntil - now) / 1000).toInt()
            startOtpCooldown(secondsRemaining)
        } else if (getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
            // Cooldown expired but attempts not yet reset
            clearBruteForceState()
        }

        btnVerifikasi?.setOnClickListener {
            // Block verification if cooldown is active
            val currentCooldown = getCooldownUntil()
            if (currentCooldown > System.currentTimeMillis()) return@setOnClickListener

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

                    // MessageResponse hanya memiliki field 'message', success disimpulkan dari HTTP 200
                    val isSuccessByMessage = !apiResponse.message.isNullOrEmpty() &&
                        (apiResponse.message.contains("valid", ignoreCase = true) ||
                         apiResponse.message.contains("sukses", ignoreCase = true) ||
                         apiResponse.message.contains("berhasil", ignoreCase = true) ||
                         apiResponse.message.contains("success", ignoreCase = true))
                    val isHttpSuccess = response.code() == 200

                    // Navigasi jika HTTP sukses dan pesan mengandung kata kunci sukses
                    val shouldNavigate = isHttpSuccess && isSuccessByMessage

                    if (shouldNavigate) {
                        // Reset failed attempts on success
                        clearBruteForceState()

                        MotionToast.createColorToast(
                            this@VerifikasiOtpActivity,
                            "Berhasil",
                            apiResponse.message ?: "Kode OTP terverifikasi",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )

                        setPendingOtp(otp)
                        startActivity(Intent(this@VerifikasiOtpActivity, BuatSandiBaruActivity::class.java))
                        finish()
                    } else {
                        onOtpFailed()
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
                    onOtpFailed()
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

                    clearOtpBoxes()
                }
            } catch (e: Exception) {
                setLoading(false)

                MotionToast.createColorToast(
                    this@VerifikasiOtpActivity,
                    "Kesalahan",
                    "Terjadi kesalahan jaringan. Periksa koneksi Anda",
                    MotionToastStyle.ERROR,
                    Gravity.CENTER,
                    MotionToast.LONG_DURATION,
                    null
                )
            }
        }
    }

    private fun onOtpFailed() {
        val newCount = getFailedAttempts() + 1
        setFailedAttempts(newCount)
        if (newCount >= MAX_OTP_ATTEMPTS) {
            startOtpCooldown(COOLDOWN_SECONDS)
        } else {
            val remaining = MAX_OTP_ATTEMPTS - newCount
            MotionToast.createColorToast(
                this,
                "Peringatan",
                "Sisa percobaan: $remaining",
                MotionToastStyle.WARNING,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
        }
    }

    private fun startOtpCooldown(seconds: Int) {
        btnVerifikasi?.isEnabled = false
        val cooldownUntil = System.currentTimeMillis() + (seconds * 1000L)
        setCooldownUntil(cooldownUntil)
        otpCooldownJob?.cancel()
        otpCooldownJob = lifecycleScope.launch {
            for (secondsRemaining in seconds downTo 1) {
                if (isFinishing || isDestroyed) return@launch
                btnVerifikasi?.text = "Tunggu ${secondsRemaining}s..."
                delay(1000)
            }
            clearBruteForceState()
            btnVerifikasi?.isEnabled = true
            btnVerifikasi?.text = "Verifikasi"
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
                "Kesalahan",
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
                    "Kesalahan",
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
                    
                    
                    if (i == otpBoxes.size - 1 && s?.length == 1) {
                        // Block auto-verify if cooldown is active
                        if (getCooldownUntil() <= System.currentTimeMillis()) {
                            val otp = getOtpFromBoxes()
                            if (otp.length == 6) {
                                verifyOtp(otp)
                            }
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
        otpCooldownJob?.cancel()
    }
}