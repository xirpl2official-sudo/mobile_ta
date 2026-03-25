package com.xirpl2.SASMobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.DhuhaJurusanData

/**
 * Adapter untuk RecyclerView list Jurusan dengan jadwal Dhuha
 */
class JurusanAdapter(
    private val listJurusan: List<DhuhaJurusanData>
) : RecyclerView.Adapter<JurusanAdapter.JurusanViewHolder>() {

    inner class JurusanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardJurusan: CardView = itemView.findViewById(R.id.cardJurusan)
        val tvNamaJurusan: TextView = itemView.findViewById(R.id.tvNamaJurusan)
        val tvLabelHariIni: TextView = itemView.findViewById(R.id.tvLabelHariIni)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JurusanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jurusan, parent, false)
        return JurusanViewHolder(view)
    }

    override fun onBindViewHolder(holder: JurusanViewHolder, position: Int) {
        val jurusanData = listJurusan[position]
        
        // Set nama jurusan
        holder.tvNamaJurusan.text = jurusanData.jurusan
        
        // Set jadwal info
        if (jurusanData.jadwal.isNotEmpty()) {
            val jadwal = jurusanData.jadwal[0] // Take first schedule
            val waktuMulai = jadwal.jam_mulai.substring(0, 5) // HH:MM format
            val waktuSelesai = jadwal.jam_selesai.substring(0, 5)
            holder.tvLabelHariIni.text = "Dhuha ${waktuMulai}-${waktuSelesai}"
        } else {
            holder.tvLabelHariIni.text = "Dhuha Hari Ini"
        }

        // Set warna background card berdasarkan jurusan
        val warnaJurusan = when (jurusanData.jurusan.uppercase()) {
            "BROADCASTING" -> "#E14648"
            "RPL" -> "#F78B20"
            "ANIMASI" -> "#DA3F93"
            "TKJ" -> "#FFCB0B"
            "TAV" -> "#4FA898"
            "MEKATRONIKA" -> "#57A77C"
            "TEI" -> "#059452"
            "DKV" -> "#1087CC"
            else -> "#2196F3" // Default biru jika tidak cocok
        }
        holder.cardJurusan.setCardBackgroundColor(Color.parseColor(warnaJurusan))
    }

    override fun getItemCount(): Int = listJurusan.size
}
