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
import com.xirpl2.SASMobile.model.CreateGuruRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahGuruDialogFragment : BottomSheetDialogFragment() {

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
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpan)
        
        val etNama = view.findViewById<EditText>(R.id.etNama)
        val etNip = view.findViewById<EditText>(R.id.etNip)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)

        btnClose.setOnClickListener { dismiss() }
        btnBatal.setOnClickListener { dismiss() }
        
        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val nip = etNip.text.toString().ifEmpty { null }

            if (nama.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Mohon isi semua field wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(requireContext())
                        val token = sharedPref.getString("auth_token", null)
                            ?: com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
                                .getString("auth_token", null)
                        if (token == null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show()
                                btnSimpan.isEnabled = true
                            }
                            return@withContext null
                        }
                        RetrofitClient.apiService.createGuru(
                            "Bearer $token",
                            CreateGuruRequest(email, password, nama, nip)
                        )
                    } ?: return@launch

                    if (response.isSuccessful) {
                        Toast.makeText(context, "Guru berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Gagal menambahkan guru: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    btnSimpan.isEnabled = true
                }
            }
        }
    }
}
