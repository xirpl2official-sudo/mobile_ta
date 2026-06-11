package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.StatusSholat

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class JadwalSholatAdapter(
    private val onDeleteClick: ((JadwalSholat) -> Unit)? = null
) : ListAdapter<JadwalSholat, JadwalSholatAdapter.JadwalViewHolder>(JadwalDiffCallback()) {

    inner class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardJadwalItem: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardJadwalItem)
        val tvNamaSholat: TextView = itemView.findViewById(R.id.tvNamaSholat)
        val tvJamSholat: TextView = itemView.findViewById(R.id.tvJamSholat)
        val textStatusSholat: TextView = itemView.findViewById(R.id.textStatusSholat)
        val btnDelete: android.widget.ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_sholat, parent, false)
        return JadwalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val jadwal = getItem(position)
        val ctx = holder.itemView.context

        holder.tvNamaSholat.text = jadwal.namaSholat
        holder.tvJamSholat.text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"

        when (jadwal.status) {
            StatusSholat.SELESAI -> {
                holder.textStatusSholat.text = ctx.getString(R.string.StatusSelesai)
                holder.textStatusSholat.setTextColor(ContextCompat.getColor(ctx, R.color.slate_500))
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_selesai)
            }
            StatusSholat.SEDANG_BERLANGSUNG -> {
                holder.textStatusSholat.text = ctx.getString(R.string.StatusBerlangsung)
                holder.textStatusSholat.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_berlangsung)
            }
            StatusSholat.AKAN_DATANG -> {
                holder.textStatusSholat.text = ctx.getString(R.string.StatusAkanDatang)
                holder.textStatusSholat.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_akandatang)
            }
        }

        if (onDeleteClick != null) {
            holder.btnDelete.visibility = android.view.View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick.invoke(jadwal) }
        } else {
            holder.btnDelete.visibility = android.view.View.GONE
        }
    }
}

class JadwalDiffCallback : DiffUtil.ItemCallback<JadwalSholat>() {
    override fun areItemsTheSame(oldItem: JadwalSholat, newItem: JadwalSholat): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: JadwalSholat, newItem: JadwalSholat): Boolean {
        return oldItem == newItem
    }
}
