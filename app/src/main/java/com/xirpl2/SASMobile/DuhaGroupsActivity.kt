package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.xirpl2.SASMobile.model.DuhaGroup
import com.xirpl2.SASMobile.model.DuhaGroupRequest
import com.xirpl2.SASMobile.model.WeeklyDuhaGroupRequest
import com.xirpl2.SASMobile.repository.BerandaRepository
import kotlinx.coroutines.launch

class DuhaGroupsActivity : BaseAdminActivity() {

    private val repository = BerandaRepository()
    private lateinit var adapter: DuhaGroupAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val allGroups = mutableListOf<DuhaGroup>()

    private val daysList = listOf("Senin", "Selasa", "Rabu", "Kamis")
    private val fixedJurusanList = listOf("RPL", "TKJ", "TEI", "TAV", "BC", "TMT", "DKV", "ANM")

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.KELOLA_KELAS

    override fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duha_groups)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupFab()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadData() }

        loadData()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerDuhaGroups)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        emptyStateContainer = findViewById(R.id.emptyState)
    }

    private fun setupRecyclerView() {
        adapter = DuhaGroupAdapter(
            onEditClick = { group -> showDuhaGroupDialog(group) },
            onDeleteClick = { group -> confirmDelete(group) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DuhaGroupsActivity)
            adapter = this@DuhaGroupsActivity.adapter
        }
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.btnTambahDuhaGroup).setOnClickListener {
            showDuhaGroupDialog(null)
        }

        findViewById<MaterialButton>(R.id.btnBuatMingguan).setOnClickListener {
            showBuatMingguanDialog()
        }
    }

    private fun loadData() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateContainer.visibility = View.GONE

        lifecycleScope.launch {
            repository.getDuhaGroups(token).fold(
                onSuccess = { list ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    allGroups.clear()
                    allGroups.addAll(list)
                    adapter.submitList(allGroups.toList())

                    progressLoading.visibility = View.GONE
                    if (allGroups.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyStateContainer.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                },
                onFailure = { error ->
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    emptyStateContainer.visibility = View.VISIBLE
                    tvEmptyState.text = "Gagal memuat data"
                    recyclerView.visibility = View.GONE

                    Snackbar.make(findViewById(R.id.main), "Gagal: ${error.message}", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Coba Lagi") { loadData() }
                        .setActionTextColor(getColor(R.color.blue_theme))
                        .show()
                }
            )
        }
    }

    private fun showDuhaGroupDialog(existing: DuhaGroup?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_duha_group, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val actvHari = dialogView.findViewById<AutoCompleteTextView>(R.id.actvHari)
        val actvJurusan = dialogView.findViewById<AutoCompleteTextView>(R.id.actvJurusan)
        val etStudents = dialogView.findViewById<EditText>(R.id.etStudents)
        val btnSimpan = dialogView.findViewById<MaterialButton>(R.id.btnSimpan)
        val btnBatal = dialogView.findViewById<MaterialButton>(R.id.btnBatal)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        actvHari.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, daysList))
        actvJurusan.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fixedJurusanList))

        if (existing != null) {
            tvTitle.text = "Edit Grup Duha"
            btnSimpan.text = "PERBARUI"
            actvHari.setText(existing.hari, false)
            actvJurusan.setText(existing.jurusan, false)
            etStudents.setText(existing.students.joinToString("\n"))
        } else {
            tvTitle.text = "Tambah Grup Duha"
            btnSimpan.text = "SIMPAN"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val hari = actvHari.text.toString().trim()
            val jurusan = actvJurusan.text.toString().trim()
            val studentsText = etStudents.text.toString().trim()
            val students = if (studentsText.isEmpty()) emptyList() else studentsText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

            if (hari.isEmpty() || jurusan.isEmpty()) {
                Toast.makeText(this, "Harap pilih hari dan jurusan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            btnSimpan.text = "MENYIMPAN..."

            val jurusanIndex = fixedJurusanList.indexOf(jurusan)
            val request = DuhaGroupRequest(
                hari = hari,
                jurusan = jurusan,
                idJurusan = jurusanIndex + 1,
                students = students
            )

            val token = getAuthToken()

            lifecycleScope.launch {
                val result = if (existing != null) {
                    repository.updateDuhaGroup(token, existing.id, request)
                } else {
                    repository.createDuhaGroup(token, request)
                }

                result.fold(
                    onSuccess = {
                        Toast.makeText(this@DuhaGroupsActivity, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadData()
                    },
                    onFailure = { error ->
                        btnSimpan.isEnabled = true
                        btnSimpan.text = if (existing != null) "PERBARUI" else "SIMPAN"
                        Toast.makeText(this@DuhaGroupsActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun confirmDelete(group: DuhaGroup) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Grup Duha")
            .setMessage("Apakah Anda yakin ingin menghapus grup ${group.jurusan} (${group.hari})?")
            .setPositiveButton("Hapus") { _, _ -> deleteGroup(group) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteGroup(group: DuhaGroup) {
        val token = getAuthToken()
        lifecycleScope.launch {
            repository.deleteDuhaGroup(token, group.id).fold(
                onSuccess = {
                    Toast.makeText(this@DuhaGroupsActivity, "Grup berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = { error ->
                    Toast.makeText(this@DuhaGroupsActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showBuatMingguanDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Buat Jadwal Mingguan")
            .setMessage("Akan membuat jadwal Duha untuk semua grup yang ada selama satu minggu penuh.\nLanjutkan?")
            .setPositiveButton("Buat") { _, _ -> buatMingguan() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun buatMingguan() {
        val token = getAuthToken()
        progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val groups = allGroups.map { group ->
                val jurusanIndex = fixedJurusanList.indexOf(group.jurusan)
                DuhaGroupRequest(
                    hari = group.hari,
                    jurusan = group.jurusan,
                    idJurusan = if (jurusanIndex >= 0) jurusanIndex + 1 else 1,
                    students = group.students
                )
            }
            val request = WeeklyDuhaGroupRequest(groups = groups)

            repository.createWeeklyDuhaGroups(token, request).fold(
                onSuccess = {
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@DuhaGroupsActivity, "Jadwal mingguan berhasil dibuat", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = { error ->
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@DuhaGroupsActivity, "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

class DuhaGroupAdapter(
    private val onEditClick: (DuhaGroup) -> Unit,
    private val onDeleteClick: (DuhaGroup) -> Unit
) : ListAdapter<DuhaGroup, DuhaGroupAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_duha_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.cardDuhaGroup)
        private val tvHari: TextView = view.findViewById(R.id.tvHari)
        private val tvJurusan: TextView = view.findViewById(R.id.tvJurusan)
        private val tvStudents: TextView = view.findViewById(R.id.tvStudents)
        private val btnEdit: ImageView = view.findViewById(R.id.btnEditDuhaGroup)
        private val btnDelete: ImageView = view.findViewById(R.id.btnDeleteDuhaGroup)

        fun bind(group: DuhaGroup) {
            tvHari.text = group.hari
            tvJurusan.text = group.jurusan
            tvStudents.text = if (group.students.isEmpty()) "Tidak ada siswa" else group.students.joinToString(", ")
            btnEdit.setOnClickListener { onEditClick(group) }
            btnDelete.setOnClickListener { onDeleteClick(group) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DuhaGroup>() {
        override fun areItemsTheSame(oldItem: DuhaGroup, newItem: DuhaGroup) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DuhaGroup, newItem: DuhaGroup) = oldItem == newItem
    }
}
