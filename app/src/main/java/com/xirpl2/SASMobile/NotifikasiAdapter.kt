package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.databinding.ItemNotifikasiBinding
import com.xirpl2.SASMobile.model.Notifikasi

class NotifikasiAdapter(private val notificationList: MutableList<Notifikasi>) :
    RecyclerView.Adapter<NotifikasiAdapter.NotifikasiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifikasiViewHolder {
        val binding =
            ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifikasiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifikasiViewHolder, position: Int) {
        val notification = notificationList[position]
        holder.bind(notification)
        holder.binding.btnDelete.setOnClickListener {
            notificationList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notificationList.size)
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    class NotifikasiViewHolder(val binding: ItemNotifikasiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notifikasi: Notifikasi) {
            binding.tvTitle.text = notifikasi.title
            binding.tvMessage.text = notifikasi.message
            binding.tvTime.text = notifikasi.time
        }
    }
}
