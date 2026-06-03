package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
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
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputLayout

class DaftarActivity : BaseActivity() {

    private lateinit var etNis: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnDaftar: Button
    private lateinit var textMasuk: TextView
    private lateinit var nisLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout2: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar)
        window.statusBarColor = 0xFFE48134.toInt()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        etNis = findViewById(R.id.et_nis)
        etPassword = findViewById(R.id.et_password)
        etEmail = findViewById(R.id.et_email)
        btnDaftar = findViewById(R.id.btn_daftar)
        textMasuk = findViewById(R.id.textMasuk)
        nisLayout = findViewById(R.id.nisLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout2 = findViewById(R.id.passwordLayout2)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        setHintTextColors()
        setupRealTimeValidation()

        btnDaftar.setOnClickListener {
            registerUser()
        }

        textMasuk.setOnClickListener {
            finish()
        }

        etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                registerUser()
                true
            } else false
        }
    }

    private fun setupRealTimeValidation() {
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                if (password.isNotEmpty() && !isPasswordValid(password)) {
                    passwordLayout2.error = "Harus ada huruf dan angka. Contoh: Sandi123"
                } else {
                    passwordLayout2.error = null
                }
                // Re-validate confirm password if it has text
                val confirm = etConfirmPassword.text.toString()
                if (confirm.isNotEmpty()) {
                    if (confirm != password) {
                        confirmPasswordLayout.error = "Kata sandi tidak cocok"
                    } else {
                        confirmPasswordLayout.error = null
                    }
                }
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val confirm = s?.toString() ?: ""
                val password = etPassword.text.toString()
                if (confirm.isNotEmpty() && confirm != password) {
                    confirmPasswordLayout.error = "Kata sandi tidak cocok"
                } else {
                    confirmPasswordLayout.error = null
                }
            }
        })
    }

    private fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun dismissKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun clearFieldErrors() {
        nisLayout.error = null
        emailLayout.error = null
        passwordLayout2.error = null
        confirmPasswordLayout.error = null
    }

    private fun setHintTextColors() {
        val hintColor = ContextCompat.getColorStateList(this, R.color.hint_and_floating)
        nisLayout.defaultHintTextColor = hintColor
        emailLayout.defaultHintTextColor = hintColor
        passwordLayout2.defaultHintTextColor = hintColor
        confirmPasswordLayout.defaultHintTextColor = hintColor
    }

    private fun registerUser() {
        clearFieldErrors()

        val nis = etNis.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val email = etEmail.text.toString().trim()

        if (nis.isEmpty()) {
            nisLayout.error = "NIS wajib diisi"
            etNis.requestFocus()
            return
        }
        if (email.isEmpty()) {
            emailLayout.error = "Email wajib diisi"
            etEmail.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Format email tidak valid"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            passwordLayout2.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }
        if (!isPasswordValid(password)) {
            passwordLayout2.error = "Harus ada huruf dan angka. Contoh: Sandi123"
            etPassword.requestFocus()
            return
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Konfirmasi password wajib diisi"
            etConfirmPassword.requestFocus()
            return
        }
        if (password != confirmPassword) {
            confirmPasswordLayout.error = "Konfirmasi kata sandi tidak cocok"
            etConfirmPassword.requestFocus()
            return
        }

        dismissKeyboard()
        btnDaftar.isEnabled = false
        btnDaftar.text = "Mendaftar..."

        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.register(RegisterRequest(nis, email, password))

                withContext(Dispatchers.Main) {
                    btnDaftar.isEnabled = true
                    btnDaftar.text = "Buat Akun"

                    if (response.isSuccessful) {
                        val body = response.body()

                        MotionToast.createColorToast(
                            this@DaftarActivity,
                            "Berhasil",
                            body?.message ?: "Daftar berhasil",
                            MotionToastStyle.SUCCESS,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                        finish() 
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                org.json.JSONObject(errorBody).optString("message", "Pendaftaran gagal")
                            } else {
                                "Pendaftaran gagal (${response.code()})"
                            }
                        } catch (_: Exception) {
                            "Pendaftaran gagal"
                        }
                        MotionToast.createColorToast(
                            this@DaftarActivity,
                            "Gagal",
                            errorMessage,
                            MotionToastStyle.ERROR,
                            Gravity.CENTER,
                            MotionToast.LONG_DURATION,
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnDaftar.isEnabled = true
                    btnDaftar.text = "Buat Akun"
                    MotionToast.createColorToast(
                        this@DaftarActivity,
                        "Error",
                        if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException)
                            "Tidak dapat terhubung ke server"
                        else "Terjadi kesalahan, coba lagi",
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