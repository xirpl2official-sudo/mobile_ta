package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.AbsensiStaffItem

/**
 * Adapter for displaying attendance records in the Laporan (Report) table
 */
class LaporanAbsensiAdapter(
    private var items: List<AbsensiStaffItem> = emptyList()
) : RecyclerView.Adapter<LaporanAbsensiAdapter.LaporanViewHolder>() {

    class LaporanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        val tvSholat: TextView = itemView.findViewById(R.id.tvSholat)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_laporan_absensi, parent, false)
        return LaporanViewHolder(view)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        val item = items[position]
        
        // Format tanggal (dari "2025-01-28" ke "28/01/2025")
        holder.tvTanggal.text = formatTanggal(item.tanggal)
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
        
        // Jenis sholat
        holder.tvSholat.text = item.jenis_sholat?.replaceFirstChar { it.uppercase() } ?: "-"
        
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
     * Format tanggal dari "2025-01-28" ke "28/01/2025"
     */
    private fun formatTanggal(tanggal: String): String {
        return try {
            val parts = tanggal.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } else {
                tanggal
            }
        } catch (e: Exception) {
            tanggal
        }
    }

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
