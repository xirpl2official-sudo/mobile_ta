package com.xirpl2.SASMobile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.model.ChangePasswordRequest
import com.xirpl2.SASMobile.model.DeviceChangeRequestBody
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengaturanAkunActivity : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var cardProfilePhoto: CardView
    private lateinit var tvInitial: TextView
    private lateinit var tvFotoProfilLabel: TextView
    private lateinit var etNIS: TextView
    private lateinit var etNamaLengkap: TextView
    private lateinit var tvJenisKelamin: TextView
    private lateinit var etEmail: TextView
    private lateinit var tvChangeEmail: com.google.android.material.button.MaterialButton
    private lateinit var tilNIS: View
    private lateinit var tilNamaLengkap: View
    private lateinit var tilJenisKelamin: View
    private lateinit var tilEmail: View
    private lateinit var tilChangeEmail: View
    private lateinit var cardDeviceChange: View
    
    private var currentUserData: UserData? = null
    private var selectedImageUri: Uri? = null
    private var userRole: String = ""
    
    private val TAG = "PengaturanAkunActivity"
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                showPhotoPreview(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengaturan_akun)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val topBar = findViewById<View>(R.id.topBarContent)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        initializeViews()

        val sessionPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        userRole = sessionPref.getString("user_role", "")?.lowercase() ?: ""
        applyRoleBasedVisibility()

        loadUserDataFromAPI()

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadUserDataFromAPI()
            swipeRefresh.isRefreshing = false
        }

        setupListeners()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        cardProfilePhoto = findViewById(R.id.cardProfilePhoto)
        tvInitial = findViewById(R.id.tvInitial)
        tvFotoProfilLabel = findViewById(R.id.tvFotoProfilLabel)
        etNIS = findViewById(R.id.etNIS)
        etNamaLengkap = findViewById(R.id.etNamaLengkap)
        tvJenisKelamin = findViewById(R.id.tvJenisKelamin)
        etEmail = findViewById(R.id.etEmail)
        tvChangeEmail = findViewById(R.id.tilChangeEmail)
        tilNIS = findViewById(R.id.tilNIS)
        tilNamaLengkap = findViewById(R.id.tilNamaLengkap)
        tilJenisKelamin = findViewById(R.id.tilJenisKelamin)
        tilEmail = findViewById(R.id.tilEmail)
        tilChangeEmail = findViewById(R.id.tilChangeEmail)
        cardDeviceChange = findViewById(R.id.cardDeviceChange)
    }

    private fun applyRoleBasedVisibility() {
        when {
            userRole.contains("admin") -> {
                tilNIS.visibility = View.GONE
                tilNamaLengkap.visibility = View.VISIBLE
                tilJenisKelamin.visibility = View.GONE
                tilEmail.visibility = View.VISIBLE
                tilChangeEmail.visibility = View.VISIBLE
                cardDeviceChange.visibility = View.GONE
            }
            userRole.contains("wali") -> {
                tilNIS.visibility = View.VISIBLE
                tilNamaLengkap.visibility = View.VISIBLE
                tilJenisKelamin.visibility = View.GONE
                tilEmail.visibility = View.VISIBLE
                tilChangeEmail.visibility = View.VISIBLE
                cardDeviceChange.visibility = View.GONE
            }
            userRole.contains("guru") -> {
                tilNIS.visibility = View.GONE
                tilNamaLengkap.visibility = View.VISIBLE
                tilJenisKelamin.visibility = View.GONE
                tilEmail.visibility = View.VISIBLE
                tilChangeEmail.visibility = View.VISIBLE
                cardDeviceChange.visibility = View.GONE
            }
            else -> {
                tilNIS.visibility = View.VISIBLE
                tilNamaLengkap.visibility = View.VISIBLE
                tilJenisKelamin.visibility = View.VISIBLE
                tilEmail.visibility = View.VISIBLE
                tilChangeEmail.visibility = View.VISIBLE
                cardDeviceChange.visibility = View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        
        btnBack.setOnClickListener {
            finish()
        }
        
        
        cardProfilePhoto.setOnClickListener {
            showPhotoPickerDialog()
        }
        
        
        // Change password
        tvChangeEmail.setOnClickListener {
            showChangeEmailDialog()
        }
        
        // Change password button
        val btnChangePassword = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChangePassword)
        btnChangePassword?.setOnClickListener {
            showChangePasswordDialog()
        }

        // Device change request button (only visible for siswa)
        val btnDeviceChangeRequest = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeviceChangeRequest)
        btnDeviceChangeRequest?.setOnClickListener {
            showDeviceChangeDialog()
        }
    }

    private fun loadUserDataFromAPI() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@PengaturanAkunActivity)
                val token = sharedPref.getString("auth_token", null)

                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PengaturanAkunActivity, "Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getProfile("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val profileData = body?.data

                        if (profileData != null) {
                            // Fix: Gunakan properti yang tersedia di AkunLoginResponse
                            currentUserData = UserData(
                                nis = profileData.nis ?: "",
                                namaLengkap = profileData.nama ?: profileData.nama_siswa ?: "",
                                jenisKelamin = profileData.jk ?: "L",
                                email = profileData.email ?: "",
                                fotoProfil = null,
                                jurusan = profileData.jurusan ?: "",
                                kelas = profileData.kelas ?: "",
                                isGoogleAccount = false  // is_google_acct tidak ada di response
                            )
                            
                            
                            saveUserDataLocally(currentUserData!!)
                            
                            
                            populateFields(currentUserData!!)
                        }
                    } else {
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            "Gagal mengambil data profil",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        
                        loadUserDataFromPreferences()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PengaturanAkunActivity,
                        "Kesalahan: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    
                    loadUserDataFromPreferences()
                }
            }
        }
    }

    private fun loadUserDataFromPreferences() {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
        
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
        
        
        if (currentUserData != null) {
            populateFields(currentUserData!!)
        }
    }

    private fun saveUserDataLocally(userData: UserData) {
        val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
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

    private fun populateFields(userData: UserData) {
        etNIS.text = userData.nis
        etNamaLengkap.text = userData.namaLengkap
        etEmail.text = userData.email
        
        
        val jenisKelaminText = if (userData.jenisKelamin == "P") "Perempuan" else "Laki - laki"
        tvJenisKelamin.text = jenisKelaminText
        
        
        if (userData.namaLengkap.isNotEmpty()) {
            tvInitial.text = userData.namaLengkap.first().uppercase()
            tvFotoProfilLabel.text = userData.namaLengkap
        }
    }

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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showPhotoPreview(imageUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    // First pass: decode bounds only
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeStream(inputStream, null, boundsOptions)

                    // Calculate down-sample size to avoid OOM on large images
                    val targetSize = 512
                    val scale = maxOf(1, maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / targetSize)

                    // Second pass: decode with down-sampling
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = scale
                    }
                    contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        // Remove any previously added photo preview
                        val existingPhoto = cardProfilePhoto.findViewWithTag<ImageView>("photo_preview")
                        existingPhoto?.let { cardProfilePhoto.removeView(it) }

                        // Create an ImageView to display the photo inside the circular CardView
                        val photoView = ImageView(this@PengaturanAkunActivity).apply {
                            tag = "photo_preview"
                            layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageBitmap(bitmap)
                        }
                        cardProfilePhoto.addView(photoView, 0)
                        tvInitial.visibility = View.GONE

                        // Photo is shown locally but upload is not yet implemented on the server
                        Toast.makeText(this@PengaturanAkunActivity, "Foto ditampilkan sebagai pratinjau. Upload foto profil belum tersedia.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@PengaturanAkunActivity, "Gagal memuat foto", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PengaturanAkunActivity, "Gagal memuat foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showChangeEmailDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ubah_email, null)
        
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etEmailDialog = dialogView.findViewById<EditText>(R.id.etEmailDialog)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatalDialog)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpanDialog)
        
        etEmailDialog.setText(currentUserData?.email)
        
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
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etEmailDialog.error = "Format email tidak valid"
                return@setOnClickListener
            }
            
            updateEmail(newEmail)
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    private fun updateEmail(newEmail: String) {
        val currentData = currentUserData ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@PengaturanAkunActivity)
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


                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PengaturanAkunActivity,
                        "Memperbarui email...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val request = com.xirpl2.SASMobile.model.ChangeEmailRequest(newEmail = newEmail)

                val response = RetrofitClient.apiService.changeEmail("Bearer $token", request)
                
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!

                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            apiResponse.message ?: "Kode OTP telah dikirim ke email baru Anda",
                            Toast.LENGTH_LONG
                        ).show()

                        // Show OTP verification dialog
                        showEmailOtpDialog(token, newEmail)

                    } else {
                        val errorBody = response.errorBody()?.string()

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

    private fun showEmailOtpDialog(token: String, newEmail: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_otp_input, null)
        val etOtp = dialogView.findViewById<EditText>(R.id.etOtp)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Verifikasi OTP")
            .setMessage("Masukkan kode OTP yang telah dikirim ke $newEmail")
            .setView(dialogView)
            .setPositiveButton("Verifikasi", null)
            .setNeutralButton("Kirim Ulang", null)
            .setNegativeButton("Batal", null)
            .setCancelable(false)
            .create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.isEmpty()) {
                etOtp.error = "Kode OTP tidak boleh kosong"
                return@setOnClickListener
            }

            it.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val verifyRequest = com.xirpl2.SASMobile.model.VerifyEmailOTPRequest(
                        newEmail = newEmail,
                        otp = otp
                    )
                    val verifyResponse = RetrofitClient.apiService.verifyChangeEmail("Bearer $token", verifyRequest)

                    withContext(Dispatchers.Main) {
                        if (verifyResponse.isSuccessful) {
                            Toast.makeText(
                                this@PengaturanAkunActivity,
                                verifyResponse.body()?.message ?: "Email berhasil diubah",
                                Toast.LENGTH_SHORT
                            ).show()
                            etEmail.text = newEmail
                            currentUserData = currentUserData?.copy(email = newEmail)
                            val sessionPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@PengaturanAkunActivity)
                            sessionPref.edit().putString("user_email", newEmail).apply()
                            val dataPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@PengaturanAkunActivity)
                            dataPref.edit().putString("email", newEmail).apply()
                            alertDialog.dismiss()
                        } else {
                            it.isEnabled = true
                            val errorBody = verifyResponse.errorBody()?.string()
                            val msg = try {
                                if (!errorBody.isNullOrEmpty()) org.json.JSONObject(errorBody).optString("message", "Verifikasi gagal")
                                else "Verifikasi gagal (${verifyResponse.code()})"
                            } catch (_: Exception) { "Verifikasi gagal (${verifyResponse.code()})" }
                            Toast.makeText(this@PengaturanAkunActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        it.isEnabled = true
                        Toast.makeText(this@PengaturanAkunActivity, "Kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            etOtp.text.clear()
            resendEmailOtp(token, newEmail)
        }

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            Toast.makeText(
                this,
                "OTP masih aktif. Klik 'Ganti Email' lagi untuk memasukkan kode OTP",
                Toast.LENGTH_LONG
            ).show()
            alertDialog.dismiss()
        }
    }

    private fun resendEmailOtp(token: String, newEmail: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = com.xirpl2.SASMobile.model.ChangeEmailRequest(newEmail = newEmail)
                val response = RetrofitClient.apiService.changeEmail("Bearer $token", request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            "Kode OTP baru telah dikirim ke $newEmail",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PengaturanAkunActivity,
                            "Gagal mengirim ulang OTP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PengaturanAkunActivity, "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class UserData(
        val nis: String,
        val namaLengkap: String,
        val jenisKelamin: String,
        val email: String,
        val fotoProfil: String? = null,
        val jurusan: String = "",
        val kelas: String = "",
        val isGoogleAccount: Boolean = false
    )
    
    private fun showChangePasswordDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_ubah_password, null)
        
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val tilCurrentPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCurrentPassword)
        val tilNewPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilNewPassword)
        val tilConfirmPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirmPassword)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClosePassword)
        val btnBatal = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatalPassword)
        val btnSimpan = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSimpanPassword)
        
        btnClose.setOnClickListener { alertDialog.dismiss() }
        btnBatal.setOnClickListener { alertDialog.dismiss() }
        
        btnSimpan.setOnClickListener {
            val currentPwd = etCurrentPassword.text.toString()
            val newPwd = etNewPassword.text.toString()
            val confirmPwd = etConfirmPassword.text.toString()
            
            // Validate
            var isValid = true
            
            if (currentPwd.isEmpty()) {
                tilCurrentPassword.error = "Wajib diisi"
                isValid = false
            } else {
                tilCurrentPassword.error = null
            }
            
            if (newPwd.isEmpty()) {
                tilNewPassword.error = "Wajib diisi"
                isValid = false
            } else if (newPwd.length < 8) {
                tilNewPassword.error = "Minimal 8 karakter"
                isValid = false
            } else {
                tilNewPassword.error = null
            }
            
            if (confirmPwd != newPwd) {
                tilConfirmPassword.error = "Kata sandi tidak cocok"
                isValid = false
            } else {
                tilConfirmPassword.error = null
            }
            
            if (!isValid) return@setOnClickListener
            
            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."
            
            changePassword(currentPwd, newPwd) { success, message ->
                runOnUiThread {
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "Simpan"
                    
                    if (success) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        alertDialog.dismiss()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        alertDialog.show()
    }
    
    private fun changePassword(currentPassword: String, newPassword: String, callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@PengaturanAkunActivity)
                val token = sharedPref.getString("auth_token", null)
                    ?: com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@PengaturanAkunActivity)
                        .getString("auth_token", null)
                
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Token tidak ditemukan. Silakan login kembali")
                    }
                    return@launch
                }
                
                val request = ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
                
                val response = RetrofitClient.apiService.changePassword("Bearer $token", request)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(true, response.body()?.message ?: "Kata sandi berhasil diubah")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                org.json.JSONObject(errorBody).optString("message", "Gagal mengubah kata sandi")
                            } else {
                                when (response.code()) {
                                    400 -> "Format data tidak valid"
                                    401 -> "Kata sandi saat ini salah"
                                    404 -> "Akun tidak ditemukan"
                                    else -> "Gagal mengubah kata sandi (${response.code()})"
                                }
                            }
                        } catch (e: Exception) {
                            "Gagal mengubah kata sandi (${response.code()})"
                        }
                        callback(false, errorMessage)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Koneksi timeout"
                        else -> "Terjadi kesalahan: ${e.message}"
                    }
                    callback(false, errorMsg)
                }
            }
        }
    }

    private fun showDeviceChangeDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_device_change, null)
        
        dialogBuilder.setView(dialogView)
        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val etAlasan = dialogView.findViewById<EditText>(R.id.etAlasan)
        val tilAlasan = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilAlasan)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseDevice)
        val btnBatal = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatalDevice)
        val btnKirim = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnKirimDevice)
        
        btnClose.setOnClickListener { alertDialog.dismiss() }
        btnBatal.setOnClickListener { alertDialog.dismiss() }
        
        btnKirim.setOnClickListener {
            val alasan = etAlasan.text.toString().trim()
            
            if (alasan.isEmpty()) {
                tilAlasan.error = "Wajib diisi"
                return@setOnClickListener
            } else if (alasan.length < 10) {
                tilAlasan.error = "Minimal 10 karakter"
                return@setOnClickListener
            } else {
                tilAlasan.error = null
            }
            
            btnKirim.isEnabled = false
            btnKirim.text = "Mengirim..."
            
            submitDeviceChangeRequest(alasan) { success, message ->
                runOnUiThread {
                    btnKirim.isEnabled = true
                    btnKirim.text = "Kirim Pengajuan"
                    
                    if (success) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        alertDialog.dismiss()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        alertDialog.show()
    }

    private fun submitDeviceChangeRequest(alasan: String, callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPref = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this@PengaturanAkunActivity)
                val token = sharedPref.getString("auth_token", null)
                    ?: com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@PengaturanAkunActivity)
                        .getString("auth_token", null)
                
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Token tidak ditemukan. Silakan login kembali")
                    }
                    return@launch
                }
                
                // Get current device ID
                val hardwareId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                
                val request = DeviceChangeRequestBody(
                    newHardwareId = hardwareId,
                    alasan = alasan
                )
                
                val response = RetrofitClient.apiService.createDeviceChangeRequest("Bearer $token", request)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        callback(true, response.body()?.message ?: "Pengajuan berhasil dikirim")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            if (!errorBody.isNullOrEmpty()) {
                                org.json.JSONObject(errorBody).optString("message", "Gagal mengirim pengajuan")
                            } else {
                                when (response.code()) {
                                    400 -> "Format data tidak valid"
                                    404 -> "Perangkat saat ini tidak ditemukan"
                                    409 -> "Anda sudah memiliki pengajuan yang belum diproses"
                                    else -> "Gagal mengirim pengajuan (${response.code()})"
                                }
                            }
                        } catch (e: Exception) {
                            "Gagal mengirim pengajuan (${response.code()})"
                        }
                        callback(false, errorMessage)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = when (e) {
                        is java.net.UnknownHostException -> "Tidak dapat terhubung ke server"
                        is java.net.SocketTimeoutException -> "Koneksi timeout"
                        else -> "Terjadi kesalahan: ${e.message}"
                    }
                    callback(false, errorMsg)
                }
            }
        }
    }
}
