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
    private val onEditClick: ((item: JadwalDhuhaKeahlian) -> Unit)? = null
) : RecyclerView.Adapter<DhuhaScheduleAdapter.ViewHolder>() {

    var isEditMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        val tvJurusan1: TextView = itemView.findViewById(R.id.tvJurusan1)
        val tvJurusan2: TextView = itemView.findViewById(R.id.tvJurusan2)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onEditClick?.invoke(rows[adapterPosition])
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
        android.util.Log.d("DhuhaAdapter", "Binding row $position: ${row.hari}")
        holder.tvDay.text = row.hari
        
        holder.itemView.setBackgroundColor(if (position % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA"))

        holder.tvJurusan1.text = row.keahlian1.joinToString(", ").ifEmpty { "-" }
        holder.tvJurusan2.text = row.keahlian2.joinToString(", ").ifEmpty { "-" }
        
        if (isEditMode) {
            holder.tvJurusan1.setTextColor(Color.parseColor("#2196F3"))
            holder.tvJurusan2.setTextColor(Color.parseColor("#2196F3"))
        } else {
            holder.tvJurusan1.setTextColor(Color.BLACK)
            holder.tvJurusan2.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = rows.size
    
    fun updateData(newRows: List<JadwalDhuhaKeahlian>) {
        this.rows = newRows
        notifyDataSetChanged()
    }
}
