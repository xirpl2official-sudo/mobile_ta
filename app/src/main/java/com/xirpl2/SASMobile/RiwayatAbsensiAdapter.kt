package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.RiwayatAbsensi
import com.xirpl2.SASMobile.model.StatusAbsensi

class RiwayatAbsensiAdapter(
    private val riwayatList: List<RiwayatAbsensi>
) : RecyclerView.Adapter<RiwayatAbsensiAdapter.RiwayatViewHolder>() {

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
        val riwayat = riwayatList[position]

        holder.tvTanggal.text = riwayat.tanggal
        holder.tvWaktu.text = riwayat.waktuAbsen ?: "-"
        holder.tvSholat.text = riwayat.namaSholat

        // Set status dengan background dan warna yang berbeda
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
        }
    }

    override fun getItemCount(): Int = riwayatList.size
}