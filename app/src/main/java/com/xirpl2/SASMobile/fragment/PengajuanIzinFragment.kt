package com.xirpl2.SASMobile.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.StudentMainActivity
import com.xirpl2.SASMobile.adapter.RiwayatIzinAdapter
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.repository.PengajuanIzinRepository
import com.xirpl2.SASMobile.utils.LogoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PengajuanIzinFragment : Fragment(R.layout.fragment_pengajuan_izin) {

    private lateinit var rgPermitType: RadioGroup
    private lateinit var rbSakit: RadioButton
    private lateinit var rbIzin: RadioButton
    private lateinit var tilStartDate: com.google.android.material.textfield.TextInputLayout
    private lateinit var etStartDate: EditText
    private lateinit var tilEndDate: com.google.android.material.textfield.TextInputLayout
    private lateinit var etEndDate: EditText
    private lateinit var tilReason: com.google.android.material.textfield.TextInputLayout
    private lateinit var etReason: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var layoutUploadPhoto: FrameLayout
    private lateinit var layoutUploadPlaceholder: LinearLayout
    private lateinit var layoutPhotoPreview: LinearLayout
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnRemovePhoto: com.google.android.material.button.MaterialButton
    private lateinit var tvPhotoLabel: TextView
    private var selectedPhotoUri: Uri? = null
    private var tempPhotoFile: File? = null
    private lateinit var rvRiwayatIzin: RecyclerView
    private lateinit var progressRiwayat: ProgressBar
    private lateinit var tvEmptyRiwayat: TextView
    private lateinit var riwayatAdapter: RiwayatIzinAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val repository = PengajuanIzinRepository()
    private val calendar = Calendar.getInstance()
    private var startDateMillis: Long = 0L

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedPhotoUri = it; showPhotoPreview(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyEdgeToEdge(view)
        initializeViews(view)
        view.findViewById<ImageView>(R.id.logoSekolah)?.let { LogoHelper.loadLogo(it) }
        setupPermitTypes()
        setupDatePickers()
        setupSubmitButton()
        setupCancelButton()
        setupPhotoUpload()
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadRiwayatIzin() }
        loadRiwayatIzin()
    }

    private fun initializeViews(view: View) {
        rgPermitType = view.findViewById(R.id.rgPermitType)
        rbSakit = view.findViewById(R.id.rbSakit)
        rbIzin = view.findViewById(R.id.rbIzin)
        tilStartDate = view.findViewById(R.id.tilStartDate)
        etStartDate = view.findViewById(R.id.etStartDate)
        tilEndDate = view.findViewById(R.id.tilEndDate)
        etEndDate = view.findViewById(R.id.etEndDate)
        tilReason = view.findViewById(R.id.tilReason)
        etReason = view.findViewById(R.id.etReason)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnCancel = view.findViewById(R.id.btnCancel)
        layoutUploadPhoto = view.findViewById(R.id.layoutUploadPhoto)
        layoutUploadPlaceholder = view.findViewById(R.id.layoutUploadPlaceholder)
        layoutPhotoPreview = view.findViewById(R.id.layoutPhotoPreview)
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview)
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto)
        tvPhotoLabel = view.findViewById(R.id.tvPhotoLabel)
        rvRiwayatIzin = view.findViewById(R.id.rvRiwayatIzin)
        progressRiwayat = view.findViewById(R.id.progressRiwayat)
        tvEmptyRiwayat = view.findViewById(R.id.tvEmptyRiwayat)
        riwayatAdapter = RiwayatIzinAdapter()
        rvRiwayatIzin.layoutManager = LinearLayoutManager(requireContext())
        rvRiwayatIzin.adapter = riwayatAdapter
    }

    private fun setupPhotoUpload() {
        layoutUploadPhoto.setOnClickListener {
            if (selectedPhotoUri == null) photoPickerLauncher.launch("image/*")
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
                tvPhotoLabel.setTextColor(requireContext().getColor(R.color.status_error))
            } else {
                tvPhotoLabel.text = "BUKTI FOTO (OPSIONAL)"
                tvPhotoLabel.setTextColor(requireContext().getColor(R.color.slate_500))
            }
        }
    }

    private fun setupDatePickers() {
        val startDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            etStartDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            tilStartDate.error = null
            val cal = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
            startDateMillis = cal.timeInMillis
        }
        val endDateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            etEndDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            tilEndDate.error = null
        }
        etStartDate.setOnClickListener {
            DatePickerDialog(requireContext(), startDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }.show()
        }
        etEndDate.setOnClickListener {
            DatePickerDialog(requireContext(), endDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = if (startDateMillis > 0L) startDateMillis else Calendar.getInstance().timeInMillis }.show()
        }
    }

    private fun setupCancelButton() {
        btnCancel.setOnClickListener { resetForm() }
    }

    private fun setupSubmitButton() {
        btnSubmit.setOnClickListener { if (validateForm()) submitPermitRequest() }
    }

    private fun validateForm(): Boolean {
        var isValid = true
        if (rgPermitType.checkedRadioButtonId == -1) { Toast.makeText(requireContext(), "Pilih jenis perizinan", Toast.LENGTH_SHORT).show(); isValid = false }
        val startDate = etStartDate.text.toString().trim()
        if (startDate.isEmpty()) { tilStartDate.error = "Pilih tanggal mulai"; isValid = false } else tilStartDate.error = null
        val endDate = etEndDate.text.toString().trim()
        if (endDate.isEmpty()) { tilEndDate.error = "Pilih tanggal berakhir"; isValid = false } else tilEndDate.error = null
        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            try {
                val start = parseDate(startDate)
                val end = parseDate(endDate)
                if (end < start) {
                    tilEndDate.error = "Tanggal akhir harus setelah atau sama dengan tanggal mulai"
                    isValid = false
                }
            } catch (_: Exception) {
                tilEndDate.error = "Format tanggal tidak valid"
                isValid = false
            }
        }
        val reason = etReason.text.toString().trim()
        if (reason.isEmpty()) { tilReason.error = "Isi alasan perizinan"; isValid = false } else if (reason.length < 10) { tilReason.error = "Alasan minimal 10 karakter"; isValid = false } else tilReason.error = null
        if (rbSakit.isChecked && selectedPhotoUri == null) { Toast.makeText(requireContext(), "Lampirkan bukti foto untuk izin sakit", Toast.LENGTH_SHORT).show(); isValid = false }
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
                val token = getToken()
                val response = RetrofitClient.apiService.createPengajuanIzin(
                    token = "Bearer $token",
                    jenisIzin = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, jenisIzin),
                    tanggalAwal = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, startDate),
                    tanggalAkhir = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, endDate),
                    keterangan = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, reason),
                    buktiFoto = preparePhotoPart()
                )
                deleteTempPhotoFile()
                    withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(requireContext(), response.body()?.message ?: "Pengajuan izin berhasil dikirim", Toast.LENGTH_LONG).show()
                        resetForm(); loadRiwayatIzin()
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string()?.let { org.json.JSONObject(it).optString("error", response.message()) } ?: response.message()
                        } catch (_: Exception) { response.message() }
                        Toast.makeText(requireContext(), "Gagal mengirim: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                deleteTempPhotoFile()
                withContext(Dispatchers.Main) { setLoading(false); Toast.makeText(requireContext(), "Kesalahan: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun preparePhotoPart(): MultipartBody.Part? {
        val uri = selectedPhotoUri ?: return null
        return try {
            deleteTempPhotoFile()
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("bukti_foto_", ".jpg", requireContext().cacheDir)
            FileOutputStream(tempFile).use { inputStream.copyTo(it) }; inputStream.close()
            tempPhotoFile = tempFile
            MultipartBody.Part.createFormData("bukti_foto", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull()!!))
        } catch (e: Exception) { null }
    }

    private fun deleteTempPhotoFile() { tempPhotoFile?.let { try { if (it.exists()) it.delete() } catch (_: Exception) {} }; tempPhotoFile = null }

    private fun resetForm() {
        rgPermitType.clearCheck(); etStartDate.text.clear(); etEndDate.text.clear(); startDateMillis = 0L
        etReason.text.clear(); tilStartDate.error = null; tilEndDate.error = null; tilReason.error = null
        selectedPhotoUri = null; layoutUploadPlaceholder.visibility = View.VISIBLE; layoutPhotoPreview.visibility = View.GONE
        tvPhotoLabel.text = "BUKTI FOTO (OPSIONAL)"; tvPhotoLabel.setTextColor(requireContext().getColor(R.color.slate_500))
    }

    private fun loadRiwayatIzin() {
        progressRiwayat.visibility = View.VISIBLE; rvRiwayatIzin.visibility = View.GONE; tvEmptyRiwayat.visibility = View.GONE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { repository.getPengajuanIzinList(getToken()) }
            progressRiwayat.visibility = View.GONE
            result.onSuccess { response ->
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                if (response.data.isEmpty()) { tvEmptyRiwayat.visibility = View.VISIBLE; rvRiwayatIzin.visibility = View.GONE }
                else { tvEmptyRiwayat.visibility = View.GONE; rvRiwayatIzin.visibility = View.VISIBLE; riwayatAdapter.submitList(response.data) }
            }
            result.onFailure {
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                tvEmptyRiwayat.visibility = View.VISIBLE; rvRiwayatIzin.visibility = View.GONE
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnSubmit.isEnabled = !isLoading; btnSubmit.text = if (isLoading) "Mengirim..." else "Kirim Pengajuan"
        rbSakit.isEnabled = !isLoading; rbIzin.isEnabled = !isLoading
        etStartDate.isEnabled = !isLoading; etEndDate.isEnabled = !isLoading; etReason.isEnabled = !isLoading
        btnCancel.isEnabled = !isLoading; layoutUploadPhoto.isClickable = !isLoading
    }

    private fun getToken(): String {
        val act = requireActivity()
        return if (act is StudentMainActivity) act.getAuthTokenSiswa() else ""
    }

    private fun applyEdgeToEdge(view: View) {
        val topBar = view.findViewById<View>(R.id.topBarContent) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deleteTempPhotoFile()
    }
}
