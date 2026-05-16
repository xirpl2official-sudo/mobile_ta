package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.KelasManagementItem
import com.xirpl2.SASMobile.model.SiswaItem

class KelasManageAdapter(
    private var kelasList: List<KelasManagementItem>,
    private val onUbahWaliClick: (KelasManagementItem) -> Unit,
    private val onExpandClick: (KelasManagementItem, (List<SiswaItem>) -> Unit) -> Unit,
    private val onStudentDetailClick: (SiswaItem) -> Unit
) : RecyclerView.Adapter<KelasManageAdapter.KelasViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()
    private val classStudentsMap = mutableMapOf<Int, List<SiswaItem>>()

    inner class KelasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaKelas: TextView = view.findViewById(R.id.tvNamaKelas)
        val badgeSiswaCount: TextView = view.findViewById(R.id.badgeSiswaCount)
        val tvWaliKelas: TextView = view.findViewById(R.id.tvWaliKelas)
        val btnUbahWali: MaterialButton = view.findViewById(R.id.btnUbahWali)
        val ivExpand: ImageView = view.findViewById(R.id.ivExpand)
        val layoutExpandable: View = view.findViewById(R.id.layoutExpandable)
        val tvDaftarSiswaTitle: TextView = view.findViewById(R.id.tvDaftarSiswaTitle)
        val recyclerSiswa: RecyclerView = view.findViewById(R.id.recyclerSiswaInClass)
        val pbLoading: ProgressBar = view.findViewById(R.id.pbLoadingSiswa)
        val cardHeader: View = view.findViewById(R.id.cardHeader)

        fun bind(kelas: KelasManagementItem, position: Int) {
            tvNamaKelas.text = kelas.label
            badgeSiswaCount.text = "${kelas.siswa_count} Siswa"
            tvWaliKelas.text = kelas.wali_kelas ?: "Belum ada wali kelas"
            tvDaftarSiswaTitle.text = "Daftar Siswa (${kelas.siswa_count})"

            val isExpanded = expandedPositions.contains(position)
            layoutExpandable.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpand.rotation = if (isExpanded) 90f else 0f

            btnUbahWali.setOnClickListener { onUbahWaliClick(kelas) }

            cardHeader.setOnClickListener {
                if (isExpanded) {
                    expandedPositions.remove(position)
                    notifyItemChanged(position)
                } else {
                    expandedPositions.add(position)
                    notifyItemChanged(position)
                    
                    if (!classStudentsMap.containsKey(kelas.id_kelas)) {
                        pbLoading.visibility = View.VISIBLE
                        onExpandClick(kelas) { students ->
                            classStudentsMap[kelas.id_kelas] = students
                            pbLoading.visibility = View.GONE
                            setupStudentList(recyclerSiswa, students)
                        }
                    } else {
                        setupStudentList(recyclerSiswa, classStudentsMap[kelas.id_kelas]!!)
                    }
                }
            }

            if (isExpanded && classStudentsMap.containsKey(kelas.id_kelas)) {
                setupStudentList(recyclerSiswa, classStudentsMap[kelas.id_kelas]!!)
            }
        }

        private fun setupStudentList(recyclerView: RecyclerView, students: List<SiswaItem>) {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
            recyclerView.adapter = SiswaInClassAdapter(students, onStudentDetailClick)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KelasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kelas_manage, parent, false)
        return KelasViewHolder(view)
    }

    override fun onBindViewHolder(holder: KelasViewHolder, position: Int) {
        holder.bind(kelasList[position], position)
    }

    override fun getItemCount(): Int = kelasList.size

    fun updateData(newList: List<KelasManagementItem>) {
        kelasList = newList
        expandedPositions.clear()
        classStudentsMap.clear()
        notifyDataSetChanged()
    }
}

class SiswaInClassAdapter(
    private val students: List<SiswaItem>,
    private val onDetailClick: (SiswaItem) -> Unit
) : RecyclerView.Adapter<SiswaInClassAdapter.SiswaViewHolder>() {

    inner class SiswaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNo: TextView = view.findViewById(R.id.tvNo)
        val tvNis: TextView = view.findViewById(R.id.tvNis)
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvJK: TextView = view.findViewById(R.id.tvJK)
        val btnDetail: MaterialButton = view.findViewById(R.id.btnDetail)

        fun bind(siswa: SiswaItem, position: Int) {
            tvNo.text = (position + 1).toString()
            tvNis.text = siswa.nis
            tvNama.text = siswa.nama_siswa
            tvJK.text = siswa.jenis_kelamin
            btnDetail.setOnClickListener { onDetailClick(siswa) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiswaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_siswa_manage_table_row, parent, false)
        return SiswaViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiswaViewHolder, position: Int) {
        holder.bind(students[position], position)
    }

    override fun getItemCount(): Int = students.size
}
