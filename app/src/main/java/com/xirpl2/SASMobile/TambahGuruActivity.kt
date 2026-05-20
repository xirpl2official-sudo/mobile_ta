package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.xirpl2.SASMobile.model.CreateGuruRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahGuruActivity : BaseActivity() {

    private lateinit var etNama: EditText
    private lateinit var etNip: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_guru)

        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etNama = findViewById(R.id.etNama)
        etNip = findViewById(R.id.etNip)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        progressLoading = findViewById(R.id.progressLoading)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSimpan.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun validateAndSubmit() {
        val nama = etNama.text.toString().trim()
        val nip = etNip.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nama, Email, dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        submitData(nama, if (nip.isEmpty()) null else nip, email, password)
    }

    private fun submitData(nama: String, nip: String?, email: String, password: String) {
        val token = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        setLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = CreateGuruRequest(
                    email = email,
                    password = password,
                    nama = nama,
                    nip = nip
                )

                val response = RetrofitClient.apiService.createGuru("Bearer $token", request)
                
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@TambahGuruActivity, "Berhasil menambahkan guru", Toast.LENGTH_SHORT).show()
                        finish() // Return to previous screen
                    } else {
                        Toast.makeText(this@TambahGuruActivity, "Gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Toast.makeText(this@TambahGuruActivity, "Terjadi kesalahan koneksi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            btnSimpan.text = ""
            btnSimpan.isEnabled = false
            progressLoading.visibility = View.VISIBLE
        } else {
            btnSimpan.text = "Simpan Data"
            btnSimpan.isEnabled = true
            progressLoading.visibility = View.GONE
        }
    }
}