package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewUnreadDot: View = view.findViewById(R.id.viewUnreadDot)
        val tvNotifType: TextView = view.findViewById(R.id.tvNotifType)
        val tvNotifTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvNotifMessage: TextView = view.findViewById(R.id.tvNotifMessage)
        val tvNotifTime: TextView = view.findViewById(R.id.tvNotifTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.viewUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
        holder.tvNotifTitle.text = item.title
        holder.tvNotifMessage.text = item.message
        holder.tvNotifTime.text = formatRelativeTime(item.createdAt)

        val context = holder.itemView.context
        holder.tvNotifType.apply {
            text = item.type.replaceFirstChar { it.uppercase() }
            when (item.type.lowercase()) {
                "warning" -> {
                    setBackgroundResource(R.drawable.bg_badge_berlangsung)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "urgent", "failure" -> {
                    setBackgroundResource(R.drawable.bg_badge_berlangsung)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "info" -> {
                    setBackgroundResource(R.drawable.bg_badge_akandatang)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "success" -> {
                    background = ContextCompat.getDrawable(context, R.drawable.bg_status_hadir)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                "reminder" -> {
                    setBackgroundResource(R.drawable.bg_badge_selesai)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                else -> {
                    setBackgroundResource(R.drawable.bg_badge_selesai)
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    private fun formatRelativeTime(isoDate: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatNoTz = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val date = format.parse(isoDate) ?: formatNoTz.parse(isoDate)
            if (date == null) return isoDate.take(16).replace("T", " ")

            val now = Date()
            val diff = now.time - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {
                minutes < 1 -> "Baru saja"
                minutes < 60 -> "$minutes mnt lalu"
                hours < 24 -> "$hours jam lalu"
                days < 7 -> "$days hari lalu"
                else -> {
                    val outFormat = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
                    outFormat.format(date)
                }
            }
        } catch (e: Exception) {
            isoDate.take(16).replace("T", " ")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.isRead == newItem.isRead && oldItem.title == newItem.title
        }
    }
}
