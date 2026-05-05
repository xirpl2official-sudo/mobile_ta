package com.xirpl2.SASMobile

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class PengajuanIzinActivity : AppCompatActivity() {

    
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


    private val calendar = Calendar.getInstance()

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
    }

    private fun setupPermitTypes() {
        rgPermitType.setOnCheckedChangeListener { _, _ ->
            
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

        
        lifecycleScope.launch {
            delay(1500) 
            
            setLoading(false)
            
            Toast.makeText(
                this@PengajuanIzinActivity,
                "Pengajuan izin berhasil dikirim",
                Toast.LENGTH_LONG
            ).show()
            
            finish()
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
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, MasukActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}