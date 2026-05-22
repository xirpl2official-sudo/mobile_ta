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
import com.google.android.material.snackbar.Snackbar
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
    private var dataLoaded: Boolean = false

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

        if (guruId == -1) {
            Toast.makeText(this, "Data guru tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show intent data immediately as placeholder, but block saving
        etNama.setText(intent.getStringExtra("guru_nama") ?: "")
        etNip.setText(intent.getStringExtra("guru_nip") ?: "")
        etEmail.setText(intent.getStringExtra("guru_email") ?: "")

        // Disable save until fresh data loads successfully
        dataLoaded = false
        btnSimpan.isEnabled = false

        // Fetch fresh data from API
        fetchGuruDetail()
    }

    private fun fetchGuruDetail() {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this).getString("auth_token", "") ?: ""
        if (token.isEmpty()) return

        setLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getGuruDetail("Bearer $token", guruId)

                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    if (response.isSuccessful) {
                        val guru = response.body()?.data
                        if (guru != null) {
                            etNama.setText(guru.nama)
                            etNip.setText(guru.nip)
                            etEmail.setText(guru.email)
                        }
                        dataLoaded = true
                        btnSimpan.isEnabled = true
                    } else {
                        showFetchRetry("Gagal memuat data guru")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoadingState(false)
                    showFetchRetry("Terjadi kesalahan koneksi")
                }
            }
        }
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSimpan.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun showFetchRetry(message: String) {
        Snackbar.make(btnSimpan, message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Coba Lagi") { fetchGuruDetail() }
            .show()
    }

    private fun validateAndSubmit() {
        if (!dataLoaded) {
            Toast.makeText(this, "Data belum dimuat dari server", Toast.LENGTH_SHORT).show()
            return
        }

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
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = if (!errorBody.isNullOrEmpty()) {
                            try { org.json.JSONObject(errorBody).getString("message") } catch (_: Exception) { errorBody }
                        } else {
                            "Gagal memperbarui data guru"
                        }
                        Toast.makeText(this@EditGuruActivity, "Gagal: $errorMsg", Toast.LENGTH_SHORT).show()
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
            btnSimpan.isEnabled = dataLoaded
            progressLoading.visibility = View.GONE
        }
    }
}
