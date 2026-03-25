package com.xirpl2.SASMobile

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import com.xirpl2.SASMobile.model.RegisterRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout

class DaftarActivity : AppCompatActivity() {

    private lateinit var etNis: EditText
    private lateinit var etPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnDaftar: Button
    private lateinit var textMasuk: TextView
    private lateinit var nisLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout2: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar)
        window.statusBarColor = 0xFFE48134.toInt()
        
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
        etEmail = findViewById(R.id.et_email)
        btnDaftar = findViewById(R.id.btn_daftar)
        textMasuk = findViewById(R.id.textMasuk)
        nisLayout = findViewById(R.id.nisLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout2 = findViewById(R.id.passwordLayout2)

        setHintTextColors()

        btnDaftar.setOnClickListener {
            registerUser()
        }

        textMasuk.setOnClickListener {
            finish() // Kembali ke halaman login
        }
    }

    private fun setHintTextColors() {
        val hintColor = ContextCompat.getColorStateList(this, R.color.hint_and_floating)
        nisLayout.defaultHintTextColor = hintColor
        emailLayout.defaultHintTextColor = hintColor
        passwordLayout2.defaultHintTextColor = hintColor
    }

    private fun registerUser() {
        val nis = etNis.text.toString().trim()
        val password = etPassword.text.toString()
        val email = etEmail.text.toString().trim()

        // Validasi input
        if (nis.isEmpty() || password.isEmpty() || email.isEmpty()) {
            MotionToast.createColorToast(
                this,
                "Peringatan",
                "Semua field harus diisi!",
                MotionToastStyle.WARNING,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
            return
        }

        if (password.length < 6) {
            MotionToast.createColorToast(
                this,
                "Peringatan",
                "Password minimal 6 karakter",
                MotionToastStyle.WARNING,
                Gravity.CENTER,
                MotionToast.LONG_DURATION,
                null
            )
            return
        }

        // Disable button saat proses
        btnDaftar.isEnabled = false
        btnDaftar.text = "Mendaftar..."

        // Panggil API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DaftarActivity", "Mengirim request register: NIS=$nis")
                val response = RetrofitClient.apiService.register(RegisterRequest(nis, email, password))

                withContext(Dispatchers.Main) {
                    btnDaftar.isEnabled = true
                    btnDaftar.text = "Buat Akun"

                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d("DaftarActivity", "Response: ${response.code()} - $body")

                        MotionToast.createColorToast(
                            this@DaftarActivity,
                            "Berhasil",
                            body?.message ?: "Daftar berhasil",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                        finish() // Kembali ke halaman login
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("DaftarActivity", "Error: ${response.code()} - $errorBody")
                        MotionToast.createColorToast(
                            this@DaftarActivity,
                            "Gagal",
                            "Daftar gagal: ${response.message()}",
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DaftarActivity", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    btnDaftar.isEnabled = true
                    btnDaftar.text = "Buat Akun"
                    MotionToast.createColorToast(
                        this@DaftarActivity,
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
}