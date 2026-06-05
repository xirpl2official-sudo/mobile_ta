package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xirpl2.SASMobile.adapter.JurusanGroupAdapter
import com.xirpl2.SASMobile.adapter.JurusanGroup
import com.xirpl2.SASMobile.model.KelasManagementItem
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.model.StaffInfo
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class KelolaKelasActivity : BaseAdminActivity() {

    private val repository = BerandaRepository()
    private lateinit var kelasAdapter: JurusanGroupAdapter

    private lateinit var etSearch: EditText
    private lateinit var actvJurusan: AutoCompleteTextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var recyclerKelas: RecyclerView

    private var allKelasList = mutableListOf<KelasManagementItem>()
    private var staffList = listOf<StaffInfo>()
    private var majorsList = mutableListOf("Semua Jurusan")

    private var selectedJurusan = "Semua Jurusan"
    private var searchQuery = ""
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var searchJob: Job? = null
    private val isWaliUpdating = AtomicBoolean(false)

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

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadData()
            loadStaffList()
        }

        loadData()
        loadStaffList()
    }

    private fun initializeViews() {
        etSearch = findViewById(R.id.etSearch)
        actvJurusan = findViewById(R.id.actvJurusan)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        emptyStateContainer = findViewById(R.id.emptyState)
        recyclerKelas = findViewById(R.id.recyclerKelas)
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

        actvJurusan.setOnItemClickListener { parent, _, position, _ ->
            selectedJurusan = parent.getItemAtPosition(position).toString()
            applyFilters()
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
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    allKelasList.clear()
                    allKelasList.addAll(list)

                    val majors = list.mapNotNull { it.jurusan }.distinct().sorted()
                    setupJurusanDropdown(majors)

                    applyFilters()
                    progressLoading.visibility = View.GONE
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                    tvEmptyState.text = "Gagal memuat data"
                    recyclerKelas.visibility = View.GONE

                    Snackbar.make(findViewById(R.id.main), "Gagal: ${error.message}", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Coba Lagi") { loadData() }
                        .setActionTextColor(getColor(R.color.blue_theme))
                        .show()
                }
            )
        }
    }

    private fun setupJurusanDropdown(majors: List<String>) {
        majorsList.clear()
        majorsList.add("Semua Jurusan")
        majorsList.addAll(majors)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, majorsList)
        actvJurusan.setAdapter(adapter)
        actvJurusan.setText(majorsList[0], false)
    }

    private fun loadStaffList() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        lifecycleScope.launch {
            repository.getStaffGuruLookup(token).fold(
                onSuccess = { list -> staffList = list },
                onFailure = { error ->
                    Toast.makeText(this@KelolaKelasActivity, "Gagal memuat data guru: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
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
                JurusanGroup(
                    jurusan = jurusan,
                    logoRes = getJurusanLogo(jurusan),
                    kelas = kelasList.sortedWith(compareBy { it.label })
                )
            }

        kelasAdapter.updateGroups(groups)

        emptyStateContainer.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        recyclerKelas.visibility = if (groups.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun getJurusanLogo(jurusan: String?): Int {
        if (jurusan == null) return R.drawable.ic_class
        return when (jurusan.uppercase()) {
            "RPL" -> R.drawable.logo_rpl
            "TKJ" -> R.drawable.logo_tkj
            "DKV" -> R.drawable.logo_dkv
            "TEI" -> R.drawable.logo_tei
            "BC", "BROADCASTING" -> R.drawable.logo_bc
            "TMT", "MEKATRONIKA" -> R.drawable.logo_mt
            "TAV" -> R.drawable.logo_tav
            "ANM", "ANIMASI" -> R.drawable.logo_animasi
            else -> R.drawable.ic_class
        }
    }

    private fun loadStudentsInClass(idKelas: Int, callback: (List<SiswaItem>) -> Unit) {
        val token = getAuthToken()
        if (token.isEmpty()) {
            callback(emptyList())
            return
        }
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
            Toast.makeText(this, "Data guru belum tersedia. Tarik ke bawah untuk memuat ulang.", Toast.LENGTH_SHORT).show()
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
                    updateWaliKelas(kelas, selectedStaff.id_staff)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateWaliKelas(kelas: KelasManagementItem, idStaff: Int) {
        if (!isWaliUpdating.compareAndSet(false, true)) {
            Toast.makeText(this, "Sedang memperbarui...", Toast.LENGTH_SHORT).show()
            return
        }
        val token = getAuthToken()
        if (token.isEmpty()) { isWaliUpdating.set(false); return }
        lifecycleScope.launch {
            val staffName = staffList.find { it.id_staff == idStaff }?.nama ?: ""
            repository.updateWaliKelas(token, kelas.id_kelas, idStaff).fold(
                onSuccess = { message ->
                    val index = allKelasList.indexOfFirst { it.id_kelas == kelas.id_kelas }
                    if (index >= 0) {
                        allKelasList[index] = allKelasList[index].copy(wali_kelas = staffName, id_staff_wali = idStaff)
                        applyFilters()
                    }
                    Toast.makeText(this@KelolaKelasActivity, message, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(this@KelolaKelasActivity, "Gagal update: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            isWaliUpdating.set(false)
        }
    }

    private fun showStudentDetail(siswa: SiswaItem) {
        val dialog = SiswaDetailDialogFragment.newInstance(siswa)
        dialog.show(supportFragmentManager, "SiswaDetailDialog")
    }
}
