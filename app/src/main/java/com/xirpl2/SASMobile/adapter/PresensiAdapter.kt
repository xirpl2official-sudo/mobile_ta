package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.AbsensiStaffItem

/**
 * Adapter for displaying attendance records in PresensiSholatAdminActivity
 */
class PresensiAdapter(
    private var items: List<AbsensiStaffItem> = emptyList()
) : RecyclerView.Adapter<PresensiAdapter.PresensiViewHolder>() {

    class PresensiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
        val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        val tvJenisSholat: TextView = itemView.findViewById(R.id.tvJenisSholat)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresensiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_presensi, parent, false)
        return PresensiViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresensiViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvNo.text = (position + 1).toString()
        holder.tvNis.text = item.nis
        holder.tvNama.text = item.nama_siswa
        
        // Format kelas with jurusan
        val kelasDisplay = buildString {
            append(item.kelas ?: "")
            if (!item.jurusan.isNullOrEmpty()) {
                append(" ")
                append(item.jurusan)
            }
        }
        holder.tvKelas.text = kelasDisplay

        // Set jenis sholat
        holder.tvJenisSholat.text = item.jenis_sholat ?: "-"
        
        // Set status with colored background
        val status = item.status.lowercase()
        holder.tvStatus.text = status.replaceFirstChar { it.uppercase() }
        
        val backgroundRes = when (status) {
            "hadir" -> R.drawable.bg_status_hadir
            "izin" -> R.drawable.bg_status_izin
            "sakit" -> R.drawable.bg_status_sakit
            "alpha" -> R.drawable.bg_status_alpha
            else -> R.drawable.bg_status_gray
        }
        holder.tvStatus.setBackgroundResource(backgroundRes)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Update the adapter with new data
     */
    fun updateData(newItems: List<AbsensiStaffItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Clear all data
     */
    fun clearData() {
        items = emptyList()
        notifyDataSetChanged()
    }
}
