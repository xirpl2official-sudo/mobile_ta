package com.xirpl2.SASMobile.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.PengajuanIzin
import java.text.SimpleDateFormat
import java.util.Locale

class RiwayatIzinAdapter :
    ListAdapter<PengajuanIzin, RiwayatIzinAdapter.ViewHolder>(DIFF_CALLBACK) {

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
            .inflate(R.layout.item_riwayat_izin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvIzinName: TextView = view.findViewById(R.id.tvIzinName)
        private val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        private val tvPeriod: TextView = view.findViewById(R.id.tvPeriod)
        private val ivAttachment: ImageView = view.findViewById(R.id.ivAttachment)

        fun bind(item: PengajuanIzin) {
            tvIzinName.text = item.jenisIzin.replaceFirstChar { it.uppercase() }
            tvPeriod.text = "${formatDate(item.tanggalAwal)} - ${formatDate(item.tanggalAkhir)}"

            setupStatusBadge(item.status)

            ivAttachment.visibility = if (!item.buktiFoto.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        private fun formatDate(dateStr: String): String {
            return try {
                val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val output = SimpleDateFormat("dd MMM yyyy", Locale("id"))
                output.format(input.parse(dateStr)!!)
            } catch (_: Exception) {
                dateStr
            }
        }

        private fun setupStatusBadge(status: String) {
            when (status.lowercase()) {
                "pending" -> {
                    tvStatusBadge.text = "MENUNGGU"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_pending)
                    tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                }
                "disetujui", "approved" -> {
                    tvStatusBadge.text = "DISETUJUI"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_approved)
                    tvStatusBadge.setTextColor(Color.parseColor("#065F46"))
                }
                "ditolak", "rejected" -> {
                    tvStatusBadge.text = "DITOLAK"
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_rejected)
                    tvStatusBadge.setTextColor(Color.parseColor("#991B1B"))
                }
                else -> {
                    tvStatusBadge.text = status.uppercase()
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_pending)
                    tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                }
            }
        }
    }
}
