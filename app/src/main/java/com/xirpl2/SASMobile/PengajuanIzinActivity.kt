package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PengajuanIzinActivity : BaseActivity() {

    // Form fields
    private lateinit var rgPermitType: RadioGroup
    private lateinit var rbSakit: RadioButton
    private lateinit var rbIzin: RadioButton
    private lateinit var tilStartDate: TextInputLayout
    private lateinit var etStartDate: EditText
    private lateinit var tilEndDate: TextInputLayout
    private lateinit var etEndDate: EditText
    private lateinit var tilReason: TextInputLayout
    private lateinit var etReason: EditText

    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var btnMenu: ImageView
    private lateinit var btnBack: ImageView

    // Photo upload
    private lateinit var layoutUploadPhoto: FrameLayout
    private lateinit var layoutUploadPlaceholder: LinearLayout
    private lateinit var layoutPhotoPreview: LinearLayout
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnRemovePhoto: com.google.android.material.button.MaterialButton
    private lateinit var tvPhotoLabel: TextView
    private var selectedPhotoUri: Uri? = null

    private val calendar = Calendar.getInstance()

    // Photo picker launcher
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            showPhotoPreview(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_izin)

        initializeViews()
        setupPermitTypes()
        setupDatePickers()
        setupMenu()
        setupSubmitButton()
        setupBackButton()
        setupCancelButton()
        setupPhotoUpload()
    }

    private fun initializeViews() {
        rgPermitType = findViewById(R.id.rgPermitType)
        rbSakit = findViewById(R.id.rbSakit)
        rbIzin = findViewById(R.id.rbIzin)
        tilStartDate = findViewById(R.id.tilStartDate)
        etStartDate = findViewById(R.id.etStartDate)
        tilEndDate = findViewById(R.id.tilEndDate)
        etEndDate = findViewById(R.id.etEndDate)
        tilReason = findViewById(R.id.tilReason)
        etReason = findViewById(R.id.etReason)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnCancel = findViewById(R.id.btnCancel)
        btnMenu = findViewById(R.id.btnMenu)
        btnBack = findViewById(R.id.btnBack)

        // Photo upload views
        layoutUploadPhoto = findViewById(R.id.layoutUploadPhoto)
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder)
        layoutPhotoPreview = findViewById(R.id.layoutPhotoPreview)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        tvPhotoLabel = findViewById(R.id.tvPhotoLabel)
    }

    private fun setupPhotoUpload() {
        layoutUploadPhoto.setOnClickListener {
            if (selectedPhotoUri == null) {
                photoPickerLauncher.launch("image/*")
            }
        }

        btnRemovePhoto.setOnClickListener {
            selectedPhotoUri = null
            layoutUploadPlaceholder.visibility = View.VISIBLE
            layoutPhotoPreview.visibility = View.GONE
        }
    }

    private fun showPhotoPreview(uri: Uri) {
        ivPhotoPreview.setImageURI(uri)
        layoutUploadPlaceholder.visibility = View.GONE
        layoutPhotoPreview.visibility = View.VISIBLE
    }

    private fun setupPermitTypes() {
        rgPermitType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSakit) {
                tvPhotoLabel.text = "BUKTI FOTO (WAJIB)"
                tvPhotoLabel.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                tvPhotoLabel.text = "BUKTI FOTO (OPSIONAL)"
                tvPhotoLabel.setTextColor(getColor(R.color.slate_500))
            }
        }
    }

    private fun setupDatePickers() {
        val startDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            etStartDate.setText(selectedDate)
            tilStartDate.error = null
        }

        val endDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            etEndDate.setText(selectedDate)
            tilEndDate.error = null
        }

        etStartDate.setOnClickListener {
            val startDatePicker = DatePickerDialog(
                this,
                startDateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            startDatePicker.datePicker.minDate = calendar.timeInMillis - 1000
            startDatePicker.show()
        }

        etEndDate.setOnClickListener {
            val endDatePicker = DatePickerDialog(
                this,
                endDateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            endDatePicker.datePicker.minDate = calendar.timeInMillis - 1000
            endDatePicker.show()
        }
    }

    private fun setupMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_menu, null)
        val navView = bottomSheetView.findViewById<NavigationView>(R.id.navView)

        bottomSheetDialog.setContentView(bottomSheetView)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, BerandaActivity::class.java))
                    finish()
                }
                R.id.nav_presensi -> {
                    Toast.makeText(this, "Fitur dalam pengembangan", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_pengajuan_izin -> {
                    // Already here
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, PengaturanAkunActivity::class.java))
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            bottomSheetDialog.dismiss()
            true
        }

        btnMenu.setOnClickListener {
            bottomSheetDialog.show()
        }
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupCancelButton() {
        btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitPermitRequest()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (rgPermitType.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Pilih jenis perizinan", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        val startDate = etStartDate.text.toString().trim()
        if (startDate.isEmpty()) {
            tilStartDate.error = "Pilih tanggal mulai"
            isValid = false
        } else {
            tilStartDate.error = null
        }

        val endDate = etEndDate.text.toString().trim()
        if (endDate.isEmpty()) {
            tilEndDate.error = "Pilih tanggal berakhir"
            isValid = false
        } else {
            tilEndDate.error = null
        }

        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            try {
                val start = parseDate(startDate)
                val end = parseDate(endDate)
                if (end < start) {
                    tilEndDate.error = "Tanggal akhir harus setelah mulai"
                    isValid = false
                }
            } catch (e: Exception) {
                tilEndDate.error = "Format tanggal tidak valid"
                isValid = false
            }
        }

        val reason = etReason.text.toString().trim()
        if (reason.isEmpty()) {
            tilReason.error = "Isi alasan perizinan"
            isValid = false
        } else if (reason.length < 10) {
            tilReason.error = "Alasan minimal 10 karakter"
            isValid = false
        } else {
            tilReason.error = null
        }

        if (rbSakit.isChecked && selectedPhotoUri == null) {
            Toast.makeText(this, "Lampirkan bukti foto (surat dokter/resep) untuk izin sakit", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun parseDate(dateStr: String): Calendar {
        val parts = dateStr.split("-")
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        return cal
    }

    private fun submitPermitRequest() {
        setLoading(true)

        val jenisIzin = if (rbSakit.isChecked) "sakit" else "izin"
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()
        val reason = etReason.text.toString().trim()

        lifecycleScope.launch {
            try {
                val token = getSharedPreferences("UserData", MODE_PRIVATE)
                    .getString("auth_token", "") ?: ""
                
                val jenisIzinBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, jenisIzin)
                val startDateBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, startDate)
                val endDateBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, endDate)
                val reasonBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, reason)

                // Prepare photo part if selected
                val photoPart = preparePhotoPart()

                val response = RetrofitClient.apiService.createPengajuanIzin(
                    token = "Bearer $token",
                    jenisIzin = jenisIzinBody,
                    tanggalAwal = startDateBody,
                    tanggalAkhir = endDateBody,
                    keterangan = reasonBody,
                    buktiFoto = photoPart
                )

                setLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        this@PengajuanIzinActivity,
                        response.body()?.message ?: "Pengajuan izin berhasil dikirim",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Semua field wajib diisi atau jenis izin tidak valid"
                        409 -> "Pengajuan tumpang-tindih dengan periode yang sudah diajukan"
                        else -> "Gagal mengirim pengajuan: ${response.message()}"
                    }
                    Toast.makeText(this@PengajuanIzinActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(
                    this@PengajuanIzinActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun preparePhotoPart(): MultipartBody.Part? {
        val uri = selectedPhotoUri ?: return null
        
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("bukti_foto_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("bukti_foto", tempFile.name, requestBody)
        } catch (e: Exception) {
            android.util.Log.w("PengajuanIzin", "Failed to prepare photo: ${e.message}")
            null
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnSubmit.isEnabled = !isLoading
        btnSubmit.text = if (isLoading) "Mengirim..." else getString(R.string.kirim_pengajuan)
        
        rbSakit.isEnabled = !isLoading
        rbIzin.isEnabled = !isLoading
        etStartDate.isEnabled = !isLoading
        etEndDate.isEnabled = !isLoading
        etReason.isEnabled = !isLoading
        btnCancel.isEnabled = !isLoading
        layoutUploadPhoto.isClickable = !isLoading
    }

    private fun logout() {
        val token = getSharedPreferences("UserData", MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
        
        lifecycleScope.launch {
            try {
                if (token.isNotEmpty()) {
                    RetrofitClient.apiService.logout("Bearer $token")
                }
            } catch (_: Exception) { }
            
            // Clear ALL SharedPreferences stores
            with(getSharedPreferences("UserData", MODE_PRIVATE).edit()) {
                clear()
                apply()
            }
            with(getSharedPreferences("user_session", MODE_PRIVATE).edit()) {
                clear()
                apply()
            }
            with(getSharedPreferences("NotificationData", MODE_PRIVATE).edit()) {
                clear()
                apply()
            }

            val intent = Intent(this@PengajuanIzinActivity, MasukActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}