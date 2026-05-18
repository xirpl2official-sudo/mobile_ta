package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.JadwalSholat
import com.xirpl2.SASMobile.model.StatusSholat

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class JadwalSholatAdapter : ListAdapter<JadwalSholat, JadwalSholatAdapter.JadwalViewHolder>(JadwalDiffCallback()) {

    inner class JadwalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardJadwalItem: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardJadwalItem)
        val tvNamaSholat: TextView = itemView.findViewById(R.id.tvNamaSholat)
        val tvJamSholat: TextView = itemView.findViewById(R.id.tvJamSholat)
        val textStatusSholat: TextView = itemView.findViewById(R.id.textStatusSholat)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_sholat, parent, false)
        return JadwalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val jadwal = getItem(position)

        holder.tvNamaSholat.text = jadwal.namaSholat
        holder.tvJamSholat.text = "${jadwal.jamMulai} - ${jadwal.jamSelesai}"

        
        when (jadwal.status) {
            StatusSholat.SELESAI -> {
                
                holder.cardJadwalItem.setCardBackgroundColor(Color.TRANSPARENT)
                holder.cardJadwalItem.setBackgroundResource(R.drawable.bg_status_gray)

                holder.textStatusSholat.text = "Selesai"
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_selesai)
                holder.textStatusSholat.setTextColor(Color.WHITE)
            }
            StatusSholat.SEDANG_BERLANGSUNG -> {
                
                holder.cardJadwalItem.setCardBackgroundColor(Color.TRANSPARENT)
                
                holder.cardJadwalItem.setBackgroundResource(R.drawable.bg_status_berlangsung)

                holder.textStatusSholat.text = "Berlangsung"
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_berlangsung)
                holder.textStatusSholat.setTextColor(Color.WHITE)
            }
            StatusSholat.AKAN_DATANG -> {
                
                holder.cardJadwalItem.setCardBackgroundColor(Color.TRANSPARENT)
                
                holder.cardJadwalItem.setBackgroundResource(R.drawable.bg_status_akandatang)

                
                holder.textStatusSholat.text = "Akan Datang"
                holder.textStatusSholat.setBackgroundResource(R.drawable.bg_badge_akandatang)
                holder.textStatusSholat.setTextColor(Color.WHITE)
            }
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