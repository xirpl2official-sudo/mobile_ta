package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.AbsensiStaffItem

class PresensiAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_FOOTER = 1
    }

    enum class FooterState {
        HIDDEN, LOADING, NO_MORE
    }

    private val items = mutableListOf<AbsensiStaffItem>()
    var footerState: FooterState = FooterState.HIDDEN
        set(value) {
            if (field != value) {
                val oldHadFooter = field != FooterState.HIDDEN
                val newHasFooter = value != FooterState.HIDDEN
                field = value
                if (oldHadFooter != newHasFooter) {
                    if (newHasFooter) notifyItemInserted(items.size)
                    else notifyItemRemoved(items.size)
                } else if (oldHadFooter) {
                    notifyItemChanged(items.size)
                }
            }
        }

    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) VIEW_TYPE_ITEM else VIEW_TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_presensi, parent, false)
            PresensiViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pagination_footer, parent, false)
            FooterViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PresensiViewHolder) {
            bindItem(holder, items[position], position)
        } else if (holder is FooterViewHolder) {
            bindFooter(holder)
        }
    }

    override fun getItemCount(): Int {
        return items.size + if (footerState != FooterState.HIDDEN) 1 else 0
    }

    private fun bindItem(holder: PresensiViewHolder, item: AbsensiStaffItem, position: Int) {
        holder.tvNo.text = (position + 1).toString()
        holder.tvNis.text = item.nis
        holder.tvNama.text = item.nama_siswa

        val kelasVal = item.kelas ?: ""
        val jurusanVal = item.jurusan ?: ""
        val kelasDisplay = if (jurusanVal.isNotEmpty() && kelasVal.contains(jurusanVal)) {
            kelasVal
        } else if (jurusanVal.isNotEmpty()) {
            "$kelasVal $jurusanVal"
        } else {
            kelasVal
        }
        holder.tvKelas.text = kelasDisplay

        holder.tvJenisSholat.text = item.jenis_sholat ?: "-"

        val tanggal = item.tanggal
        holder.tvTanggal.text = try {
            val inp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val out = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("in", "ID"))
            inp.parse(tanggal)?.let { out.format(it) } ?: tanggal
        } catch (e: Exception) { tanggal }

        val status = item.status.lowercase()
        val statusDisplay = status.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
        holder.tvStatus.text = statusDisplay

        val backgroundRes = when (status) {
            "hadir" -> R.drawable.bg_status_hadir
            "izin" -> R.drawable.bg_status_izin
            "sakit" -> R.drawable.bg_status_sakit
            "alpha" -> R.drawable.bg_status_alpha
            else -> R.drawable.bg_status_gray
        }
        holder.tvStatus.setBackgroundResource(backgroundRes)

        // Aksi column removed per user request
        holder.btnDetail.visibility = View.GONE
    }

    private fun bindFooter(holder: FooterViewHolder) {
        when (footerState) {
            FooterState.LOADING -> {
                holder.progressBar.visibility = View.VISIBLE
                holder.tvFooterText.visibility = View.VISIBLE
                holder.tvFooterText.text = "Memuat..."
            }
            FooterState.NO_MORE -> {
                holder.progressBar.visibility = View.GONE
                holder.tvFooterText.visibility = View.VISIBLE
                holder.tvFooterText.text = "— Semua data telah dimuat —"
            }
            FooterState.HIDDEN -> {}
        }
    }

    private fun showDetailDialog(context: android.content.Context, item: AbsensiStaffItem) {
        val status = item.status.lowercase()

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
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun appendData(newItems: List<AbsensiStaffItem>) {
        val oldSize = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(oldSize, newItems.size)
    }

    fun clearData() {
        items.clear()
        footerState = FooterState.HIDDEN
        notifyDataSetChanged()
    }

    class PresensiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNo: TextView = itemView.findViewById(R.id.tvNo)
        val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        val tvJenisSholat: TextView = itemView.findViewById(R.id.tvJenisSholat)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnDetail: MaterialButton = itemView.findViewById(R.id.btnDetail)
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressFooter)
        val tvFooterText: TextView = itemView.findViewById(R.id.tvFooterText)
    }
}
