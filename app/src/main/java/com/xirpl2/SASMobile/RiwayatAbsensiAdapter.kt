package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.RiwayatAbsensi
import com.xirpl2.SASMobile.model.StatusAbsensi

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class RiwayatAbsensiAdapter : ListAdapter<RiwayatAbsensi, RiwayatAbsensiAdapter.RiwayatViewHolder>(RiwayatDiffCallback()) {

    inner class RiwayatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
        val tvSholat: TextView = itemView.findViewById(R.id.tvSholat)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RiwayatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_absensi, parent, false)
        return RiwayatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RiwayatViewHolder, position: Int) {
        val riwayat = getItem(position)

        holder.tvTanggal.text = riwayat.tanggal
        holder.tvWaktu.text = riwayat.waktuAbsen ?: "-"
        holder.tvSholat.text = riwayat.namaSholat

        
        when (riwayat.status) {
            StatusAbsensi.HADIR -> {
                holder.tvStatus.text = "Hadir"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_hadir)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
            StatusAbsensi.ALPHA -> {
                holder.tvStatus.text = "Alpha"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_alpha)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
            StatusAbsensi.SAKIT -> {
                holder.tvStatus.text = "Sakit"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_sakit)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
            StatusAbsensi.IZIN -> {
                holder.tvStatus.text = "Izin"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_izin)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
            StatusAbsensi.UNKNOWN -> {
                holder.tvStatus.text = "-"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_alpha)
                holder.tvStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.white)
                )
            }
        }
    }
}

class RiwayatDiffCallback : DiffUtil.ItemCallback<RiwayatAbsensi>() {
    override fun areItemsTheSame(oldItem: RiwayatAbsensi, newItem: RiwayatAbsensi): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RiwayatAbsensi, newItem: RiwayatAbsensi): Boolean {
        return oldItem == newItem
    }
}