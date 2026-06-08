package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.model.VerifyCodeRequest
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch

class ManualPresensiSiswaDialogFragment : DialogFragment() {

    private lateinit var tvNis: TextView
    private lateinit var tvNama: TextView
    private lateinit var etCode: TextInputEditText
    private lateinit var tvStatus: TextView
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnBatal: MaterialButton
    private lateinit var btnClose: ImageView

    private var studentNis: String = ""
    private var studentNama: String = ""

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_manual_presensi_siswa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadStudentData()
        setupListeners()
    }

    private fun initViews(view: View) {
        tvNis = view.findViewById(R.id.tvNis)
        tvNama = view.findViewById(R.id.tvNama)
        etCode = view.findViewById(R.id.etCode)
        tvStatus = view.findViewById(R.id.tvStatus)
        btnSimpan = view.findViewById(R.id.btnSimpan)
        btnBatal = view.findViewById(R.id.btnBatal)
        btnClose = view.findViewById(R.id.btnClose)
    }

    private fun loadStudentData() {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(requireContext())
        studentNis = session.getString("user_nis", "") ?: ""
        studentNama = session.getString("user_name", "") ?: ""

        tvNis.text = studentNis.ifEmpty { "-" }
        tvNama.text = studentNama.ifEmpty { "-" }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnBatal.setOnClickListener { dismiss() }
        btnSimpan.setOnClickListener { submitCode() }

        etCode.addTextChangedListener { text ->
            if (text?.length == 6) {
                tvStatus.visibility = View.GONE
            }
        }
    }

    private fun submitCode() {
        val code = etCode.text.toString().trim()

        if (studentNis.isEmpty()) {
            Toast.makeText(context, "Data siswa tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        if (code.length != 6) {
            tvStatus.visibility = View.VISIBLE
            tvStatus.text = "Kode harus 6 digit"
            tvStatus.setTextColor(requireContext().getColor(R.color.status_error))
            return
        }

        btnSimpan.isEnabled = false
        btnSimpan.text = "MENGIRIM..."
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "Memverifikasi kode..."
        tvStatus.setTextColor(requireContext().getColor(R.color.text_hint))

        val token = getToken()
        if (token.isEmpty()) {
            Toast.makeText(context, "Sesi berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        lifecycleScope.launch {
            try {
                val request = VerifyCodeRequest(code = code)
                val response = RetrofitClient.apiService.verifyAttendanceCode(
                    token = "Bearer $token",
                    request = request
                )

                if (isAdded.not()) return@launch

                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val data = body.data
                        val msg = buildString {
                            append(body.message)
                            if (data != null) {
                                append("\n${data.jenis_sholat} - ${data.tanggal}")
                                append("\nStatus: ${data.status}")
                            }
                        }
                        tvStatus.text = msg
                        tvStatus.setTextColor(requireContext().getColor(R.color.status_success))
                        Toast.makeText(context, "Absensi berhasil!", Toast.LENGTH_LONG).show()
                        onDismissCallback?.invoke()
                        dismiss()
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string()?.let { errBody ->
                                org.json.JSONObject(errBody).optString("message", response.message())
                            } ?: response.message()
                        } catch (_: Exception) {
                            response.message()
                        }

                        tvStatus.text = "Gagal: $errorMsg"
                        tvStatus.setTextColor(requireContext().getColor(R.color.status_error))
                        btnSimpan.isEnabled = true
                        btnSimpan.text = "KIRIM ABSENSI"
                    }
                }
            } catch (e: Exception) {
                if (isAdded.not()) return@launch
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    tvStatus.text = "Kesalahan: ${e.message}"
                    tvStatus.setTextColor(requireContext().getColor(R.color.status_error))
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "KIRIM ABSENSI"
                }
            }
        }
    }

    private fun getToken(): String {
        val session = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(requireContext())
        return session.getString("auth_token", "") ?: ""
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(onDismiss: (() -> Unit)? = null): ManualPresensiSiswaDialogFragment {
            return ManualPresensiSiswaDialogFragment().apply {
                onDismissCallback = onDismiss
            }
        }
    }
}
