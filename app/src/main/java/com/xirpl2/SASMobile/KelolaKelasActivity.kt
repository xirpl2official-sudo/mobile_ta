package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.adapter.KelasManageAdapter
import com.xirpl2.SASMobile.model.KelasManagementItem
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.model.StaffInfo
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KelolaKelasActivity : BaseAdminActivity() {

    private val repository = BerandaRepository()
    private lateinit var kelasAdapter: KelasManageAdapter
    
    private lateinit var etSearch: EditText
    private lateinit var btnFilterJurusan: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerKelas: RecyclerView

    private var allKelasList = mutableListOf<KelasManagementItem>()
    private var filteredKelasList = mutableListOf<KelasManagementItem>()
    private var staffList = listOf<StaffInfo>()
    
    private var selectedJurusan = "Semua Jurusan"
    private var searchQuery = ""
    private var searchJob: Job? = null

    private val fixedJurusanList = listOf("Semua Jurusan", "RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.KELOLA_KELAS

    override fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_kelas)

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupListeners()
        
        loadData()
        loadStaffList()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnFilterJurusan = findViewById(R.id.btnFilterJurusan)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        recyclerKelas = findViewById(R.id.recyclerKelas)
    }

    private fun setupRecyclerView() {
        kelasAdapter = KelasManageAdapter(
            kelasList = filteredKelasList,
            onUbahWaliClick = { kelas -> showUbahWaliDialog(kelas) },
            onExpandClick = { kelas, callback -> loadStudentsInClass(kelas.id_kelas, callback) },
            onStudentDetailClick = { siswa -> showStudentDetail(siswa) }
        )
        recyclerKelas.apply {
            layoutManager = LinearLayoutManager(this@KelolaKelasActivity)
            adapter = kelasAdapter
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    searchQuery = s?.toString() ?: ""
                    applyFilters()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnFilterJurusan.setOnClickListener {
            showJurusanPicker()
        }
    }

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerKelas.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            repository.getAdminManagementKelas(token).fold(
                onSuccess = { list ->
                    allKelasList.clear()
                    allKelasList.addAll(list)
                    applyFilters()
                    
                    progressLoading.visibility = View.GONE
                    recyclerKelas.visibility = if (filteredKelasList.isNotEmpty()) View.VISIBLE else View.GONE
                    tvEmptyState.visibility = if (filteredKelasList.isEmpty()) View.VISIBLE else View.GONE
                },
                onFailure = { error ->
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@KelolaKelasActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadStaffList() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getStaffGuruLookup(token).onSuccess { list ->
                staffList = list
            }
        }
    }

    private fun applyFilters() {
        filteredKelasList.clear()
        val result = allKelasList.filter { kelas ->
            val matchSearch = kelas.label.contains(searchQuery, ignoreCase = true) || 
                             (kelas.wali_kelas?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchJurusan = selectedJurusan == "Semua Jurusan" || kelas.jurusan == selectedJurusan
            matchSearch && matchJurusan
        }
        filteredKelasList.addAll(result)
        kelasAdapter.updateData(filteredKelasList)
        
        tvEmptyState.visibility = if (filteredKelasList.isEmpty()) View.VISIBLE else View.GONE
        recyclerKelas.visibility = if (filteredKelasList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showJurusanPicker() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Jurusan")
            .setItems(fixedJurusanList.toTypedArray()) { _, which ->
                selectedJurusan = fixedJurusanList[which]
                btnFilterJurusan.text = selectedJurusan
                applyFilters()
            }
            .show()
    }

    private fun loadStudentsInClass(idKelas: Int, callback: (List<SiswaItem>) -> Unit) {
        val token = getAuthToken()
        lifecycleScope.launch {
            repository.getAdminManagementKelasDetail(token, idKelas).fold(
                onSuccess = { detail -> callback(detail.students) },
                onFailure = { error -> 
                    Toast.makeText(this@KelolaKelasActivity, "Gagal memuat siswa: ${error.message}", Toast.LENGTH_SHORT).show()
                    callback(emptyList())
                }
            )
        }
    }

    private fun showUbahWaliDialog(kelas: KelasManagementItem) {
        if (staffList.isEmpty()) {
            Toast.makeText(this, "Data guru belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val staffNames = staffList.map { it.nama }.toTypedArray()
        var selectedStaffIndex = staffList.indexOfFirst { it.id_staff == kelas.id_staff_wali }

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Wali Kelas - ${kelas.label}")
            .setSingleChoiceItems(staffNames, selectedStaffIndex) { _, which ->
                selectedStaffIndex = which
            }
            .setPositiveButton("Simpan") { dialog, _ ->
                if (selectedStaffIndex != -1) {
                    val selectedStaff = staffList[selectedStaffIndex]
                    updateWaliKelas(kelas.id_kelas, selectedStaff.id_staff)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateWaliKelas(idKelas: Int, idStaff: Int) {
        val token = getAuthToken()
        lifecycleScope.launch {
            repository.updateWaliKelas(token, idKelas, idStaff).fold(
                onSuccess = { message ->
                    Toast.makeText(this@KelolaKelasActivity, message, Toast.LENGTH_SHORT).show()
                    loadData() // Refresh
                },
                onFailure = { error ->
                    Toast.makeText(this@KelolaKelasActivity, "Gagal update: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showStudentDetail(siswa: SiswaItem) {
        val dialog = SiswaDetailDialogFragment.newInstance(siswa)
        dialog.show(supportFragmentManager, "SiswaDetailDialog")
    }
}
