package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.AbsensiStaffItem
import java.util.Locale

class PresensiAdapter :
    ListAdapter<AbsensiStaffItem, PresensiAdapter.PresensiViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AbsensiStaffItem>() {
            override fun areItemsTheSame(oldItem: AbsensiStaffItem, newItem: AbsensiStaffItem): Boolean {
                return oldItem.id_absen == newItem.id_absen
            }

            override fun areContentsTheSame(oldItem: AbsensiStaffItem, newItem: AbsensiStaffItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class PresensiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
        val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        val tvJenisSholat: TextView = itemView.findViewById(R.id.tvJenisSholat)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnDetail: MaterialButton = itemView.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresensiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_presensi, parent, false)
        return PresensiViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresensiViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvNo.text = (position + 1).toString()
        holder.tvNis.text = item.nis ?: ""
        holder.tvNama.text = item.nama_siswa ?: ""


        val kelasDisplay = buildString {
            append(item.kelas ?: "")
            if (!item.jurusan.isNullOrEmpty()) {
                append(" ")
                append(item.jurusan)
            }
        }
        holder.tvKelas.text = kelasDisplay


        holder.tvJenisSholat.text = item.jenis_sholat ?: "-"


        val status = item.status?.lowercase() ?: "alpha"
        holder.tvStatus.text = status.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }

        val backgroundRes = when (status) {
            "hadir" -> R.drawable.bg_status_hadir
            "izin" -> R.drawable.bg_status_izin
            "sakit" -> R.drawable.bg_status_sakit
            "alpha" -> R.drawable.bg_status_alpha
            else -> R.drawable.bg_status_gray
        }
        holder.tvStatus.setBackgroundResource(backgroundRes)

        // Detail button action for all statuses
        holder.btnDetail.visibility = View.VISIBLE
        holder.btnDetail.setOnClickListener {
            showDetailDialog(holder.itemView.context, item)
        }
    }

    private fun showDetailDialog(context: android.content.Context, item: AbsensiStaffItem) {
        val status = item.status?.lowercase() ?: "alpha"

        val (title, detailText) = when (status) {
            "izin", "sakit" -> {
                val text = if (item.deskripsi.isNullOrBlank()) {
                    "Tidak ada dokumen pendukung tambahan."
                } else {
                    item.deskripsi
                }
                Pair("Dokumen Pendukung - ${item.nama_siswa}", "Status: ${status.uppercase()}\n\nKeterangan:\n$text")
            }
            else -> {
                Pair("Detail Presensi - ${item.nama_siswa}", "Detail Kosong\n(Tidak memiliki dokumen pendukung untuk status ${status.uppercase()})")
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(detailText)
            .setPositiveButton("Tutup", null)
            .show()
    }

    fun updateData(newItems: List<AbsensiStaffItem>) {
        submitList(newItems)
    }

    fun clearData() {
        submitList(emptyList())
    }
}
