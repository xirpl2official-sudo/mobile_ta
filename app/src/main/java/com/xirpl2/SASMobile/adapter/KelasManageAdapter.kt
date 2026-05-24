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
    private val loadingStates = mutableSetOf<Int>()

    fun setLoading(idKelas: Int, isLoading: Boolean) {
        if (isLoading) loadingStates.add(idKelas) else loadingStates.remove(idKelas)
        notifyDataSetChanged()
    }

    inner class KelasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutJurusanHeader: View = view.findViewById(R.id.layoutJurusanHeader)
        val tvJurusanTitle: TextView = view.findViewById(R.id.tvJurusanTitle)
        val tvJurusanCount: TextView = view.findViewById(R.id.tvJurusanCount)

        val cardContainer: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardContainer)
        val ivJurusanLogo: ImageView = view.findViewById(R.id.ivJurusanLogo)
        val tvNamaKelas: TextView = view.findViewById(R.id.tvNamaKelas)
        val badgeSiswaCount: TextView = view.findViewById(R.id.badgeSiswaCount)
        val layoutWaliInfo: View = view.findViewById(R.id.layoutWaliInfo)
        val tvWaliKelas: TextView = view.findViewById(R.id.tvWaliKelas)
        val btnUbahWaliQuick: MaterialButton = view.findViewById(R.id.btnUbahWaliQuick)
        val btnUbahWaliSection: MaterialButton = view.findViewById(R.id.btnUbahWaliSection)
        val ivExpand: ImageView = view.findViewById(R.id.ivExpand)
        val pbSavingWali: ProgressBar = view.findViewById(R.id.pbSavingWali)
        
        val layoutExpandable: View = view.findViewById(R.id.layoutExpandable)
        val recyclerSiswa: RecyclerView = view.findViewById(R.id.recyclerSiswaInClass)
        val pbLoadingSiswa: ProgressBar = view.findViewById(R.id.pbLoadingSiswa)
        val emptyStateSiswa: View = view.findViewById(R.id.emptyStateSiswa)
        val tvDaftarSiswaTitle: TextView = view.findViewById(R.id.tvDaftarSiswaTitle)

        private val siswaAdapter = SiswaInClassAdapter(onStudentDetailClick)

        init {
            recyclerSiswa.layoutManager = LinearLayoutManager(view.context)
            recyclerSiswa.adapter = siswaAdapter
        }

        fun bind(kelas: KelasManagementItem, position: Int) {
            val context = itemView.context
            
            // Visual Jurusan
            val logoRes = when(kelas.jurusan?.uppercase()) {
                "RPL" -> R.drawable.logo_rpl
                "TKJ" -> R.drawable.logo_tkj
                "DKV" -> R.drawable.logo_dkv
                "TEI" -> R.drawable.logo_tei
                "BC" -> R.drawable.logo_bc
                "TMT" -> R.drawable.logo_mt
                "TAV" -> R.drawable.logo_tav
                else -> R.drawable.ic_class
            }
            ivJurusanLogo.setImageResource(logoRes)

            tvNamaKelas.text = "${kelas.tingkatan} ${kelas.part}"
            badgeSiswaCount.text = "${kelas.siswa_count} Siswa"
            tvDaftarSiswaTitle.text = "Daftar Siswa (${kelas.siswa_count})"
            
            // Wali Kelas UI
            if (kelas.wali_kelas.isNullOrBlank()) {
                tvWaliKelas.text = "Wali belum diatur"
                tvWaliKelas.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                layoutWaliInfo.setBackgroundResource(R.drawable.bg_card_warning_border)
            } else {
                tvWaliKelas.text = kelas.wali_kelas
                tvWaliKelas.setTextColor(ContextCompat.getColor(context, R.color.slate_700))
                layoutWaliInfo.setBackgroundResource(R.drawable.bg_card_default_border)
            }

            val isLoading = loadingStates.contains(kelas.id_kelas)
            pbSavingWali.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnUbahWaliQuick.isEnabled = !isLoading
            btnUbahWaliSection.isEnabled = !isLoading
            
            btnUbahWaliQuick.setOnClickListener { onUbahWaliClick(kelas) }
            btnUbahWaliSection.setOnClickListener { onUbahWaliClick(kelas) }

            // Expand logic
            val isExpanded = expandedIds.contains(kelas.id_kelas)
            layoutExpandable.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpand.rotation = if (isExpanded) 90f else 0f

            cardContainer.setOnClickListener {
                if (isExpanded) {
                    expandedIds.remove(kelas.id_kelas)
                    layoutExpandable.visibility = View.GONE
                    ivExpand.animate().rotation(0f).setDuration(200).start()
                } else {
                    expandedIds.add(kelas.id_kelas)
                    layoutExpandable.visibility = View.VISIBLE
                    ivExpand.animate().rotation(90f).setDuration(200).start()
                    
                    if (!classStudentsMap.containsKey(kelas.id_kelas)) {
                        pbLoadingSiswa.visibility = View.VISIBLE
                        recyclerSiswa.visibility = View.GONE
                        emptyStateSiswa.visibility = View.GONE
                        
                        onExpandClick(kelas) { students ->
                            classStudentsMap[kelas.id_kelas] = students
                            pbLoadingSiswa.visibility = View.GONE
                            if (students.isEmpty()) {
                                emptyStateSiswa.visibility = View.VISIBLE
                                recyclerSiswa.visibility = View.GONE
                            } else {
                                emptyStateSiswa.visibility = View.GONE
                                recyclerSiswa.visibility = View.VISIBLE
                                siswaAdapter.updateStudents(students)
                            }
                        }
                    } else {
                        val students = classStudentsMap[kelas.id_kelas]!!
                        pbLoadingSiswa.visibility = View.GONE
                        if (students.isEmpty()) {
                            emptyStateSiswa.visibility = View.VISIBLE
                            recyclerSiswa.visibility = View.GONE
                        } else {
                            emptyStateSiswa.visibility = View.GONE
                            recyclerSiswa.visibility = View.VISIBLE
                            siswaAdapter.updateStudents(students)
                        }
                    }
                }
            }
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
