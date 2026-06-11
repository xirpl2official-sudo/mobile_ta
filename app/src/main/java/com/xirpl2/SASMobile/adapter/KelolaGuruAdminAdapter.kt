package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.GuruItem

class KelolaGuruAdminAdapter(
    private val onEditClick: (GuruItem) -> Unit,
    private val onLepasWaliKelasClick: (GuruItem) -> Unit,
    private val onDeleteClick: (GuruItem) -> Unit,
    private val onPageChanged: ((totalItems: Int, totalPages: Int, currentPage: Int) -> Unit)? = null
) : ListAdapter<GuruItem, KelolaGuruAdminAdapter.GuruViewHolder>(GuruDiffCallback()) {

    private var fullList: List<GuruItem> = emptyList()
    private var currentPage = 0
    private val pageSize = 7

    fun setFullList(list: List<GuruItem>) {
        fullList = list
        currentPage = 0
        refreshPage()
    }

    fun getTotalPages(): Int = if (fullList.isEmpty()) 1 else Math.ceil(fullList.size.toDouble() / pageSize).toInt()

    fun getCurrentPage(): Int = currentPage + 1

    fun getTotalItems(): Int = fullList.size

    fun goToPage(page: Int) {
        val zeroBased = page - 1
        if (zeroBased in 0 until getTotalPages()) {
            currentPage = zeroBased
            refreshPage()
        }
    }

    fun nextPage() {
        if (currentPage + 1 < getTotalPages()) {
            currentPage++
            refreshPage()
        }
    }

    fun prevPage() {
        if (currentPage > 0) {
            currentPage--
            refreshPage()
        }
    }

    private fun refreshPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullList.size)
        val pageItems = if (start < fullList.size) fullList.subList(start, end) else emptyList()
        submitList(pageItems.toList())
        onPageChanged?.invoke(fullList.size, getTotalPages(), getCurrentPage())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuruViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guru_admin, parent, false)
        return GuruViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuruViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GuruViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvNip: TextView = itemView.findViewById(R.id.tvNip)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvWaliKelas: TextView = itemView.findViewById(R.id.tvWaliKelas)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        private val btnLepasWaliKelas: ImageView = itemView.findViewById(R.id.btnLepasWaliKelas)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(guru: GuruItem) {
            tvNama.text = guru.nama
            tvNip.text = if (guru.nip.isNullOrEmpty()) "-" else guru.nip
            tvEmail.text = if (guru.email.isNullOrEmpty()) "-" else guru.email

            if (guru.id_kelas_wali != null) {
                tvWaliKelas.text = guru.label_kelas
                tvWaliKelas.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_jurusan)
                tvWaliKelas.setTextColor(ContextCompat.getColor(itemView.context, R.color.blue_theme))
                tvWaliKelas.visibility = View.VISIBLE
                btnLepasWaliKelas.visibility = View.VISIBLE
            } else {
                tvWaliKelas.text = "-"
                tvWaliKelas.background = null
                tvWaliKelas.setTextColor(ContextCompat.getColor(itemView.context, R.color.slate_700))
                tvWaliKelas.visibility = View.VISIBLE
                btnLepasWaliKelas.visibility = View.GONE
            }

            btnEdit.setOnClickListener { onEditClick(guru) }
            btnLepasWaliKelas.setOnClickListener { onLepasWaliKelasClick(guru) }
            btnDelete.setOnClickListener { onDeleteClick(guru) }
        }
    }

    class GuruDiffCallback : DiffUtil.ItemCallback<GuruItem>() {
        override fun areItemsTheSame(oldItem: GuruItem, newItem: GuruItem): Boolean {
            return oldItem.id_staff == newItem.id_staff
        }

        override fun areContentsTheSame(oldItem: GuruItem, newItem: GuruItem): Boolean {
            return oldItem == newItem
        }
    }
}
