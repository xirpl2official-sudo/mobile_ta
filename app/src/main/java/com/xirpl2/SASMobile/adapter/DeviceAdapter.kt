package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.DeviceChangeRequestItem
import com.xirpl2.SASMobile.model.DeviceManagementItem

class DeviceAdapter(
    private var devices: List<Any>,
    private val onAction: (Any, String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    fun updateData(newItems: List<Any>) {
        devices = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_management, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = devices[position]
        
        if (item is DeviceManagementItem) {
            holder.tvDeviceName.text = item.device_name ?: "Unknown Device"
            holder.tvHardwareId.text = "ID: ${item.hardware_id}"
            holder.tvExtraInfo.text = "OS: ${item.os_version ?: "-"} | Aktif: ${item.last_auth_at ?: "-"}"
            holder.tvStatus.text = if (item.is_verified) "TERVERIFIKASI" else "PENDING"
            holder.tvStatus.setBackgroundResource(if (item.is_verified) R.drawable.bg_status_approved else R.drawable.bg_status_gray)
            holder.layoutActions.visibility = View.GONE
        } else if (item is DeviceChangeRequestItem) {
            holder.tvDeviceName.text = "Permintaan Ganti Perangkat"
            holder.tvHardwareId.text = "NIS: ${item.user_id} | Dari: ${item.hardware_id_lama.take(8)}... Ke: ${item.hardware_id_baru.take(8)}..."
            holder.tvExtraInfo.text = "Alasan: ${item.reason ?: "-"}\nDiajukan: ${item.created_at}"
            holder.tvStatus.text = item.status.uppercase()
            
            when (item.status.lowercase()) {
                "pending" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_gray)
                    holder.layoutActions.visibility = View.VISIBLE
                }
                "approved" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved)
                    holder.layoutActions.visibility = View.GONE
                }
                "rejected" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_alpha)
                    holder.layoutActions.visibility = View.GONE
                }
            }

            holder.btnApprove.setOnClickListener { onAction(item, "approve") }
            holder.btnReject.setOnClickListener { onAction(item, "reject") }
        }
    }

    override fun getItemCount(): Int = devices.size

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
