package com.xirpl2.SASMobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.VerifyAccountRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyAccountActivity : BaseActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var btnVerify: MaterialButton
    private lateinit var btnLogout: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_account)

        // Set status bar color to match blue header
        window.statusBarColor = 0xFF1A77C6.toInt()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        btnVerify = findViewById(R.id.btnVerify)
        btnLogout = findViewById(R.id.btnLogout)

        btnVerify.setOnClickListener {
            verifyAccount()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun verifyAccount() {
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        var isValid = true

        if (newPassword.isEmpty()) {
            tilNewPassword.error = "Kata sandi tidak boleh kosong"
            isValid = false
        } else if (newPassword.length < 8) {
            tilNewPassword.error = "Minimal 8 karakter"
            isValid = false
        } else {
            tilNewPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Konfirmasi kata sandi tidak boleh kosong"
            isValid = false
        } else if (newPassword != confirmPassword) {
            tilConfirmPassword.error = "Kata sandi tidak cocok"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        if (!isValid) return

        btnVerify.isEnabled = false
        btnVerify.text = "Memverifikasi..."

        val request = VerifyAccountRequest(
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@VerifyAccountActivity)
                val token = sharedPref.getString("auth_token", null)
                    ?: com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@VerifyAccountActivity)
                        .getString("auth_token", null)

                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VerifyAccountActivity, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
                        logout()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.verifyAccount("Bearer $token", request)

                withContext(Dispatchers.Main) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verifikasi dan Lanjutkan"

                    if (response.isSuccessful) {
                        Toast.makeText(this@VerifyAccountActivity, "Akun berhasil diverifikasi", Toast.LENGTH_SHORT).show()
                        
                        // Update the local storage to mark as verified
                        with(com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@VerifyAccountActivity).edit()) {
                            putBoolean("is_verified", true)
                            apply()
                        }
                        
                        // Check role and navigate to appropriate home
                        val role = sharedPref.getString("user_role", "siswa")
                        when (role) {
                            "guru", "wali_kelas" -> startActivity(Intent(this@VerifyAccountActivity, BerandaGuruActivity::class.java))
                            "admin" -> startActivity(Intent(this@VerifyAccountActivity, BerandaAdminActivity::class.java))
                            else -> startActivity(Intent(this@VerifyAccountActivity, BerandaActivity::class.java))
                        }
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                org.json.JSONObject(errorBody).optString("message", "Gagal memverifikasi akun")
                            } else {
                                "Gagal memverifikasi akun (${response.code()})"
                            }
                        } catch (e: Exception) {
                            "Gagal memverifikasi akun"
                        }
                        Toast.makeText(this@VerifyAccountActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verifikasi dan Lanjutkan"
                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Koneksi timeout"
                        else -> "Terjadi kesalahan: ${e.message}"
                    }
                    Toast.makeText(this@VerifyAccountActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun logout() {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (token.isNotEmpty()) {
                    RetrofitClient.apiService.logout("Bearer $token")
                }
            } catch (_: Exception) { }

            withContext(Dispatchers.Main) {
                with(com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@VerifyAccountActivity).edit()) {
                    clear()
                    apply()
                }
                with(com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@VerifyAccountActivity).edit()) {
                    clear()
                    apply()
                }
                with(getSharedPreferences("NotificationData", Context.MODE_PRIVATE).edit()) {
                    clear()
                    apply()
                }
                com.xirpl2.SASMobile.utils.NotificationCounterManager.clearCounter(this@VerifyAccountActivity)
                androidx.work.WorkManager.getInstance(this@VerifyAccountActivity)
                    .cancelUniqueWork(com.xirpl2.SASMobile.utils.NotificationPollWorker.WORK_NAME)

                val intent = Intent(this@VerifyAccountActivity, MasukActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}
