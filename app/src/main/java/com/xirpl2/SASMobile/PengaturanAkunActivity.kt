package com.xirpl2.SASMobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.model.UserProfile
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PengaturanAkunActivity : AppCompatActivity() {

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var cardProfilePhoto: CardView
    private lateinit var tvInitial: TextView
    private lateinit var etNIS: TextView
    private lateinit var etNamaLengkap: TextView
    private lateinit var tvJenisKelamin: TextView
    private lateinit var etEmail: TextView
    private lateinit var tvChangeEmail: TextView
    
    // Data
    private var currentUserData: UserData? = null
    private var selectedImageUri: Uri? = null
    
    private val TAG = "PengaturanAkunActivity"
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                // TODO: Update foto profil preview
                Toast.makeText(this, "Foto dipilih (preview segera menyusul)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaturan_akun)

        // Initialize Views
        initializeViews()
        
        // Setup Spinner
        setupJenisKelaminSpinner()
        
        // Load User Data from API
        loadUserDataFromAPI()
        
        // Setup Click Listeners
        setupListeners()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        cardProfilePhoto = findViewById(R.id.cardProfilePhoto)
        tvInitial = findViewById(R.id.tvInitial)
        etNIS = findViewById(R.id.etNIS)
        etNamaLengkap = findViewById(R.id.etNamaLengkap)
        tvJenisKelamin = findViewById(R.id.tvJenisKelamin)
        etEmail = findViewById(R.id.etEmail)
        tvChangeEmail = findViewById(R.id.tvChangeEmail)
    }

    private fun setupJenisKelaminSpinner() {
        // No longer needed as we use TextView
    }

    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }
        
        // Profile photo click
        cardProfilePhoto.setOnClickListener {
            showPhotoPickerDialog()
        }
        
        // Change email link
        tvChangeEmail.setOnClickListener {
            showChangeEmailDialog()
        }
    }

    /**
     * Load user data dari API dengan Bearer token
     */
    private fun loadUserDataFromAPI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val token = sharedPref.getString("auth_token", null)

                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PengaturanAkunActivity, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                Log.d(TAG, "Fetching user profile with token: Bearer ${token.take(10)}...")
                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val profileData = body?.data

                        if (profileData != null) {
                            Log.d(TAG, "Profile data received: ${profileData.nama_siswa}")
                            
                            // Convert UserProfile to UserData
                            currentUserData = UserData(
                                nis = profileData.nis ?: "",
                                namaLengkap = profileData.name ?: profileData.username ?: profileData.nama_siswa ?: "",
                                jenisKelamin = profileData.jk,
                                email = profileData.email,
                                fotoProfil = null,
                                jurusan = profileData.jurusan ?: "",
                                kelas = profileData.kelas ?: "",
                                isGoogleAccount = profileData.is_google_acct
                            )
                            
                            // Save to SharedPreferences for future use
                            saveUserDataLocally(currentUserData!!)
                            
                            // Populate fields
                            populateFields(currentUserData!!)
                        }
                    } else {
                        Log.e(TAG, "Error fetching profile: ${response.code()}")
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            "Gagal mengambil data profil",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Fallback to SharedPreferences data
                        loadUserDataFromPreferences()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PengaturanAkunActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Fallback to SharedPreferences data
                    loadUserDataFromPreferences()
                }
            }
        }
    }

    /**
     * Load user data dari SharedPreferences (fallback)
     */
    private fun loadUserDataFromPreferences() {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        
        currentUserData = UserData(
            nis = sharedPref.getString("user_nis", "") ?: "",
            namaLengkap = sharedPref.getString("user_name", "") ?: "",
            jenisKelamin = sharedPref.getString("user_jk", "L") ?: "L",
            email = sharedPref.getString("user_email", "") ?: "",
            fotoProfil = null,
            jurusan = sharedPref.getString("user_jurusan", "") ?: "",
            kelas = sharedPref.getString("user_kelas", "") ?: "",
            isGoogleAccount = false
        )
        
        // Populate fields
        if (currentUserData != null) {
            populateFields(currentUserData!!)
        }
    }

    /**
     * Save user data to SharedPreferences
     */
    private fun saveUserDataLocally(userData: UserData) {
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_nis", userData.nis)
            putString("user_name", userData.namaLengkap)
            putString("user_jk", userData.jenisKelamin)
            putString("user_kelas", userData.kelas)
            putString("user_jurusan", userData.jurusan)
            putString("user_email", userData.email)
            apply()
        }
    }

    /**
     * Populate form fields dengan data user (read-only)
     */
    private fun populateFields(userData: UserData) {
        etNIS.text = userData.nis
        etNamaLengkap.text = userData.namaLengkap
        etEmail.text = userData.email
        
        // Set jenis kelamin
        val jenisKelaminText = if (userData.jenisKelamin == "P") "Perempuan" else "Laki - laki"
        tvJenisKelamin.text = jenisKelaminText
        
        // Set initial dari nama
        if (userData.namaLengkap.isNotEmpty()) {
            tvInitial.text = userData.namaLengkap.first().uppercase()
        }
    }

    /**
     * Show dialog untuk memilih foto profil
     */
    private fun showPhotoPickerDialog() {
        val options = arrayOf("Pilih dari Galeri", "Batal")
        AlertDialog.Builder(this)
            .setTitle("Ganti Foto Profil")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * Open image picker
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    /**
     * Show dialog untuk change email dengan custom layout
     */
    private fun showChangeEmailDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ubah_email, null)
        
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Initialize dialog views
        val etEmailDialog = dialogView.findViewById<EditText>(R.id.etEmailDialog)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatalDialog)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpanDialog)
        
        // Set current email
        etEmailDialog.setText(currentUserData?.email)
        
        // Setup listeners
        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }
        
        btnBatal.setOnClickListener {
            alertDialog.dismiss()
        }
        
        btnSimpan.setOnClickListener {
            val newEmail = etEmailDialog.text.toString().trim()
            
            if (newEmail.isEmpty()) {
                etEmailDialog.error = "Email tidak boleh kosong"
                return@setOnClickListener
            }
            
            if (!newEmail.contains("@")) {
                etEmailDialog.error = "Email tidak valid"
                return@setOnClickListener
            }
            
            // Update data
            updateEmail(newEmail)
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    private fun updateEmail(newEmail: String) {
        val currentData = currentUserData ?: return
        
        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get auth token
                val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val token = sharedPref.getString("auth_token", null)
                
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            "Token tidak ditemukan. Silakan login kembali",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }
                
                Log.d(TAG, "Updating email to: $newEmail")
                
                // Show loading
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PengaturanAkunActivity,
                        "Memperbarui email...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Make API request
                val request = com.xirpl2.SASMobile.model.ChangeEmailRequest(email = newEmail)
                Log.d(TAG, "========== CHANGE EMAIL REQUEST ==========")
                Log.d(TAG, "Request email: $newEmail")
                Log.d(TAG, "Request object: $request")
                
                val response = RetrofitClient.apiService.changeEmail("Bearer $token", request)
                
                Log.d(TAG, "========== CHANGE EMAIL RESPONSE ==========")
                Log.d(TAG, "Response code: ${response.code()}")
                Log.d(TAG, "Response successful: ${response.isSuccessful}")
                Log.d(TAG, "Response body: ${response.body()}")
                Log.d(TAG, "Response error body: ${response.errorBody()?.string()}")
                Log.d(TAG, "==========================================")
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        Log.d(TAG, "Email updated successfully: ${apiResponse.message}")
                        
                        // Update local data
                        val updatedData = currentData.copy(email = newEmail)
                        currentUserData = updatedData
                        
                        // Save to SharedPreferences
                        sharedPref.edit().putString("user_email", newEmail).apply()
                        
                        // Update UI
                        etEmail.text = newEmail
                        
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            apiResponse.message ?: "Email berhasil diubah",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Parse error response
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error updating email: $errorBody")
                        
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                val jsonObject = org.json.JSONObject(errorBody)
                                jsonObject.optString("message", "Gagal mengubah email")
                            } else {
                                when (response.code()) {
                                    400 -> "Email tidak valid"
                                    401 -> "Sesi Anda telah berakhir. Silakan login kembali"
                                    409 -> "Email sudah digunakan akun lain"
                                    500 -> "Server sedang bermasalah"
                                    else -> "Gagal mengubah email (${response.code()})"
                                }
                            }
                        } catch (e: Exception) {
                            "Gagal mengubah email (${response.code()})"
                        }
                        
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating email: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Koneksi timeout. Periksa internet Anda"
                        else -> "Terjadi kesalahan: ${e.message}"
                    }
                    
                    Toast.makeText(
                        this@PengaturanAkunActivity,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    /**
     * Data class untuk user data
     */
    data class UserData(
        val nis: String,
        val namaLengkap: String,
        val jenisKelamin: String, // "L" atau "P"
        val email: String,
        val fotoProfil: String? = null,
        val jurusan: String = "",
        val kelas: String = "",
        val isGoogleAccount: Boolean = false
    )
}
