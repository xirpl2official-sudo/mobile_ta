package com.xirpl2.SASMobile

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.JadwalSholatData

data class DhuhaDayRow(
    val day: String,
    var slot1: JadwalSholatData? = null,
    var slot2: JadwalSholatData? = null
)

class DhuhaScheduleAdapter(
    private var rows: List<DhuhaDayRow>,
    private val onModified: (item: JadwalSholatData) -> Unit,
    private val onEditClick: ((id: Int, jenis: String) -> Unit)? = null
) : RecyclerView.Adapter<DhuhaScheduleAdapter.ViewHolder>() {

    var isEditMode = false
        set(value) {
            field = value
            val size = rows.size
            if (!value) {
                selectedItem = null
            }
            notifyItemRangeChanged(0, size)
        }

    // Pair of rowIndex and slotIndex (1 or 2)
    private var selectedItem: Pair<Int, Int>? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvJurusan1: TextView = itemView.findViewById(R.id.tvJurusan1)
        val tvJurusan2: TextView = itemView.findViewById(R.id.tvJurusan2)

        init {
            tvJurusan1.setOnClickListener { handleSlotClick(adapterPosition, 1) }
            tvJurusan2.setOnClickListener { handleSlotClick(adapterPosition, 2) }
        }
    }

    private fun handleSlotClick(rowIndex: Int, slotIndex: Int) {
        if (rowIndex == RecyclerView.NO_POSITION) return
        val row = rows[rowIndex]
        val clickedData = if (slotIndex == 1) row.slot1 else row.slot2

        if (!isEditMode) {
            // Original behavior: open dialog
            if (clickedData != null) {
                onEditClick?.invoke(clickedData.id, "Dhuha")
            }
            return
        }

        // In edit mode: first selection
        if (selectedItem == null) {
            if (clickedData == null) return // Can't start a swap from an empty slot
            selectedItem = Pair(rowIndex, slotIndex)
            notifyItemChanged(rowIndex)
        } else {
            // Second selection: could be another item or an empty slot
            val firstSelection = selectedItem!!
            if (firstSelection.first == rowIndex && firstSelection.second == slotIndex) {
                // Deselect if same item clicked
                selectedItem = null
                notifyItemChanged(rowIndex)
                return
            }

            // Execute Swap or Move Logic
            val row1 = rows[firstSelection.first]
            val data1 = if (firstSelection.second == 1) row1.slot1 else row1.slot2
            val row2 = rows[rowIndex]
            val data2 = if (slotIndex == 1) row2.slot1 else row2.slot2

            if (data1 != null) {
                // UPDATE POSITIONS AND HARI PROPERTY
                // Update Slot in Row 1
                if (firstSelection.second == 1) row1.slot1 = data2 else row1.slot2 = data2
                // Update Slot in Row 2
                if (slotIndex == 1) row2.slot1 = data1 else row2.slot2 = data1

                // Important: Update the 'hari' property for API persistence
                data1.hari = row2.day
                onModified(data1)
                
                if (data2 != null) {
                    data2.hari = row1.day
                    onModified(data2)
                }

                selectedItem = null
                notifyItemChanged(firstSelection.first)
                notifyItemChanged(rowIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dhuha_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.tvDay.text = row.day
        
        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA"))

        bindSlot(holder.tvJurusan1, row.slot1, position, 1)
        bindSlot(holder.tvJurusan2, row.slot2, position, 2)
    }

    private fun bindSlot(tv: TextView, data: JadwalSholatData?, rowIndex: Int, slotIndex: Int) {
        tv.text = data?.jurusan ?: "-"
        if (data == null) {
            tv.setTextColor(Color.parseColor("#999999"))
        } else {
            val isSelected = selectedItem?.first == rowIndex && selectedItem?.second == slotIndex
            if (isSelected) {
                // "changes the text color ... to Blue"
                tv.setTextColor(Color.parseColor("#2196F3")) // Selected (Blue)
            } else {
                // "text color ... reverted back to Black"
                tv.setTextColor(Color.parseColor("#000000")) // Default (Black)
            }
        }
    }

    override fun getItemCount(): Int = rows.size
    
    fun updateData(newRows: List<DhuhaDayRow>) {
        this.rows = newRows
        selectedItem = null
        notifyDataSetChanged()
    }
}
