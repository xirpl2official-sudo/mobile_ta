package com.xirpl2.SASMobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.xirpl2.SASMobile.model.DhuhaJurusanData

class JurusanAdapter(
    private var listJurusan: List<DhuhaJurusanData> = emptyList()
) : RecyclerView.Adapter<JurusanAdapter.JurusanViewHolder>() {

    fun updateData(newList: List<DhuhaJurusanData>) {
        listJurusan = newList
        notifyDataSetChanged()
    }

    inner class JurusanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardJurusan: MaterialCardView = itemView.findViewById(R.id.cardJurusan)
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

        holder.tvNamaJurusan.text = jurusanData.jurusan

        if (jurusanData.jadwal.isNotEmpty()) {
            val jadwal = jurusanData.jadwal[0]
            val rawMulai = jadwal.jam_mulai
            val rawSelesai = jadwal.jam_selesai

            val waktuMulai = if (rawMulai.length >= 5) rawMulai.substring(0, 5) else rawMulai
            val waktuSelesai = if (rawSelesai.length >= 5) rawSelesai.substring(0, 5) else rawSelesai

            if (waktuMulai.isNotEmpty() && waktuSelesai.isNotEmpty()) {
                holder.tvLabelHariIni.text = "Duha ${waktuMulai}-${waktuSelesai} WIB"
            } else {
                holder.tvLabelHariIni.text = "Duha Hari Ini"
            }
        } else {
            holder.tvLabelHariIni.text = "Duha Hari Ini"
        }

        val warnaJurusan = Color.parseColor(JurusanHelper.getColorForJurusan(jurusanData.jurusan))
        holder.cardJurusan.setCardBackgroundColor(warnaJurusan)
    }

    override fun getItemCount(): Int = listJurusan.size
}
