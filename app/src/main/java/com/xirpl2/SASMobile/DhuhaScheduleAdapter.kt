package com.xirpl2.SASMobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian

class DhuhaScheduleAdapter(
    private var rows: List<JadwalDhuhaKeahlian>,
    private val onSwap: ((row1: Int, col1: Int, row2: Int, col2: Int) -> Unit)? = null
) : RecyclerView.Adapter<DhuhaScheduleAdapter.ViewHolder>() {

    var isEditMode = false
        set(value) {
            field = value
            selectedRow = -1
            selectedCol = -1
            notifyDataSetChanged()
        }

    private var selectedRow = -1
    private var selectedCol = -1

    private fun handleItemClick(row: Int, col: Int) {
        if (!isEditMode) return

        if (selectedRow == -1 && selectedCol == -1) {
            selectedRow = row
            selectedCol = col
            notifyDataSetChanged()
        } else {
            onSwap?.invoke(selectedRow, selectedCol, row, col)
            selectedRow = -1
            selectedCol = -1
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvJurusan1: TextView = itemView.findViewById(R.id.tvJurusan1)
        val tvJurusan2: TextView = itemView.findViewById(R.id.tvJurusan2)

        init {
            tvJurusan1.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    handleItemClick(adapterPosition, 1)
                }
            }
            tvJurusan2.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    handleItemClick(adapterPosition, 2)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dhuha_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.tvDay.text = row.hari

        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA"))

        holder.tvJurusan1.text = row.jurusan1?.nama_jurusan ?: "-"
        holder.tvJurusan2.text = row.jurusan2?.nama_jurusan ?: "-"

        if (isEditMode) {
            val isSelected1 = (position == selectedRow && selectedCol == 1)
            val isSelected2 = (position == selectedRow && selectedCol == 2)

            holder.tvJurusan1.setTextColor(if (isSelected1) Color.WHITE else Color.parseColor("#1E3A8A"))
            if (isSelected1) {
                holder.tvJurusan1.setBackgroundResource(R.drawable.bg_selected_jurusan)
            } else {
                holder.tvJurusan1.setBackgroundResource(0)
            }

            holder.tvJurusan2.setTextColor(if (isSelected2) Color.WHITE else Color.parseColor("#1E3A8A"))
            if (isSelected2) {
                holder.tvJurusan2.setBackgroundResource(R.drawable.bg_selected_jurusan)
            } else {
                holder.tvJurusan2.setBackgroundResource(0)
            }
        } else {
            holder.tvJurusan1.setTextColor(Color.BLACK)
            holder.tvJurusan1.setBackgroundResource(0)
            holder.tvJurusan2.setTextColor(Color.BLACK)
            holder.tvJurusan2.setBackgroundResource(0)
        }
    }

    override fun getItemCount(): Int = rows.size

    fun updateData(newRows: List<JadwalDhuhaKeahlian>) {
        this.rows = newRows
        notifyDataSetChanged()
    }
}
