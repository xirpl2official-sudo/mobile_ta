package com.xirpl2.SASMobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.adapter.KelolaGuruAdminAdapter
import com.xirpl2.SASMobile.model.GuruItem
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KelolaGuruAdminActivity : BaseAdminActivity() {

    private lateinit var recyclerGuru: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmptyState: View
    private lateinit var etSearch: EditText
    private lateinit var tvCountInfo: TextView

    private lateinit var adapter: KelolaGuruAdminAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var guruList = mutableListOf<GuruItem>()
    private var searchJob: Job? = null

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.KELOLA_GURU

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelola_guru_admin)

        setupStatusBar()
        findViewById<View>(R.id.topBarContent)?.let { applyEdgeToEdge(it) }

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupSearch()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadDataGuru(etSearch.text.toString()) }

        loadDataGuru()
    }

    override fun onResume() {
        super.onResume()
        // Reload data whenever this activity resumes (e.g. after adding or editing a guru)
        loadDataGuru(etSearch.text.toString())
    }

    private fun initViews() {
        recyclerGuru = findViewById(R.id.recyclerGuru)
        progressLoading = findViewById(R.id.progressLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        etSearch = findViewById(R.id.etSearch)
        tvCountInfo = findViewById(R.id.tvCountInfo)

        val fabAddGuru = findViewById<View>(R.id.fabAddGuru)
        fabAddGuru?.setOnClickListener {
            safeNavigateTo(TambahGuruActivity::class.java)
        }
    }

    private fun setupRecyclerView() {
        adapter = KelolaGuruAdminAdapter(
            onEditClick = { guru ->
                val intent = Intent(this, EditGuruActivity::class.java).apply {
                    putExtra("guru_id", guru.id_staff)
                    putExtra("guru_nama", guru.nama)
                    putExtra("guru_nip", guru.nip)
                    putExtra("guru_email", guru.email)
                }
                startActivity(intent)
            },
            onLepasWaliKelasClick = { guru ->
                showConfirmLepasWaliKelas(guru)
            },
            onDeleteClick = { guru ->
                showConfirmDelete(guru)
            }
        )
        recyclerGuru.layoutManager = LinearLayoutManager(this)
        recyclerGuru.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    loadDataGuru(s?.toString())
                }
            }
        })
    }

    private fun loadDataGuru(searchQuery: String? = null) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerGuru.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getAdminGuruList(
                    token = "Bearer $token",
                    search = searchQuery,
                    limit = 100 // Fetch up to 100 or implement pagination later
                )
                
                withContext(Dispatchers.Main) {
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body()
                        val list = body?.data ?: emptyList()
                        guruList.clear()
                        guruList.addAll(list)
                        adapter.submitList(guruList)
                        tvCountInfo.text = "Menampilkan ${list.size} Guru"

                        if (list.isEmpty()) {
                            tvEmptyState.visibility = View.VISIBLE
                            recyclerGuru.visibility = View.GONE
                        } else {
                            tvEmptyState.visibility = View.GONE
                            recyclerGuru.visibility = View.VISIBLE
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = if (!errorBody.isNullOrEmpty()) {
                            try { org.json.JSONObject(errorBody).getString("message") } catch (_: Exception) { errorBody }
                        } else {
                            "Gagal memuat data"
                        }
                        Toast.makeText(this@KelolaGuruAdminActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@KelolaGuruAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showConfirmLepasWaliKelas(guru: GuruItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Lepaskan Wali Kelas")
            .setMessage("Apakah Anda yakin ingin melepaskan ${guru.nama} dari wali kelas ${guru.label_kelas}?")
            .setPositiveButton("Ya") { _, _ ->
                lepasWaliKelas(guru.id_staff)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun lepasWaliKelas(idStaff: Int) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.removeGuruWaliKelas("Bearer $token", idStaff)
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@KelolaGuruAdminActivity, "Berhasil melepaskan wali kelas", Toast.LENGTH_SHORT).show()
                        loadDataGuru(etSearch.text.toString())
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = if (!errorBody.isNullOrEmpty()) {
                            try { org.json.JSONObject(errorBody).getString("message") } catch (_: Exception) { errorBody }
                        } else {
                            "Gagal melepaskan wali kelas"
                        }
                        Toast.makeText(this@KelolaGuruAdminActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@KelolaGuruAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showConfirmDelete(guru: GuruItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Data Guru")
            .setMessage("Apakah Anda yakin ingin menghapus data ${guru.nama}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteGuru(guru.id_staff)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteGuru(idStaff: Int) {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.deleteGuru("Bearer $token", idStaff)
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@KelolaGuruAdminActivity, response.body()?.message ?: "Berhasil menghapus data guru", Toast.LENGTH_SHORT).show()
                        loadDataGuru(etSearch.text.toString())
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = if (!errorBody.isNullOrEmpty()) {
                            try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                        } else {
                            "Gagal menghapus data guru"
                        }
                        Toast.makeText(this@KelolaGuruAdminActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@KelolaGuruAdminActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}