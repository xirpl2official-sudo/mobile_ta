package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.adapter.RiwayatIzinAdapter
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.PengajuanIzinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PengajuanIzinActivity : BaseSiswaActivity() {

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
    
    // Photo upload
    private lateinit var layoutUploadPhoto: FrameLayout
    private lateinit var layoutUploadPlaceholder: LinearLayout
    private lateinit var layoutPhotoPreview: LinearLayout
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnRemovePhoto: com.google.android.material.button.MaterialButton
    private lateinit var tvPhotoLabel: TextView
    private var selectedPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null

    // Riwayat
    private lateinit var rvRiwayatIzin: RecyclerView
    private lateinit var progressRiwayat: ProgressBar
    private lateinit var tvEmptyRiwayat: TextView
    private lateinit var riwayatAdapter: RiwayatIzinAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val repository = PengajuanIzinRepository()

    private val calendar = Calendar.getInstance()
    private var startDateMillis: Long = 0L

    // Photo picker launcher
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            showPhotoPreview(it)
        }
    }

    override fun getCurrentMenuItem(): SiswaMenuItem = SiswaMenuItem.PENGAJUAN_IZIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_izin)
        setupStatusBar()

        findViewById<android.view.View>(R.id.topBarContent)?.let { topBar ->
            applyEdgeToEdge(topBar)
        }

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupPermitTypes()
        setupDatePickers()
        setupSubmitButton()
        setupCancelButton()
        setupPhotoUpload()
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadRiwayatIzin() }

        loadRiwayatIzin()

        // Hardware back press navigates to Beranda
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@PengajuanIzinActivity, BerandaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        })
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

        // Photo upload views
        layoutUploadPhoto = findViewById(R.id.layoutUploadPhoto)
        layoutUploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder)
        layoutPhotoPreview = findViewById(R.id.layoutPhotoPreview)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        tvPhotoLabel = findViewById(R.id.tvPhotoLabel)

        // Riwayat views
        rvRiwayatIzin = findViewById(R.id.rvRiwayatIzin)
        progressRiwayat = findViewById(R.id.progressRiwayat)
        tvEmptyRiwayat = findViewById(R.id.tvEmptyRiwayat)
        riwayatAdapter = RiwayatIzinAdapter()
        rvRiwayatIzin.layoutManager = LinearLayoutManager(this)
        rvRiwayatIzin.adapter = riwayatAdapter
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
                tvPhotoLabel.setTextColor(getColor(R.color.status_error))
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

            // Store start date millis for end date picker minimum
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            startDateMillis = cal.timeInMillis

            // Clear end date if it is now before the new start date
            val endText = etEndDate.text.toString().trim()
            if (endText.isNotEmpty()) {
                try {
                    val endCal = parseDate(endText)
                    if (endCal.timeInMillis < startDateMillis) {
                        etEndDate.text.clear()
                        tilEndDate.error = null
                    }
                } catch (_: Exception) {
                    etEndDate.text.clear()
                    tilEndDate.error = null
                }
            }
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
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            startDatePicker.datePicker.minDate = calendar.timeInMillis
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
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            // Use selected start date as minimum, fall back to today
            endDatePicker.datePicker.minDate = if (startDateMillis > 0L) startDateMillis else calendar.timeInMillis
            endDatePicker.show()
        }
    }

    private fun setupCancelButton() {
        btnCancel.setOnClickListener {
            val intent = Intent(this@PengajuanIzinActivity, BerandaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this@PengajuanIzinActivity)
                    .getString("auth_token", "") ?: ""
                
                val jenisIzinBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, jenisIzin)
                val startDateBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, startDate)
                val endDateBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, endDate)
                val reasonBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, reason)

                // Prepare photo part on IO thread
                val photoPart = preparePhotoPart()

                val response = RetrofitClient.apiService.createPengajuanIzin(
                    token = "Bearer $token",
                    jenisIzin = jenisIzinBody,
                    tanggalAwal = startDateBody,
                    tanggalAkhir = endDateBody,
                    keterangan = reasonBody,
                    buktiFoto = photoPart
                )

                deleteTempPhotoFile()

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    setLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@PengajuanIzinActivity,
                            response.body()?.message ?: "Pengajuan izin berhasil dikirim",
                            Toast.LENGTH_LONG
                        ).show()
                        resetForm()
                        loadRiwayatIzin()
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> "Semua field wajib diisi atau jenis izin tidak valid"
                            409 -> "Pengajuan tumpang-tindih dengan periode yang sudah diajukan"
                            else -> "Gagal mengirim pengajuan: ${response.message()}"
                        }
                        Toast.makeText(this@PengajuanIzinActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                deleteTempPhotoFile()
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    setLoading(false)
                    Toast.makeText(
                        this@PengajuanIzinActivity,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun preparePhotoPart(): MultipartBody.Part? {
        val uri = selectedPhotoUri ?: return null

        return try {
            // Clean up any previous temp file
            deleteTempPhotoFile()

            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("bukti_foto_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            tempPhotoFile = tempFile
            val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("bukti_foto", tempFile.name, requestBody)
        } catch (e: Exception) {
            android.util.Log.w("PengajuanIzin", "Failed to prepare photo: ${e.message}")
            null
        }
    }

    private fun deleteTempPhotoFile() {
        tempPhotoFile?.let {
            try {
                if (it.exists()) it.delete()
            } catch (_: Exception) { }
            tempPhotoFile = null
        }
    }

    private fun resetForm() {
        rgPermitType.clearCheck()
        etStartDate.text.clear()
        etEndDate.text.clear()
        startDateMillis = 0L
        etReason.text.clear()
        tilStartDate.error = null
        tilEndDate.error = null
        tilReason.error = null
        selectedPhotoUri = null
        layoutUploadPlaceholder.visibility = View.VISIBLE
        layoutPhotoPreview.visibility = View.GONE
        tvPhotoLabel.text = "BUKTI FOTO (OPSIONAL)"
        tvPhotoLabel.setTextColor(getColor(R.color.slate_500))
    }

    private fun loadRiwayatIzin() {
        progressRiwayat.visibility = View.VISIBLE
        rvRiwayatIzin.visibility = View.GONE
        tvEmptyRiwayat.visibility = View.GONE

        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getPengajuanIzinList(token)
            }
            progressRiwayat.visibility = View.GONE

            result.onSuccess { response ->
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                val data = response.data
                if (data.isEmpty()) {
                    tvEmptyRiwayat.visibility = View.VISIBLE
                    rvRiwayatIzin.visibility = View.GONE
                } else {
                    tvEmptyRiwayat.visibility = View.GONE
                    rvRiwayatIzin.visibility = View.VISIBLE
                    riwayatAdapter.submitList(data)
                }
            }
            result.onFailure {
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                tvEmptyRiwayat.visibility = View.VISIBLE
                tvEmptyRiwayat.text = getString(R.string.gagal_memuat_riwayat)
                rvRiwayatIzin.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteTempPhotoFile()
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
}