package com.xirpl2.SASMobile.adapter

import android.content.res.ColorStateList
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
import com.xirpl2.SASMobile.model.ApprovalRequest

class FemaleRestrictionApprovalAdapter(
    private val onApprove: (ApprovalRequest) -> Unit,
    private val onReject: (ApprovalRequest) -> Unit,
    private val onDetail: (ApprovalRequest) -> Unit
) : ListAdapter<ApprovalRequest, FemaleRestrictionApprovalAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ApprovalRequest>() {
            override fun areItemsTheSame(oldItem: ApprovalRequest, newItem: ApprovalRequest): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ApprovalRequest, newItem: ApprovalRequest): Boolean {
                return oldItem == newItem
            }
        }
    }

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
        private val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        private val btnDetail: MaterialButton = view.findViewById(R.id.btnDetail)
        private val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
        private val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)

        fun bind(item: ApprovalRequest) {
            tvStudentName.text = item.siswaName ?: "Tidak Diketahui"
            tvStudentInfo.text = "${item.siswaNis ?: "-"} \u2022 ${item.siswaKelas ?: "-"}"

            setupStatusBadge(item.status)

            tvRequestDate.text = formatDisplayDate(item.createdAt)
            tvExpiresDate.text = item.expiresAt?.let { formatDisplayDate(it) } ?: "--"

            val statusLower = item.status.lowercase()
            if (statusLower == "pending") {
                layoutActions.visibility = View.VISIBLE
                tvApproverName.visibility = View.GONE
            } else {
                layoutActions.visibility = View.GONE
                tvApproverName.visibility = View.VISIBLE
                tvApproverName.text = "Dinilai oleh: ${item.approverName ?: "--"}"
            }

            btnApprove.setOnClickListener { onApprove(item) }
            btnReject.setOnClickListener { onReject(item) }
            btnDetail.setOnClickListener { onDetail(item) }
        }

        private fun setupStatusBadge(status: String) {
            val ctx = itemView.context
            when (status.lowercase()) {
                "pending" -> {
                    tvStatusBadge.text = "PENDING"
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_warning))
                    tvStatusBadge.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, android.R.color.white)))
                }
                "disetujui", "approved" -> {
                    tvStatusBadge.text = "DISETUJUI"
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_success))
                    tvStatusBadge.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, android.R.color.white)))
                }
                "ditolak", "rejected" -> {
                    tvStatusBadge.text = "DITOLAK"
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_error))
                    tvStatusBadge.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, android.R.color.white)))
                }
                else -> {
                    tvStatusBadge.text = status.uppercase()
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.gray))
                    tvStatusBadge.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, android.R.color.white)))
                }
            }
        }

        private fun formatDisplayDate(dateStr: String): String {
            return try {
                val parts = dateStr.split("T").first().split("-")
                if (parts.size == 3) {
                    val year = parts[0]
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    val monthNames = arrayOf(
                        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )
                    "$day ${monthNames[month]} $year"
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}
