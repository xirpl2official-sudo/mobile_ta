package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.AbsensiStaffItem

class LaporanAbsensiAdapter(
    private var items: List<AbsensiStaffItem> = emptyList(),
    private var pageOffset: Int = 0
) : RecyclerView.Adapter<LaporanAbsensiAdapter.LaporanViewHolder>() {

    class LaporanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
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

        holder.tvNo.text = (pageOffset + position + 1).toString()
        holder.tvTanggal.text = formatTanggal(item.tanggal)
        holder.tvNis.text = item.nis
        holder.tvNama.text = item.nama_siswa

        val kelasDisplay = buildString {
            append(item.kelas ?: "")
            if (!item.jurusan.isNullOrEmpty()) {
                append(" ")
                append(item.jurusan)
            }
        }
        holder.tvKelas.text = kelasDisplay
        holder.tvSholat.text = item.jenis_sholat?.replaceFirstChar { it.uppercase() } ?: "-"

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

    fun updateData(newItems: List<AbsensiStaffItem>, offset: Int = 0) {
        items = newItems
        pageOffset = offset
        notifyDataSetChanged()
    }

    fun clearData() {
        items = emptyList()
        pageOffset = 0
        notifyDataSetChanged()
    }
}
