package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.KelasManagementItem
import com.xirpl2.SASMobile.model.SiswaItem

class KelasManageAdapter(
    private val onUbahWaliClick: (KelasManagementItem) -> Unit,
    private val onExpandClick: (KelasManagementItem, (List<SiswaItem>) -> Unit) -> Unit,
    private val onStudentDetailClick: (SiswaItem) -> Unit
) : ListAdapter<KelasManagementItem, KelasManageAdapter.KelasViewHolder>(KelasDiffCallback) {

    private val expandedIds = mutableSetOf<Int>()
    private val classStudentsMap = mutableMapOf<Int, List<SiswaItem>>()

    inner class KelasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutJurusanHeader: View = view.findViewById(R.id.layoutJurusanHeader)
        val tvJurusanTitle: TextView = view.findViewById(R.id.tvJurusanTitle)
        val tvJurusanCount: TextView = view.findViewById(R.id.tvJurusanCount)

        val cardContainer: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardContainer)
        val tvNamaKelas: TextView = view.findViewById(R.id.tvNamaKelas)
        val badgeSiswaCount: TextView = view.findViewById(R.id.badgeSiswaCount)
        val badgeWarningWali: View = view.findViewById(R.id.badgeWarningWali)
        val tvWaliKelas: TextView = view.findViewById(R.id.tvWaliKelas)
        val tvSectionWaliTitle: TextView = view.findViewById(R.id.tvSectionWaliTitle)
        val tvSectionWaliSubtitle: TextView = view.findViewById(R.id.tvSectionWaliSubtitle)
        val btnUbahWali: MaterialButton = view.findViewById(R.id.btnUbahWali)
        val ivExpand: ImageView = view.findViewById(R.id.ivExpand)
        val layoutExpandable: View = view.findViewById(R.id.layoutExpandable)
        val emptyStateSiswa: View = view.findViewById(R.id.emptyStateSiswa)
        val btnEmptyTambahSiswa: MaterialButton = view.findViewById(R.id.btnEmptyTambahSiswa)
        val tvDaftarSiswaTitle: TextView = view.findViewById(R.id.tvDaftarSiswaTitle)
        val recyclerSiswa: RecyclerView = view.findViewById(R.id.recyclerSiswaInClass)
        val pbLoading: ProgressBar = view.findViewById(R.id.pbLoadingSiswa)
        val cardHeader: View = view.findViewById(R.id.cardHeader)

        // Cache LayoutManager and adapter to avoid recreating on every bind
        private val siswaLayoutManager = LinearLayoutManager(recyclerSiswa.context)
        private val siswaAdapter = SiswaInClassAdapter(onStudentDetailClick)

        init {
            recyclerSiswa.layoutManager = siswaLayoutManager
            recyclerSiswa.adapter = siswaAdapter
        }

        fun bind(kelas: KelasManagementItem, position: Int) {
            val context = itemView.context
            val list = currentList

            val showHeader = position == 0 || list[position - 1].jurusan != kelas.jurusan
            if (showHeader) {
                layoutJurusanHeader.visibility = View.VISIBLE
                tvJurusanTitle.text = kelas.jurusan ?: "Umum"
                val countInJurusan = list.count { it.jurusan == kelas.jurusan }
                tvJurusanCount.text = "$countInJurusan Kelas"
            } else {
                layoutJurusanHeader.visibility = View.GONE
            }

            tvNamaKelas.text = kelas.label

            // ISS-004: Semantic Student Badge Colors
            badgeSiswaCount.text = "${kelas.siswa_count} Siswa"
            when {
                kelas.siswa_count == 0 -> {
                    badgeSiswaCount.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_error)
                }
                kelas.siswa_count < 10 -> {
                    badgeSiswaCount.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_warning)
                }
                else -> {
                    badgeSiswaCount.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_success)
                }
            }

            // ISS-002: Warning State for Missing Wali Kelas
            if (kelas.wali_kelas.isNullOrBlank()) {
                tvWaliKelas.text = "Wali belum ditentukan"
                tvWaliKelas.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                badgeWarningWali.visibility = View.VISIBLE
                cardContainer.setStrokeColor(ContextCompat.getColorStateList(context, R.color.status_warning))
                cardContainer.strokeWidth = context.resources.displayMetrics.density.toInt() * 2

                tvSectionWaliTitle.text = "Wali Kelas"
                tvSectionWaliSubtitle.text = "Pilih guru yang bertanggung jawab atas kelas ini."
                btnUbahWali.text = "Atur Wali"
            } else {
                tvWaliKelas.text = kelas.wali_kelas
                tvWaliKelas.setTextColor(android.graphics.Color.parseColor("#334155"))
                badgeWarningWali.visibility = View.GONE
                cardContainer.setStrokeColor(ContextCompat.getColorStateList(context, R.color.slate_200))
                cardContainer.strokeWidth = context.resources.displayMetrics.density.toInt() * 1

                tvSectionWaliTitle.text = kelas.wali_kelas
                tvSectionWaliSubtitle.text = "Wali Kelas saat ini"
                btnUbahWali.text = "Ganti Wali"
            }

            tvDaftarSiswaTitle.text = "Daftar Siswa (${kelas.siswa_count})"

            val isExpanded = expandedIds.contains(kelas.id_kelas)
            layoutExpandable.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpand.rotation = if (isExpanded) 90f else 0f

            // ISS-001: Empty State for 0 Students
            if (isExpanded && kelas.siswa_count == 0) {
                emptyStateSiswa.visibility = View.VISIBLE
                recyclerSiswa.visibility = View.GONE
                tvDaftarSiswaTitle.visibility = View.GONE

                btnEmptyTambahSiswa.setOnClickListener {
                    onStudentDetailClick(SiswaItem(id_siswa = 0, nis = "", nama_siswa = "NEW"))
                }
            } else {
                emptyStateSiswa.visibility = View.GONE
                recyclerSiswa.visibility = if (isExpanded) View.VISIBLE else View.GONE
                tvDaftarSiswaTitle.visibility = if (isExpanded) View.VISIBLE else View.GONE
            }

            btnUbahWali.setOnClickListener { onUbahWaliClick(kelas) }

            cardHeader.setOnClickListener {
                if (isExpanded) {
                    expandedIds.remove(kelas.id_kelas)
                    notifyItemChanged(position)
                } else {
                    expandedIds.add(kelas.id_kelas)
                    notifyItemChanged(position)

                    if (kelas.siswa_count > 0 && !classStudentsMap.containsKey(kelas.id_kelas)) {
                        pbLoading.visibility = View.VISIBLE
                        onExpandClick(kelas) { students ->
                            classStudentsMap[kelas.id_kelas] = students
                            pbLoading.visibility = View.GONE
                            setupStudentList(recyclerSiswa, students)
                        }
                    } else if (kelas.siswa_count > 0) {
                        setupStudentList(recyclerSiswa, classStudentsMap[kelas.id_kelas]!!)
                    }
                }
            }

            if (isExpanded && classStudentsMap.containsKey(kelas.id_kelas)) {
                setupStudentList(recyclerSiswa, classStudentsMap[kelas.id_kelas]!!)
            }
        }

        private fun setupStudentList(recyclerView: RecyclerView, students: List<SiswaItem>) {
            siswaAdapter.updateStudents(students)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KelasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kelas_manage, parent, false)
        return KelasViewHolder(view)
    }

    override fun onBindViewHolder(holder: KelasViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    fun updateData(newList: List<KelasManagementItem>) {
        val removedIds = currentList.map { it.id_kelas }.toSet() - newList.map { it.id_kelas }.toSet()
        removedIds.forEach { id_kelas ->
            expandedIds.remove(id_kelas)
            classStudentsMap.remove(id_kelas)
        }
        submitList(newList)
    }
}

private object KelasDiffCallback : DiffUtil.ItemCallback<KelasManagementItem>() {
    override fun areItemsTheSame(oldItem: KelasManagementItem, newItem: KelasManagementItem): Boolean {
        return oldItem.id_kelas == newItem.id_kelas
    }

    override fun areContentsTheSame(oldItem: KelasManagementItem, newItem: KelasManagementItem): Boolean {
        return oldItem == newItem
    }
}

class SiswaInClassAdapter(
    private val onDetailClick: (SiswaItem) -> Unit
) : RecyclerView.Adapter<SiswaInClassAdapter.SiswaViewHolder>() {

    private var students: List<SiswaItem> = emptyList()

    fun updateStudents(newStudents: List<SiswaItem>) {
        students = newStudents
        notifyDataSetChanged()
    }

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
