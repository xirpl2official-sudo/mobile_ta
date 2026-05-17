package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import com.xirpl2.SASMobile.adapter.SiswaAdapter
import com.xirpl2.SASMobile.model.SiswaItem
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.launch

class SiswaBelumTerdaftarAdminActivity : BaseAdminActivity() {

    private lateinit var rvSiswaBaru: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var acJurusan: AutoCompleteTextView
    private lateinit var acWaliKelas: AutoCompleteTextView
    private lateinit var cbSelectAll: CheckBox
    
    private lateinit var adapter: SiswaAdapter
    private var allStudents = listOf<SiswaItem>()

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.SISWA_BELUM_TERDAFTAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siswa_belum_terdaftar_admin)
        setupStatusBar()

        initializeViews()
        setupRecyclerView()
        setupFilters()
        setupDrawerAndSidebar()
        setupMenuIcon()
        
        loadUnregisteredStudents()
    }

    private fun initializeViews() {
        rvSiswaBaru = findViewById(R.id.rvSiswaBaru)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        etSearch = findViewById(R.id.etSearch)
        acJurusan = findViewById(R.id.acJurusan)
        acWaliKelas = findViewById(R.id.acWaliKelas)
        cbSelectAll = findViewById(R.id.cbSelectAll)

        swipeRefresh.setOnRefreshListener {
            loadUnregisteredStudents()
        }

        findViewById<View>(R.id.toolbar).setOnClickListener { openSidebar() }
    }

    private fun setupRecyclerView() {
        adapter = SiswaAdapter(
            onEditClick = {},
            onDeleteClick = {},
            onDetailClick = { siswa ->
                val dialog = SiswaDetailDialogFragment.newInstance(siswa)
                dialog.show(supportFragmentManager, "SiswaDetail")
            },
            isReadOnly = true
        )
        rvSiswaBaru.layoutManager = LinearLayoutManager(this)
        rvSiswaBaru.adapter = adapter
    }

    private fun setupFilters() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Dummy options for Jurusan and Wali Kelas as per spec
        val jurusanOptions = listOf("Semua Jurusan", "TAV", "RPL", "TKJ", "MM")
        val waliOptions = listOf("Semua Wali Kelas", "Bu Ani", "Pak Budi", "Ibu Siti")

        val jurusanAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jurusanOptions)
        acJurusan.setAdapter(jurusanAdapter)
        acJurusan.setText(jurusanOptions[0], false)
        acJurusan.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        val waliAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, waliOptions)
        acWaliKelas.setAdapter(waliAdapter)
        acWaliKelas.setText(waliOptions[0], false)
        acWaliKelas.setOnItemClickListener { _, _, _, _ -> applyFilters() }
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().lowercase()
        val selectedJurusan = acJurusan.text.toString()
        
        val filteredList = allStudents.filter { student ->
            val matchesSearch = student.nama_siswa.lowercase().contains(query) || 
                               student.nis.contains(query)
            
            val matchesJurusan = selectedJurusan == "Semua Jurusan" || 
                                student.jurusan.equals(selectedJurusan, ignoreCase = true)
            
            matchesSearch && matchesJurusan
        }

        adapter.submitList(filteredList)
        layoutEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadUnregisteredStudents() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnregisteredStudents("Bearer $token")
                
                runOnUiThread {
                    showLoading(false)
                    if (response.isSuccessful) {
                        allStudents = response.body()?.data ?: emptyList()
                        applyFilters()
                    } else {
                        Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, 
                            "Gagal mengambil data: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this@SiswaBelumTerdaftarAdminActivity, 
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        if (isLoading) {
            layoutEmpty.visibility = View.GONE
        } else {
            swipeRefresh.isRefreshing = false
        }
    }
}