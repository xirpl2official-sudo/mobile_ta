package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.HalanganPendingItem

class PendingHalanganAdapter(
    private val onValidate: (HalanganPendingItem) -> Unit
) : RecyclerView.Adapter<PendingHalanganAdapter.ViewHolder>() {

    private val items = mutableListOf<HalanganPendingItem>()

    fun submitList(newItems: List<HalanganPendingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_halangan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNis.text = item.nis
        holder.tvNama.text = item.namaSiswa
        holder.tvKelas.text = item.kelas
        holder.tvTanggal.text = formatTanggal(item.tanggal)
        holder.btnValidasi.setOnClickListener { onValidate(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNis: TextView = itemView.findViewById(R.id.tvPendingNis)
        val tvNama: TextView = itemView.findViewById(R.id.tvPendingNama)
        val tvKelas: TextView = itemView.findViewById(R.id.tvPendingKelas)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvPendingTanggal)
        val btnValidasi: MaterialButton = itemView.findViewById(R.id.btnPendingValidasi)
    }

    private fun formatTanggal(tanggal: String): String {
        return try {
            val parts = tanggal.split("-")
            if (parts.size == 3) {
                val monthNames = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
                "${parts[2].toInt()} ${monthNames.getOrElse(parts[1].toInt() - 1) { parts[1] }}"
            } else tanggal
        } catch (e: Exception) { tanggal }
    }
}
