package com.xirpl2.SASMobile.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.PengajuanIzin

class PengajuanIzinAdminAdapter(
    private val onApprove: (PengajuanIzin) -> Unit,
    private val onReject: (PengajuanIzin) -> Unit,
    private val onDetail: (PengajuanIzin) -> Unit
) : ListAdapter<PengajuanIzin, PengajuanIzinAdminAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PengajuanIzin>() {
            override fun areItemsTheSame(oldItem: PengajuanIzin, newItem: PengajuanIzin): Boolean {
                return oldItem.id_pengajuan == newItem.id_pengajuan
            }

            override fun areContentsTheSame(oldItem: PengajuanIzin, newItem: PengajuanIzin): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pengajuan_izin_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        private val tvStudentNisClass: TextView = view.findViewById(R.id.tvStudentNisClass)
        private val statusBadge: TextView = view.findViewById(R.id.statusBadge)
        private val tvLeaveType: TextView = view.findViewById(R.id.tvLeaveType)
        private val tvPeriod: TextView = view.findViewById(R.id.tvPeriod)
        private val tvReason: TextView = view.findViewById(R.id.tvReason)
        private val btnExpandReason: TextView = view.findViewById(R.id.btnExpandReason)

        private val layoutActionsPending: LinearLayout = view.findViewById(R.id.layoutActionsPending)
        private val layoutActionsProcessed: LinearLayout = view.findViewById(R.id.layoutActionsProcessed)

        private val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        private val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        private val btnDetail: MaterialButton = view.findViewById(R.id.btnDetail)
        private val btnViewDetail: MaterialButton = view.findViewById(R.id.btnViewDetail)

        fun bind(item: PengajuanIzin) {
            val context = itemView.context

            tvStudentName.text = item.siswa?.nama_siswa ?: "Unknown"
            tvStudentNisClass.text = "${item.siswa?.nis ?: "-"} \u2022 ${item.siswa?.kelas ?: "-"} ${item.siswa?.jurusan ?: ""}"

            tvLeaveType.text = item.jenisIzin.replaceFirstChar { it.uppercase() }
            setupLeaveTypeBadge(item.jenisIzin)

            tvPeriod.text = "${item.tanggalAwal} s/d ${item.tanggalAkhir}"
            tvReason.text = item.keterangan

            // Expand Reason Logic
            tvReason.maxLines = 2
            btnExpandReason.visibility = if (item.keterangan.length > 60) View.VISIBLE else View.GONE
            btnExpandReason.setOnClickListener {
                if (tvReason.maxLines == 2) {
                    tvReason.maxLines = Int.MAX_VALUE
                    btnExpandReason.text = "Sembunyikan"
                } else {
                    tvReason.maxLines = 2
                    btnExpandReason.text = "Selengkapnya"
                }
            }

            setupStatusBadge(item.status)

            // Conditional rendering for action buttons
            val statusLower = item.status.lowercase()
            if (statusLower == "pending") {
                layoutActionsPending.visibility = View.VISIBLE
                layoutActionsProcessed.visibility = View.GONE
            } else {
                layoutActionsPending.visibility = View.GONE
                layoutActionsProcessed.visibility = View.VISIBLE
            }

            btnApprove.setOnClickListener { onApprove(item) }
            btnReject.setOnClickListener { onReject(item) }
            btnDetail.setOnClickListener { onDetail(item) }
            btnViewDetail.setOnClickListener { onDetail(item) }
        }

        private fun setupLeaveTypeBadge(type: String) {
            when (type.lowercase()) {
                "sakit" -> {
                    tvLeaveType.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                    tvLeaveType.setTextColor(Color.parseColor("#1E40AF"))
                }
                "izin" -> {
                    tvLeaveType.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    tvLeaveType.setTextColor(Color.parseColor("#92400E"))
                }
                else -> {
                    tvLeaveType.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F3E8FF"))
                    tvLeaveType.setTextColor(Color.parseColor("#6B21A8"))
                }
            }
        }

        private fun setupStatusBadge(status: String) {
            statusBadge.text = status.uppercase()
            val context = itemView.context

            when (status.lowercase()) {
                "pending" -> {
                    statusBadge.text = "PENDING"
                    statusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    statusBadge.setTextColor(Color.parseColor("#92400E"))
                }
                "disetujui", "approved" -> {
                    statusBadge.text = "APPROVED"
                    statusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D1FAE5"))
                    statusBadge.setTextColor(Color.parseColor("#065F46"))
                }
                "ditolak", "rejected" -> {
                    statusBadge.text = "REJECTED"
                    statusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    statusBadge.setTextColor(Color.parseColor("#991B1B"))
                }
                else -> {
                    statusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray))
                    statusBadge.setTextColor(Color.WHITE)
                }
            }
        }
    }
}
