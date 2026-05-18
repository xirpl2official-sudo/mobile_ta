package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.GuruItem

class KelolaGuruAdminAdapter(
    private var guruList: List<GuruItem>,
    private val onEditClick: (GuruItem) -> Unit,
    private val onLepasWaliKelasClick: (GuruItem) -> Unit,
    private val onDeleteClick: (GuruItem) -> Unit
) : RecyclerView.Adapter<KelolaGuruAdminAdapter.GuruViewHolder>() {

    fun updateData(newList: List<GuruItem>) {
        guruList = newList
        notifyDataSetDataSetChanged()
    }

    private fun notifyDataSetDataSetChanged() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuruViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guru_admin, parent, false)
        return GuruViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuruViewHolder, position: Int) {
        val guru = guruList[position]
        holder.bind(guru)
    }

    override fun getItemCount(): Int = guruList.size

    inner class GuruViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvNip: TextView = itemView.findViewById(R.id.tvNip)
        private val tvWaliKelas: TextView = itemView.findViewById(R.id.tvWaliKelas)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        private val btnLepasWaliKelas: ImageView = itemView.findViewById(R.id.btnLepasWaliKelas)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(guru: GuruItem) {
            tvNama.text = guru.nama
            tvEmail.text = if (guru.email.isNullOrEmpty()) "-" else guru.email
            tvNip.text = if (guru.nip.isNullOrEmpty()) "-" else guru.nip
            
            if (guru.id_kelas_wali != null) {
                tvWaliKelas.text = guru.label_kelas
                tvWaliKelas.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_jurusan)
                tvWaliKelas.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue_theme))
                btnLepasWaliKelas.visibility = View.VISIBLE
            } else {
                tvWaliKelas.text = "-"
                tvWaliKelas.background = null
                tvWaliKelas.setTextColor(ContextCompat.getColor(itemView.context, R.color.slate_700))
                btnLepasWaliKelas.visibility = View.GONE
            }

            btnEdit.setOnClickListener { onEditClick(guru) }
            btnLepasWaliKelas.setOnClickListener { onLepasWaliKelasClick(guru) }
            btnDelete.setOnClickListener { onDeleteClick(guru) }
        }
    }
}