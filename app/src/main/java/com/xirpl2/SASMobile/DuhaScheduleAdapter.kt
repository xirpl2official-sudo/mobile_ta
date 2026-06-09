package com.xirpl2.SASMobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.JadwalDuhaKeahlian

private object DuhaDiffCallback : DiffUtil.ItemCallback<JadwalDuhaKeahlian>() {
    override fun areItemsTheSame(oldItem: JadwalDuhaKeahlian, newItem: JadwalDuhaKeahlian): Boolean =
        oldItem.hari == newItem.hari

    override fun areContentsTheSame(oldItem: JadwalDuhaKeahlian, newItem: JadwalDuhaKeahlian): Boolean =
        oldItem == newItem
}

class DuhaScheduleAdapter(
    private val onSwap: ((row1: Int, col1: Int, row2: Int, col2: Int) -> Unit)? = null
) : ListAdapter<JadwalDuhaKeahlian, DuhaScheduleAdapter.ViewHolder>(DuhaDiffCallback) {

    var isEditMode = false
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                val oldSelected = selectedPosition()
                selectedRow = -1
                selectedCol = -1
                if (oldSelected != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldSelected)
                }
                notifyItemRangeChanged(0, itemCount)
            }
        }

    private var selectedRow = -1
    private var selectedCol = -1

    private fun selectedPosition(): Int = if (selectedRow in 0 until itemCount) selectedRow else RecyclerView.NO_POSITION

    private fun handleItemClick(row: Int, col: Int) {
        if (!isEditMode) return

        val oldSelected = selectedPosition()

        if (selectedRow == -1 && selectedCol == -1) {
            selectedRow = row
            selectedCol = col
            notifyItemChanged(row)
        } else {
            onSwap?.invoke(selectedRow, selectedCol, row, col)
            selectedRow = -1
            selectedCol = -1
            if (oldSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldSelected)
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvJurusan1: TextView = itemView.findViewById(R.id.tvJurusan1)
        val tvJurusan2: TextView = itemView.findViewById(R.id.tvJurusan2)

        init {
            tvJurusan1.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    handleItemClick(bindingAdapterPosition, 1)
                }
            }
            tvJurusan2.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    handleItemClick(bindingAdapterPosition, 2)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_duha_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        holder.tvDay.text = row.hari

        val bgRes = if (position % 2 == 0) R.color.surface else R.color.slate_100
        holder.itemView.setBackgroundColor(holder.itemView.context.getColor(bgRes))

        holder.tvJurusan1.text = row.jurusan1?.nama_jurusan ?: "-"
        holder.tvJurusan2.text = row.jurusan2?.nama_jurusan ?: "-"

        if (isEditMode) {
            val isSelected1 = (position == selectedRow && selectedCol == 1)
            val isSelected2 = (position == selectedRow && selectedCol == 2)

            val primaryTextColor = holder.itemView.context.getColor(R.color.blue_700)
            holder.tvJurusan1.setTextColor(if (isSelected1) Color.WHITE else primaryTextColor)
            if (isSelected1) {
                holder.tvJurusan1.setBackgroundResource(R.drawable.bg_selected_jurusan)
            } else {
                holder.tvJurusan1.setBackgroundResource(0)
            }

            holder.tvJurusan2.setTextColor(if (isSelected2) Color.WHITE else primaryTextColor)
            if (isSelected2) {
                holder.tvJurusan2.setBackgroundResource(R.drawable.bg_selected_jurusan)
            } else {
                holder.tvJurusan2.setBackgroundResource(0)
            }
        } else {
            val textColor = holder.itemView.context.getColor(R.color.on_background)
            holder.tvJurusan1.setTextColor(textColor)
            holder.tvJurusan1.setBackgroundResource(0)
            holder.tvJurusan2.setTextColor(textColor)
            holder.tvJurusan2.setBackgroundResource(0)
        }
    }

}
