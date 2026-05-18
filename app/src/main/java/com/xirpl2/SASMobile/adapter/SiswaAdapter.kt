package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.CheckBox
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.SiswaItem

class SiswaAdapter(
    private val onEditClick: (SiswaItem) -> Unit,
    private val onDeleteClick: (SiswaItem) -> Unit,
    private val onDetailClick: (SiswaItem) -> Unit,
    private val isReadOnly: Boolean = false
) : ListAdapter<SiswaItem, RecyclerView.ViewHolder>(SiswaDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private var isLoadingMore = false

    fun setLoadingMore(loading: Boolean) {
        val wasLoading = isLoadingMore
        isLoadingMore = loading
        
        if (loading && !wasLoading) {
            notifyItemInserted(itemCount)
        } else if (!loading && wasLoading) {
            notifyItemRemoved(itemCount)
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoadingMore) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoadingMore && position == itemCount - 1) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading, parent, false)
            LoadingViewHolder(view)
        } else {
            val layoutId = if (isReadOnly) R.layout.item_siswa_unregistered_row else R.layout.item_siswa
            val view = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
            SiswaViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SiswaViewHolder -> {
                val siswa = getItem(position)
                holder.bind(siswa, onEditClick, onDeleteClick, onDetailClick, isReadOnly)
            }
            is LoadingViewHolder -> {
                
            }
        }
    }

    class SiswaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvJenisKelamin: TextView? = itemView.findViewById(R.id.tvJenisKelamin)
        private val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        private val tvJurusan: TextView? = itemView.findViewById(R.id.tvJurusan)
        private val tvWaliKelas: TextView? = itemView.findViewById(R.id.tvWaliKelas)
        private val btnDetailSiswa: MaterialButton? = itemView.findViewById(R.id.btnDetailSiswa)
        private val btnDetailRow: MaterialButton? = itemView.findViewById(R.id.btnDetail)
        private val btnEdit: ImageView? = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageView? = itemView.findViewById(R.id.btnDelete)
        private val ivDeviceStatus: ImageView? = itemView.findViewById(R.id.ivDeviceStatus)
        private val cbRow: CheckBox? = itemView.findViewById<CheckBox>(R.id.cbRow)

        fun bind(
            siswa: SiswaItem,
            onEditClick: (SiswaItem) -> Unit,
            onDeleteClick: (SiswaItem) -> Unit,
            onDetailClick: (SiswaItem) -> Unit,
            isReadOnly: Boolean
        ) {
            tvNis.text = siswa.nis
            
            // FIX-012: Visual distinction for old NIS format
            if (siswa.nis.contains("/") || siswa.nis.contains(".")) {
                tvNis.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                tvNis.setTypeface(null, android.graphics.Typeface.ITALIC)
            } else {
                tvNis.setTextColor(android.graphics.Color.parseColor("#1565C0"))
                tvNis.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            
            tvNama.text = siswa.nama_siswa
            
            // Device Status Indicator (List Item Mode)
            ivDeviceStatus?.let { iv ->
                if (siswa.deviceStatus != null || siswa.hardwareId != null) {
                    iv.visibility = View.VISIBLE
                    val color = when {
                        siswa.deviceStatus == "pending" -> "#FACC15"
                        siswa.hardwareId != null -> "#22C55E"
                        else -> "#EF4444"
                    }
                    iv.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
                } else {
                    iv.visibility = View.GONE
                }
            }

            tvJenisKelamin?.text = if (siswa.jenis_kelamin.isNotEmpty()) {
                siswa.jenis_kelamin.take(1).uppercase()
            } else {
                "-"
            }
            tvKelas.text = siswa.kelas
            tvJurusan?.text = siswa.jurusan
            tvWaliKelas?.text = "-" // Placeholder for Wali Kelas from spec

            val detailButton = btnDetailRow ?: btnDetailSiswa
            detailButton?.setOnClickListener { onDetailClick(siswa) }

            if (isReadOnly) {
                btnEdit?.visibility = View.GONE
                btnDelete?.visibility = View.GONE
            } else {
                btnEdit?.visibility = View.VISIBLE
                btnDelete?.visibility = View.VISIBLE
                btnEdit?.setOnClickListener { onEditClick(siswa) }
                btnDelete?.setOnClickListener { onDeleteClick(siswa) }
            }
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

class SiswaDiffCallback : DiffUtil.ItemCallback<SiswaItem>() {
    override fun areItemsTheSame(oldItem: SiswaItem, newItem: SiswaItem): Boolean {
        return oldItem.nis == newItem.nis
    }

    override fun areContentsTheSame(oldItem: SiswaItem, newItem: SiswaItem): Boolean {
        return oldItem == newItem
    }
}
