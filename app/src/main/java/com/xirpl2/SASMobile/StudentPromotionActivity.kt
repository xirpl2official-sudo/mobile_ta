package com.xirpl2.SASMobile

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xirpl2.SASMobile.model.BulkProgressionRequest
import com.xirpl2.SASMobile.model.PromotionRequest
import com.xirpl2.SASMobile.model.StudentTransition
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentPromotionActivity : BaseAdminActivity() {

    private lateinit var spinnerDariKelas: AutoCompleteTextView
    private lateinit var spinnerKeKelas: AutoCompleteTextView
    private lateinit var tvStudentCount: TextView
    private lateinit var recyclerStudents: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var emptyState: View
    private lateinit var tvEmptyState: TextView
    private lateinit var btnBulkProgression: MaterialButton

    // Simulation views
    private lateinit var btnSimulasi: MaterialButton
    private lateinit var progressSimulasi: ProgressBar
    private lateinit var tvSimulasiError: TextView
    private lateinit var layoutSimulasiResults: LinearLayout
    private lateinit var tvSimPromoted: TextView
    private lateinit var tvSimGraduated: TextView
    private lateinit var tvSimMutasi: TextView
    private lateinit var tvSimKeluar: TextView
    private lateinit var tvSimTetap: TextView

    private lateinit var adapter: StudentTransitionAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var allTransitions = listOf<StudentTransition>()
    private var filteredTransitions = listOf<StudentTransition>()
    private var kelasOptions = listOf<String>()

    private var selectedDariKelas: String? = null
    private var selectedKeKelas: String? = null

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.KENAIKAN_KELAS

    override fun getNavigationViewId(): Int = R.id.navigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_promotion)
        setupStatusBar()

        val topBarContent = findViewById<View>(R.id.topBarContent)
        applyEdgeToEdge(topBarContent)

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRecyclerView()
        setupListeners()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { loadTransitions() }

        loadTransitions()
    }

    private fun initializeViews() {
        spinnerDariKelas = findViewById(R.id.spinnerDariKelas)
        spinnerKeKelas = findViewById(R.id.spinnerKeKelas)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        recyclerStudents = findViewById(R.id.recyclerStudents)
        progressLoading = findViewById(R.id.progressLoading)
        emptyState = findViewById(R.id.emptyState)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnBulkProgression = findViewById(R.id.btnBulkProgression)

        // Simulation views
        btnSimulasi = findViewById(R.id.btnSimulasi)
        progressSimulasi = findViewById(R.id.progressSimulasi)
        tvSimulasiError = findViewById(R.id.tvSimulasiError)
        layoutSimulasiResults = findViewById(R.id.layoutSimulasiResults)
        tvSimPromoted = findViewById(R.id.tvSimPromoted)
        tvSimGraduated = findViewById(R.id.tvSimGraduated)
        tvSimMutasi = findViewById(R.id.tvSimMutasi)
        tvSimKeluar = findViewById(R.id.tvSimKeluar)
        tvSimTetap = findViewById(R.id.tvSimTetap)
    }

    private fun setupRecyclerView() {
        adapter = StudentTransitionAdapter()
        recyclerStudents.layoutManager = LinearLayoutManager(this)
        recyclerStudents.adapter = adapter
    }

    private fun setupListeners() {
        spinnerDariKelas.setOnItemClickListener { _, _, position, _ ->
            selectedDariKelas = kelasOptions[position]
            updateFilteredList()
            updateKeKelasOptions()
            updateButtonState()
        }

        spinnerKeKelas.setOnItemClickListener { _, _, position, _ ->
            selectedKeKelas = spinnerKeKelas.adapter.getItem(position).toString()
            updateButtonState()
        }

        btnBulkProgression.setOnClickListener {
            showConfirmationDialog()
        }

        btnSimulasi.setOnClickListener {
            runSimulation()
        }
    }

    private fun loadTransitions() {
        val token = getAuthToken()
        if (token.isEmpty()) return

        progressLoading.visibility = View.VISIBLE
        recyclerStudents.visibility = View.GONE
        emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getStudentTransitions("Bearer $token")
                }

                progressLoading.visibility = View.GONE
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false

                if (response.isSuccessful) {
                    allTransitions = response.body()?.data ?: emptyList()
                    kelasOptions = allTransitions.map { it.kelas_sekarang }.distinct().sorted()

                    val kelasAdapter = ArrayAdapter(this@StudentPromotionActivity,
                        android.R.layout.simple_dropdown_item_1line, kelasOptions)
                    spinnerDariKelas.setAdapter(kelasAdapter)

                    if (allTransitions.isEmpty()) {
                        showEmptyState("Belum ada data transisi siswa")
                    } else {
                        tvStudentCount.text = "Total ${allTransitions.size} siswa tersedia untuk promosi"
                        emptyState.visibility = View.GONE
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Gagal memuat data transisi"
                    showEmptyState(errorMsg)
                }
            } catch (e: Exception) {
                if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
                progressLoading.visibility = View.GONE
                showEmptyState("Error: ${e.message}")
            }
        }
    }

    private fun updateFilteredList() {
        val dariKelas = selectedDariKelas ?: return

        filteredTransitions = allTransitions.filter { it.kelas_sekarang == dariKelas }
        adapter.submitList(filteredTransitions)

        tvStudentCount.text = "${filteredTransitions.size} siswa di kelas $dariKelas"

        if (filteredTransitions.isEmpty()) {
            recyclerStudents.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Tidak ada siswa di kelas $dariKelas"
        } else {
            recyclerStudents.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun updateKeKelasOptions() {
        val dariKelas = selectedDariKelas ?: return

        // Get suggested target classes from transitions, or generate standard progression
        val suggestedTargets = filteredTransitions
            .map { it.kelas_tujuan }
            .distinct()
            .sorted()

        // Also add standard class progression options
        val allTargets = (suggestedTargets + generateTargetOptions(dariKelas)).distinct().sorted()

        val keKelasAdapter = ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, allTargets)
        spinnerKeKelas.setAdapter(keKelasAdapter)

        // Auto-select if there's only one suggested target
        if (suggestedTargets.size == 1) {
            spinnerKeKelas.setText(suggestedTargets[0], false)
            selectedKeKelas = suggestedTargets[0]
        } else {
            spinnerKeKelas.setText("", false)
            selectedKeKelas = null
        }
    }

    private fun generateTargetOptions(dariKelas: String): List<String> {
        // Extract the class number and generate next level
        val options = mutableListOf<String>()
        val parts = dariKelas.split(" ", limit = 2)
        if (parts.isNotEmpty()) {
            val num = parts[0].toIntOrNull()
            if (num != null) {
                val suffix = if (parts.size > 1) " ${parts[1]}" else ""
                val nextNum = num + 1
                if (nextNum <= 12) {
                    options.add("$nextNum$suffix")
                } else {
                    options.add("Alumni")
                }
            }
        }
        return options
    }

    private fun updateButtonState() {
        val enabled = selectedDariKelas != null &&
                selectedKeKelas != null &&
                filteredTransitions.isNotEmpty()
        btnBulkProgression.isEnabled = enabled
        btnBulkProgression.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun showConfirmationDialog() {
        val dariKelas = selectedDariKelas ?: return
        val keKelas = selectedKeKelas ?: return
        val count = filteredTransitions.size

        MaterialAlertDialogBuilder(this)
            .setTitle("Konfirmasi Promosi")
            .setMessage("Promosikan $count siswa dari kelas $dariKelas ke kelas $keKelas?\n\nTindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Promosikan") { _, _ ->
                executeBulkProgression()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeBulkProgression() {
        val token = getAuthToken()
        val keKelas = selectedKeKelas ?: return

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        val nisList = adapter.getStudentNisList()
        if (nisList.isEmpty()) {
            Toast.makeText(this, "Tidak ada siswa yang dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val request = BulkProgressionRequest(
            student_ids = nisList,
            target_kelas = keKelas,
            action = "promote"
        )

        btnBulkProgression.isEnabled = false
        btnBulkProgression.text = "Memproses..."
        progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.bulkStudentProgression("Bearer $token", request)
                }

                progressLoading.visibility = View.GONE
                btnBulkProgression.text = "Promosikan Semua Siswa"
                updateButtonState()

                if (response.isSuccessful) {
                    val body = response.body()
                    val message = body?.message ?: "Promosi berhasil"
                    showResultDialog(
                        success = true,
                        title = "Promosi Berhasil",
                        message = "$message\n\n${nisList.size} siswa telah dipromosikan ke kelas $keKelas"
                    )
                    // Reload data
                    loadTransitions()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        org.json.JSONObject(errorBody ?: "").optString("message", "Gagal melakukan promosi")
                    } catch (e: Exception) {
                        "Gagal melakukan promosi (${response.code()})"
                    }
                    showResultDialog(
                        success = false,
                        title = "Promosi Gagal",
                        message = errorMsg
                    )
                }
            } catch (e: Exception) {
                progressLoading.visibility = View.GONE
                btnBulkProgression.text = "Promosikan Semua Siswa"
                updateButtonState()
                showResultDialog(
                    success = false,
                    title = "Error",
                    message = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    private fun showResultDialog(success: Boolean, title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEmptyState(message: String) {
        recyclerStudents.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        tvEmptyState.text = message
    }

    private fun runSimulation() {
        val token = getAuthToken()
        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset UI
        layoutSimulasiResults.visibility = View.GONE
        tvSimulasiError.visibility = View.GONE
        progressSimulasi.visibility = View.VISIBLE
        btnSimulasi.isEnabled = false
        btnSimulasi.text = "Memproses..."

        lifecycleScope.launch {
            try {
                val request = PromotionRequest(exceptions = emptyList())
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.simulatePromotion("Bearer $token", request)
                }

                progressSimulasi.visibility = View.GONE
                btnSimulasi.isEnabled = true
                btnSimulasi.text = "Simulasi Kenaikan"

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success" && body.data != null) {
                        val data = body.data!!
                        tvSimPromoted.text = data.promotedCount.toString()
                        tvSimGraduated.text = data.graduatedCount.toString()
                        tvSimMutasi.text = data.mutasiCount.toString()
                        tvSimKeluar.text = data.keluarCount.toString()
                        tvSimTetap.text = data.tetapCount.toString()
                        layoutSimulasiResults.visibility = View.VISIBLE
                    } else {
                        tvSimulasiError.text = body?.message ?: "Gagal menjalankan simulasi"
                        tvSimulasiError.visibility = View.VISIBLE
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Tahun ajaran sudah diproses atau tidak valid"
                        404 -> "Tahun ajaran aktif tidak ditemukan"
                        else -> "Error: ${response.code()}"
                    }
                    tvSimulasiError.text = errorMsg
                    tvSimulasiError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progressSimulasi.visibility = View.GONE
                btnSimulasi.isEnabled = true
                btnSimulasi.text = "Simulasi Kenaikan"
                tvSimulasiError.text = "Koneksi gagal: ${e.message}"
                tvSimulasiError.visibility = View.VISIBLE
            }
        }
    }
}
