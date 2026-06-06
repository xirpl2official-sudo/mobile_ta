package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.DeviceChangeRequestItem
import com.xirpl2.SASMobile.model.DeviceManagementItem

sealed class DeviceListItem {
    data class Device(val item: DeviceManagementItem) : DeviceListItem()
    data class ChangeRequest(val item: DeviceChangeRequestItem) : DeviceListItem()
}

class DeviceAdapter(
    private val onAction: (DeviceListItem.ChangeRequest, String) -> Unit
) : ListAdapter<DeviceListItem, DeviceAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DeviceListItem>() {
            override fun areItemsTheSame(oldItem: DeviceListItem, newItem: DeviceListItem): Boolean {
                return when {
                    oldItem is DeviceListItem.Device && newItem is DeviceListItem.Device ->
                        oldItem.item.id == newItem.item.id
                    oldItem is DeviceListItem.ChangeRequest && newItem is DeviceListItem.ChangeRequest ->
                        oldItem.item.id == newItem.item.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: DeviceListItem, newItem: DeviceListItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun updateData(newItems: List<DeviceListItem>) {
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DeviceListItem.Device -> {
                val d = item.item
                holder.tvDeviceName.text = d.device_name ?: "Unknown Device"
                holder.tvHardwareId.text = "ID: ${d.hardware_id}"
                holder.tvExtraInfo.text = "OS: ${d.os_version ?: "-"} | Aktif: ${d.last_auth_at ?: "-"}"
                holder.tvStatus.text = if (d.is_verified) "TERVERIFIKASI" else "PENDING"
                if (d.is_verified) {
                    setBadgeColors(holder, R.drawable.bg_status_approved, R.color.badge_approved_text)
                } else {
                    setBadgeColors(holder, R.drawable.bg_status_gray, R.color.badge_pending_text)
                }
                holder.layoutActions.visibility = View.GONE
            }
            is DeviceListItem.ChangeRequest -> {
                val r = item.item
                holder.tvDeviceName.text = "Permintaan Ganti Perangkat"
                val lama = r.hardware_id_lama?.take(8) ?: "???"
                val baru = r.hardware_id_baru?.take(8) ?: "???"
                holder.tvHardwareId.text = "NIS: ${r.user_id} | Dari: $lama... Ke: $baru..."
                holder.tvExtraInfo.text = "Alasan: ${r.reason ?: "-"}\nDiajukan: ${r.created_at}"
                holder.tvStatus.text = r.status.uppercase()

                when (r.status.lowercase()) {
                    "pending" -> {
                        setBadgeColors(holder, R.drawable.bg_status_gray, R.color.badge_pending_text)
                        holder.layoutActions.visibility = View.VISIBLE
                    }
                    "approved" -> {
                        setBadgeColors(holder, R.drawable.bg_status_approved, R.color.badge_approved_text)
                        holder.layoutActions.visibility = View.GONE
                    }
                    "rejected" -> {
                        setBadgeColors(holder, R.drawable.bg_status_alpha, R.color.badge_rejected_text)
                        holder.layoutActions.visibility = View.GONE
                    }
                }

                holder.btnApprove.setOnClickListener { onAction(item, "approve") }
                holder.btnReject.setOnClickListener { onAction(item, "reject") }
            }
        }
    }

    private fun setBadgeColors(holder: ViewHolder, bgRes: Int, textColorRes: Int) {
        val ctx = holder.tvStatus.context
        holder.tvStatus.setBackgroundResource(bgRes)
        holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, textColorRes))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvHardwareId: TextView = view.findViewById(R.id.tvHardwareId)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvExtraInfo: TextView = view.findViewById(R.id.tvExtraInfo)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnApprove: MaterialButton = view.findViewById(R.id.btnApprove)
        val btnReject: MaterialButton = view.findViewById(R.id.btnReject)
    }
}
