package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.databinding.ItemNotifikasiBinding
import com.xirpl2.SASMobile.model.Notifikasi

class NotifikasiAdapter(
    private val onItemClick: ((Notifikasi) -> Unit)? = null,
    private val onDeleteClick: ((Notifikasi) -> Unit)? = null
) : ListAdapter<Notifikasi, NotifikasiAdapter.NotifikasiViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifikasiViewHolder {
        val binding =
            ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifikasiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifikasiViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(notification)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(notification)
        }

        // Update UI based on isRead status
        holder.itemView.alpha = if (notification.isRead) 0.6f else 1.0f
    }

    class NotifikasiViewHolder(val binding: ItemNotifikasiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notifikasi: Notifikasi) {
            binding.tvTitle.text = notifikasi.title
            binding.tvMessage.text = notifikasi.message
            binding.tvTime.text = notifikasi.time
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Notifikasi>() {
            override fun areItemsTheSame(oldItem: Notifikasi, newItem: Notifikasi): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Notifikasi, newItem: Notifikasi): Boolean {
                return oldItem == newItem
            }
        }
    }
}
