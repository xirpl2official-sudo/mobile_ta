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
import com.xirpl2.SASMobile.model.UpdateGuruRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditGuruActivity : BaseActivity() {

    private lateinit var etNama: EditText
    private lateinit var etNip: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSimpan: Button
    private lateinit var progressLoading: ProgressBar

    private var guruId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_guru)

        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)

        initViews()
        loadIntentData()
        setupListeners()
    }

    private fun initViews() {
        etNama = findViewById(R.id.etNama)
        etNip = findViewById(R.id.etNip)
        etEmail = findViewById(R.id.etEmail)
        btnSimpan = findViewById(R.id.btnSimpan)
        progressLoading = findViewById(R.id.progressLoading)
    }

    private fun loadIntentData() {
        guruId = intent.getIntExtra("guru_id", -1)
        val nama = intent.getStringExtra("guru_nama") ?: ""
        val nip = intent.getStringExtra("guru_nip") ?: ""
        val email = intent.getStringExtra("guru_email") ?: ""

        if (guruId == -1) {
            Toast.makeText(this, "Data guru tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etNama.setText(nama)
        etNip.setText(nip)
        etEmail.setText(email)
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

        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Email wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        submitData(nama, if (nip.isEmpty()) null else nip, email)
    }

    private fun submitData(nama: String, nip: String?, email: String) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        setLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateGuruRequest(
                    email = email,
                    nama = nama,
                    nip = nip
                )

                val response = RetrofitClient.apiService.updateGuru("Bearer $token", guruId, request)

                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditGuruActivity, "Berhasil memperbarui data guru", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditGuruActivity, "Gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    Toast.makeText(this@EditGuruActivity, "Terjadi kesalahan koneksi", Toast.LENGTH_SHORT).show()
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
            btnSimpan.text = "Simpan Perubahan"
            btnSimpan.isEnabled = true
            progressLoading.visibility = View.GONE
        }
    }
}
