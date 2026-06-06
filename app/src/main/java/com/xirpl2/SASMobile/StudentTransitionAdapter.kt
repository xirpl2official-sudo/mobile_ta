package com.xirpl2.SASMobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.model.StudentTransition

class StudentTransitionAdapter : RecyclerView.Adapter<StudentTransitionAdapter.ViewHolder>() {

    private val items = mutableListOf<StudentTransition>()

    fun submitList(list: List<StudentTransition>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getStudentNisList(): List<String> = items.map { it.nis }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_transition, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNamaSiswa: TextView = itemView.findViewById(R.id.tvNamaSiswa)
        private val tvFromKelas: TextView = itemView.findViewById(R.id.tvFromKelas)
        private val tvToKelas: TextView = itemView.findViewById(R.id.tvToKelas)

        fun bind(item: StudentTransition) {
            tvNamaSiswa.text = "${item.nis} - ${item.nama_siswa}"
            tvFromKelas.text = item.kelas_sekarang
            tvToKelas.text = item.kelas_tujuan
        }
    }
}
