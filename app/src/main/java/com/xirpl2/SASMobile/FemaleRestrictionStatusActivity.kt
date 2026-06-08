package com.xirpl2.SASMobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.xirpl2.SASMobile.model.FemaleRestriction
import com.xirpl2.SASMobile.model.FemaleTeacherInfo
import com.xirpl2.SASMobile.repository.FemaleRestrictionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class FemaleRestrictionStatusActivity : BaseSiswaActivity() {

    private lateinit var cardRestrictionStatus: MaterialCardView
    private lateinit var tvRestrictionTitle: TextView
    private lateinit var tvRestrictionSubtitle: TextView
    private lateinit var tvRemainingDays: TextView
    private lateinit var tvExpiresDate: TextView
    private lateinit var btnRequestApproval: MaterialButton
    private lateinit var cardPendingRequest: MaterialCardView
    private lateinit var tvPendingApprover: TextView
    private lateinit var tvPendingDate: TextView
    private lateinit var tvPendingStatus: TextView
    private lateinit var progressHistory: ProgressBar
    private lateinit var tvEmptyHistory: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val repository = FemaleRestrictionRepository()
    private val historyAdapter = FemaleRestrictionHistoryAdapter(::formatDate)

    override fun getCurrentMenuItem(): SiswaMenuItem = SiswaMenuItem.PENGAJUAN_IZIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_female_restriction_status)
        setupStatusBar()

        findViewById<android.view.View>(R.id.topBarContent)?.let { topBar ->
            applyEdgeToEdge(topBar)
        }

        initializeViews()
        setupDrawerAndSidebar()
        setupMenuIcon()
        setupRequestButton()

        swipeRefresh.setOnRefreshListener {
            loadRestrictionStatus()
            loadHistory()
        }

        loadRestrictionStatus()
        loadHistory()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@FemaleRestrictionStatusActivity, BerandaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        })
    }

    private fun initializeViews() {
        cardRestrictionStatus = findViewById(R.id.cardRestrictionStatus)
        tvRestrictionTitle = findViewById(R.id.tvRestrictionTitle)
        tvRestrictionSubtitle = findViewById(R.id.tvRestrictionSubtitle)
        tvRemainingDays = findViewById(R.id.tvRemainingDays)
        tvExpiresDate = findViewById(R.id.tvExpiresDate)
        btnRequestApproval = findViewById(R.id.btnRequestApproval)
        cardPendingRequest = findViewById(R.id.cardPendingRequest)
        tvPendingApprover = findViewById(R.id.tvPendingApprover)
        tvPendingDate = findViewById(R.id.tvPendingDate)
        tvPendingStatus = findViewById(R.id.tvPendingStatus)
        progressHistory = findViewById(R.id.progressHistory)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        rvHistory = findViewById(R.id.rvHistory)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter
    }

    private fun loadRestrictionStatus() {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getRestrictionStatus(token)
            }

            if (isFinishing || isDestroyed) return@launch

            result.onSuccess { status ->
                if (status.isRestricted) {
                    cardRestrictionStatus.visibility = View.VISIBLE
                    cardRestrictionStatus.setCardBackgroundColor(Color.parseColor("#DC2626"))
                    tvRestrictionTitle.text = "Sedang dalam masa halangan"
                    tvRestrictionSubtitle.text = "Anda tidak dapat melakukan scan barcode"
                    tvRemainingDays.text = status.restriction?.remainingDays?.toString() ?: "0"
                    tvExpiresDate.text = formatDate(status.restriction?.expiresAt ?: "")
                    btnRequestApproval.isEnabled = true
                } else {
                    cardRestrictionStatus.visibility = View.VISIBLE
                    cardRestrictionStatus.setCardBackgroundColor(Color.parseColor("#059669"))
                    tvRestrictionTitle.text = "Tidak ada halangan"
                    tvRestrictionSubtitle.text = "Anda dapat melakukan scan barcode"
                    tvRemainingDays.text = "0"
                    tvExpiresDate.text = "--"
                    btnRequestApproval.isEnabled = false
                }

                if (status.pendingRequest != null) {
                    cardPendingRequest.visibility = View.VISIBLE
                    tvPendingApprover.text = "Diajukan ke: ${status.pendingRequest.approverName ?: "--"}"
                    tvPendingDate.text = "Tanggal: ${formatDate(status.pendingRequest.createdAt)}"
                    tvPendingStatus.text = "Status: ${status.pendingRequest.status.replaceFirstChar { it.uppercase() }}"
                } else {
                    cardPendingRequest.visibility = View.GONE
                }
            }

            result.onFailure {
                if (isFinishing || isDestroyed) return@onFailure
                cardRestrictionStatus.visibility = View.VISIBLE
                cardRestrictionStatus.setCardBackgroundColor(Color.parseColor("#6B7280"))
                tvRestrictionTitle.text = "Gagal memuat status"
                tvRestrictionSubtitle.text = it.message ?: "Terjadi kesalahan"
                tvRemainingDays.text = "--"
                tvExpiresDate.text = "--"
                btnRequestApproval.isEnabled = false
            }
        }
    }

    private fun loadHistory() {
        progressHistory.visibility = View.VISIBLE
        rvHistory.visibility = View.GONE
        tvEmptyHistory.visibility = View.GONE

        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getHistory(token)
            }

            if (isFinishing || isDestroyed) return@launch

            swipeRefresh.isRefreshing = false
            progressHistory.visibility = View.GONE

            result.onSuccess { data ->
                if (data.isEmpty()) {
                    tvEmptyHistory.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    tvEmptyHistory.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                    historyAdapter.submitList(data)
                }
            }

            result.onFailure {
                tvEmptyHistory.visibility = View.VISIBLE
                tvEmptyHistory.text = "Gagal memuat riwayat"
                rvHistory.visibility = View.GONE
            }
        }
    }

    private fun setupRequestButton() {
        btnRequestApproval.setOnClickListener {
            showTeacherSelectionDialog()
        }
    }

    private fun showTeacherSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_female_teacher, null)
        val progressTeachers = dialogView.findViewById<ProgressBar>(R.id.progressTeachers)
        val tvEmptyTeachers = dialogView.findViewById<TextView>(R.id.tvEmptyTeachers)
        val rvTeachers = dialogView.findViewById<RecyclerView>(R.id.rvTeachers)
        val tilCatatan = dialogView.findViewById<TextInputLayout>(R.id.tilCatatan)
        val etCatatan = dialogView.findViewById<TextInputEditText>(R.id.etCatatan)

        rvTeachers.layoutManager = LinearLayoutManager(this)
        val teacherAdapter = TeacherRadioAdapter()
        rvTeachers.adapter = teacherAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Kirim", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val selectedTeacher = teacherAdapter.getSelectedTeacher()
                if (selectedTeacher == null) {
                    Toast.makeText(this, "Pilih guru terlebih dahulu", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val catatan = etCatatan.text.toString().trim()
                dialog.dismiss()
                submitApprovalRequest(selectedTeacher.idStaff, catatan.ifEmpty { null })
            }
        }

        dialog.show()

        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        lifecycleScope.launch {
            progressTeachers.visibility = View.VISIBLE
            rvTeachers.visibility = View.GONE
            tvEmptyTeachers.visibility = View.GONE

            val result = withContext(Dispatchers.IO) {
                repository.getFemaleTeachers(token)
            }

            if (isFinishing || isDestroyed) return@launch

            progressTeachers.visibility = View.GONE

            result.onSuccess { teachers ->
                if (teachers.isEmpty()) {
                    tvEmptyTeachers.visibility = View.VISIBLE
                    rvTeachers.visibility = View.GONE
                } else {
                    tvEmptyTeachers.visibility = View.GONE
                    rvTeachers.visibility = View.VISIBLE
                    teacherAdapter.submitList(teachers)
                }
            }

            result.onFailure {
                tvEmptyTeachers.visibility = View.VISIBLE
                tvEmptyTeachers.text = "Gagal memuat daftar guru"
                rvTeachers.visibility = View.GONE
            }
        }
    }

    private fun submitApprovalRequest(teacherId: Int, catatan: String?) {
        val token = com.xirpl2.SASMobile.utils.SecurePreferences.getUserData(this)
            .getString("auth_token", "") ?: ""

        btnRequestApproval.isEnabled = false
        btnRequestApproval.text = "Mengirim..."

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.createApprovalRequest(token, teacherId, catatan)
            }

            if (isFinishing || isDestroyed) return@launch

            btnRequestApproval.isEnabled = true
            btnRequestApproval.text = "Ajukan ke Guru Perempuan"

            result.onSuccess { message ->
                Toast.makeText(this@FemaleRestrictionStatusActivity, message, Toast.LENGTH_SHORT).show()
                loadRestrictionStatus()
                loadHistory()
            }

            result.onFailure {
                Toast.makeText(
                    this@FemaleRestrictionStatusActivity,
                    it.message ?: "Gagal mengirim pengajuan",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("d MMMM yyyy", Locale("id"))
            val parsed = input.parse(dateStr)
            if (parsed != null) output.format(parsed) else dateStr
        } catch (_: Exception) {
            try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val output = SimpleDateFormat("d MMMM yyyy", Locale("id"))
                val parsed = input.parse(dateStr)
                if (parsed != null) output.format(parsed) else dateStr
            } catch (_: Exception) {
                dateStr
            }
        }
    }

    private object HistoryDiffCallback : DiffUtil.ItemCallback<FemaleRestriction>() {
        override fun areItemsTheSame(oldItem: FemaleRestriction, newItem: FemaleRestriction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FemaleRestriction, newItem: FemaleRestriction): Boolean {
            return oldItem == newItem
        }
    }

    private class FemaleRestrictionHistoryAdapter(
        private val dateFormatter: (String) -> String
    ) : ListAdapter<FemaleRestriction, FemaleRestrictionHistoryAdapter.ViewHolder>(HistoryDiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restriction_approval, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
            private val tvStudentInfo: TextView = view.findViewById(R.id.tvStudentInfo)
            private val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
            private val tvRequestDate: TextView = view.findViewById(R.id.tvRequestDate)
            private val tvExpiresDate: TextView = view.findViewById(R.id.tvExpiresDate)
            private val tvApproverName: TextView = view.findViewById(R.id.tvApproverName)
            private val layoutActions: View = view.findViewById(R.id.layoutActions)

            fun bind(item: FemaleRestriction) {
                tvStudentName.text = "Halangan Aktif"
                tvStudentInfo.text = "Berlaku ${item.remainingDays} hari lagi"
                tvRequestDate.text = dateFormatter(item.restrictedAt)
                tvExpiresDate.text = dateFormatter(item.expiresAt)
                layoutActions.visibility = View.GONE
                tvApproverName.visibility = View.GONE

                when (item.status.lowercase()) {
                    "active" -> {
                        tvStatusBadge.text = "AKTIF"
                        tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_approved)
                        tvStatusBadge.setTextColor(Color.parseColor("#065F46"))
                    }
                    "expired" -> {
                        tvStatusBadge.text = "BERAKHIR"
                        tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_pending)
                        tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                    }
                    else -> {
                        tvStatusBadge.text = item.status.uppercase()
                        tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_pending)
                        tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                    }
                }
            }
        }
    }

    private class TeacherRadioAdapter :
        ListAdapter<FemaleTeacherInfo, TeacherRadioAdapter.ViewHolder>(DIFF_CALLBACK) {

        private var selectedPosition = -1

        companion object {
            private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FemaleTeacherInfo>() {
                override fun areItemsTheSame(oldItem: FemaleTeacherInfo, newItem: FemaleTeacherInfo): Boolean {
                    return oldItem.idStaff == newItem.idStaff
                }

                override fun areContentsTheSame(oldItem: FemaleTeacherInfo, newItem: FemaleTeacherInfo): Boolean {
                    return oldItem == newItem
                }
            }
        }

        fun getSelectedTeacher(): FemaleTeacherInfo? {
            if (selectedPosition == -1 || selectedPosition >= itemCount) return null
            return getItem(selectedPosition)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val radio = RadioButton(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(32, 24, 32, 24)
                textSize = 14f
            }
            return ViewHolder(radio)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val radioButton: RadioButton) :
            RecyclerView.ViewHolder(radioButton) {

            fun bind(item: FemaleTeacherInfo) {
                val displayText = buildString {
                    append(item.nama)
                    if (!item.nip.isNullOrEmpty()) {
                        append(" (${item.nip})")
                    }
                }
                radioButton.text = displayText
                radioButton.isChecked = adapterPosition == selectedPosition

                radioButton.setOnClickListener {
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition
                    if (previousPosition != -1) notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                }
            }
        }
    }
}
