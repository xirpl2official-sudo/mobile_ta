package com.xirpl2.SASMobile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.model.CreateGuruRequest
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class TambahGuruActivity : BaseActivity() {

    private lateinit var etNama: EditText
    private lateinit var etNip: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressLoading: ProgressBar
    private val repository = BerandaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_guru)

        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val topBar = findViewById<android.view.View>(R.id.topBar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private lateinit var tilPassword: com.google.android.material.textfield.TextInputLayout

    private fun generateDefaultPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#"
        return (1..12).map { chars.random() }.joinToString("")
    }

    private fun initViews() {
        etNama = findViewById(R.id.etNama)
        etNip = findViewById(R.id.etNip)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSimpan = findViewById(R.id.btnSimpan)
        progressLoading = findViewById(R.id.progressLoading)
        tilPassword = findViewById(R.id.tilPassword)

        val defaultPassword = generateDefaultPassword()
        etPassword.setText(defaultPassword)
        tilPassword.helperText = "Password default sudah di-generate. Bagikan ke guru setelah akun dibuat."
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

        if (nama.isEmpty() || nip.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Nama, NIP, Email, dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Guru")
            .setMessage("Apakah Anda yakin ingin menambahkan guru baru?")
            .setPositiveButton("Simpan") { _, _ ->
                submitData(nama, nip, email, password)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun submitData(nama: String, nip: String?, email: String, password: String) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        setLoadingState(true)

        lifecycleScope.launch {
            val request = CreateGuruRequest(email = email, password = password, nama = nama, nip = nip ?: "")
            repository.createGuru(token, request).fold(
                onSuccess = {
                    setLoadingState(false)
                    Snackbar.make(findViewById(R.id.main), "Berhasil menambahkan guru", Snackbar.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { e ->
                    setLoadingState(false)
                    Snackbar.make(findViewById(R.id.main), "Gagal: ${e.message}", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Coba Lagi") { submitData(nama, nip, email, password) }
                        .setActionTextColor(androidx.core.content.ContextCompat.getColor(this@TambahGuruActivity, R.color.blue_theme))
                        .show()
                }
            )
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