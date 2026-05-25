package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.adapter.JurusanGroupAdapter
import com.xirpl2.SASMobile.adapter.JurusanGroup
import com.xirpl2.SASMobile.model.KelasManagementItem
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.model.StaffInfo
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KelolaKelasActivity : BaseAdminActivity() {

    private val repository = BerandaRepository()
    private lateinit var kelasAdapter: JurusanGroupAdapter
    
    private lateinit var etSearch: EditText
    private lateinit var spinnerJurusan: AutoCompleteTextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var recyclerKelas: RecyclerView
    private lateinit var iconNotification: ImageView

    private var allKelasList = mutableListOf<KelasManagementItem>()
    private var staffList = listOf<StaffInfo>()
    private var majorsList = mutableListOf("Semua Jurusan")
    
    private var selectedJurusan = "Semua Jurusan"
    private var searchQuery = ""
    private var searchJob: Job? = null

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.KELOLA_KELAS

    override fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_kelas)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupListeners()

        loadData()
        loadStaffList()
    }

    private fun initializeViews() {
        etSearch = findViewById(R.id.etSearch)
        spinnerJurusan = findViewById(R.id.spinnerJurusan)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        emptyStateContainer = findViewById(R.id.emptyState)
        recyclerKelas = findViewById(R.id.recyclerKelas)
        iconNotification = findViewById(R.id.iconNotification)
    }

    private fun setupRecyclerView() {
        kelasAdapter = JurusanGroupAdapter(
            onUbahWaliClick = { kelas -> showUbahWaliDialog(kelas) },
            onExpandClick = { kelas, callback -> loadStudentsInClass(kelas.id_kelas, callback) },
            onStudentDetailClick = { siswa ->
                if (siswa.id_siswa == 0) {
                    Toast.makeText(this, "Membuka form tambah siswa...", Toast.LENGTH_SHORT).show()
                } else {
                    showStudentDetail(siswa)
                }
            }
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

        spinnerJurusan.setOnItemClickListener { parent, _, position, _ ->
            selectedJurusan = parent.getItemAtPosition(position).toString()
            applyFilters()
        }

        iconNotification.setOnClickListener {
            Toast.makeText(this, "Belum ada notifikasi baru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerKelas.visibility = View.GONE
        emptyStateContainer.visibility = View.GONE

        lifecycleScope.launch {
            repository.getAdminManagementKelas(token).fold(
                onSuccess = { list ->
                    allKelasList.clear()
                    allKelasList.addAll(list)
                    
                    val majors = list.mapNotNull { it.jurusan }.distinct().sorted()
                    setupJurusanDropdown(majors)

                    applyFilters()

                    progressLoading.visibility = View.GONE
                },
                onFailure = { error ->
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@KelolaKelasActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupJurusanDropdown(majors: List<String>) {
        majorsList.clear()
        majorsList.add("Semua Jurusan")
        majorsList.addAll(majors)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, majorsList)
        spinnerJurusan.setAdapter(adapter)
        spinnerJurusan.setText(majorsList[0], false)
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
        val result = allKelasList.filter { kelas ->
            val matchSearch = kelas.label.contains(searchQuery, ignoreCase = true) ||
                             (kelas.wali_kelas?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchJurusan = selectedJurusan == "Semua Jurusan" || kelas.jurusan == selectedJurusan
            matchSearch && matchJurusan
        }

        val groups = result
            .groupBy { it.jurusan }
            .toSortedMap()
            .map { (jurusan, kelasList) ->
                val logoRes = when (jurusan.uppercase()) {
                    "RPL" -> R.drawable.logo_rpl
                    "TKJ" -> R.drawable.logo_tkj
                    "DKV" -> R.drawable.logo_dkv
                    "TEI" -> R.drawable.logo_tei
                    "BC" -> R.drawable.logo_bc
                    "TMT" -> R.drawable.logo_mt
                    "TAV" -> R.drawable.logo_tav
                    else -> R.drawable.ic_class
                }
                JurusanGroup(
                    jurusan = jurusan,
                    logoRes = logoRes,
                    kelas = kelasList.sortedWith(compareBy { it.label })
                )
            }

        kelasAdapter.updateGroups(groups)

        emptyStateContainer.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        recyclerKelas.visibility = if (groups.isNotEmpty()) View.VISIBLE else View.GONE
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
