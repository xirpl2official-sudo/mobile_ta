package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.model.CreateGuruRequest
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahGuruDialogFragment : BottomSheetDialogFragment() {

    private val repository = BerandaRepository()
    private lateinit var btnSimpan: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_tambah_guru, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnBatal = view.findViewById<Button>(R.id.btnBatal)
        btnSimpan = view.findViewById(R.id.btnSimpan)
        
        val etNama = view.findViewById<EditText>(R.id.etNama)
        val etNip = view.findViewById<EditText>(R.id.etNip)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)

        btnClose.setOnClickListener { dismiss() }
        btnBatal.setOnClickListener { dismiss() }
        
        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val nip = etNip.text.toString().trim().ifEmpty { null }

            if (nama.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Mohon isi semua field wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tambah Guru")
                .setMessage("Apakah Anda yakin ingin menambahkan guru baru?")
                .setPositiveButton("Simpan") { _, _ ->
                    submitCreate(nama, nip, email, password)
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun submitCreate(nama: String, nip: String?, email: String, password: String) {
        val ctx = requireContext()
        btnSimpan.isEnabled = false
        lifecycleScope.launch {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(ctx)
                val token = sharedPref.getString("auth_token", null)
                    ?: com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(ctx)
                        .getString("auth_token", null)
                if (token == null) {
                    Toast.makeText(ctx, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
                    btnSimpan.isEnabled = true
                    return@launch
                }
                val request = CreateGuruRequest(email = email, password = password, nama = nama, nip = nip ?: "")
                repository.createGuru(token, request).fold(
                    onSuccess = {
                        Toast.makeText(ctx, "Guru berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        dismiss()
                    },
                    onFailure = { e ->
                        Toast.makeText(ctx, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnSimpan.isEnabled = true
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(ctx, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSimpan.isEnabled = true
            }
        }
    }
}
