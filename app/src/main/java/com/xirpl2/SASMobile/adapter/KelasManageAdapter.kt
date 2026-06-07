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

data class JurusanGroup(
    val jurusan: String,
    val logoRes: Int,
    val kelas: List<KelasManagementItem>
)

class JurusanGroupAdapter(
    private val onUbahWaliClick: (KelasManagementItem) -> Unit,
    private val onExpandClick: (KelasManagementItem, (List<SiswaItem>) -> Unit) -> Unit,
    private val onStudentDetailClick: (SiswaItem) -> Unit
) : ListAdapter<JurusanGroup, JurusanGroupAdapter.GroupViewHolder>(GroupDiffCallback) {

    private val expandedGroups = mutableSetOf<String>()
    private val expandedKelas = mutableSetOf<Int>()
    private val classStudentsMap = mutableMapOf<Int, List<SiswaItem>>()
    private val loadingStates = mutableSetOf<Int>()

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardGroup: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardJurusanGroup)
        val jurusanHeader: View = view.findViewById(R.id.jurusanHeader)
        val ivJurusanLogo: ImageView = view.findViewById(R.id.ivJurusanLogo)
        val tvJurusanName: TextView = view.findViewById(R.id.tvJurusanName)
        val tvKelasCount: TextView = view.findViewById(R.id.tvKelasCount)
        val ivExpandGroup: ImageView = view.findViewById(R.id.ivExpandGroup)
        val layoutExpandableGroup: View = view.findViewById(R.id.layoutExpandableGroup)
        val rvKelasInGroup: RecyclerView = view.findViewById(R.id.rvKelasInGroup)

        private val kelasAdapter = KelasInGroupAdapter(
            onUbahWaliClick = onUbahWaliClick,
            onExpandClick = onExpandClick,
            onStudentDetailClick = onStudentDetailClick,
            expandedKelasIds = expandedKelas,
            classStudentsMap = classStudentsMap,
            loadingStates = loadingStates
        )

        init {
            rvKelasInGroup.layoutManager = LinearLayoutManager(view.context)
            rvKelasInGroup.adapter = kelasAdapter
        }

        fun bind(group: JurusanGroup) {
            ivJurusanLogo.setImageResource(group.logoRes)
            tvJurusanName.text = group.jurusan
            tvKelasCount.text = "${group.kelas.size} Kelas"

            val isExpanded = expandedGroups.contains(group.jurusan)
            layoutExpandableGroup.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpandGroup.rotation = if (isExpanded) 90f else 0f

            kelasAdapter.submitList(group.kelas)

            jurusanHeader.setOnClickListener {
                val currentlyExpanded = expandedGroups.contains(group.jurusan)
                if (currentlyExpanded) {
                    expandedGroups.remove(group.jurusan)
                    layoutExpandableGroup.visibility = View.GONE
                    ivExpandGroup.animate().rotation(0f).setDuration(200).start()
                } else {
                    expandedGroups.add(group.jurusan)
                    layoutExpandableGroup.visibility = View.VISIBLE
                    ivExpandGroup.animate().rotation(90f).setDuration(200).start()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_jurusan_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateGroups(groups: List<JurusanGroup>) {
        submitList(groups)
    }
}

private object GroupDiffCallback : DiffUtil.ItemCallback<JurusanGroup>() {
    override fun areItemsTheSame(oldItem: JurusanGroup, newItem: JurusanGroup) = oldItem.jurusan == newItem.jurusan
    override fun areContentsTheSame(oldItem: JurusanGroup, newItem: JurusanGroup) = oldItem == newItem
}

class KelasInGroupAdapter(
    private val onUbahWaliClick: (KelasManagementItem) -> Unit,
    private val onExpandClick: (KelasManagementItem, (List<SiswaItem>) -> Unit) -> Unit,
    private val onStudentDetailClick: (SiswaItem) -> Unit,
    private val expandedKelasIds: MutableSet<Int>,
    private val classStudentsMap: MutableMap<Int, List<SiswaItem>>,
    private val loadingStates: MutableSet<Int>
) : ListAdapter<KelasManagementItem, KelasInGroupAdapter.KelasViewHolder>(KelasDiffCallback) {

    inner class KelasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaKelas: TextView = view.findViewById(R.id.tvNamaKelas)
        val badgeSiswaCount: TextView = view.findViewById(R.id.badgeSiswaCount)
        val layoutWaliInfo: View = view.findViewById(R.id.layoutWaliInfo)
        val tvWaliKelas: TextView = view.findViewById(R.id.tvWaliKelas)
        val btnUbahWaliQuick: MaterialButton = view.findViewById(R.id.btnUbahWaliQuick)
        val btnUbahWaliSection: MaterialButton = view.findViewById(R.id.btnUbahWaliSection)
        val ivExpandKelas: ImageView = view.findViewById(R.id.ivExpandKelas)
        val pbSavingWali: ProgressBar = view.findViewById(R.id.pbSavingWali)
        val cardKelas: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardKelas)
        val kelasHeader: View = view.findViewById(R.id.kelasHeader)

        val layoutExpandableKelas: View = view.findViewById(R.id.layoutExpandableKelas)
        val recyclerSiswa: RecyclerView = view.findViewById(R.id.recyclerSiswaInClass)
        val pbLoadingSiswa: ProgressBar = view.findViewById(R.id.pbLoadingSiswa)
        val emptyStateSiswa: View = view.findViewById(R.id.emptyStateSiswa)
        val tvDaftarSiswaTitle: TextView = view.findViewById(R.id.tvDaftarSiswaTitle)

        private val siswaAdapter = SiswaInClassAdapter(onStudentDetailClick)

        init {
            recyclerSiswa.layoutManager = LinearLayoutManager(view.context)
            recyclerSiswa.adapter = siswaAdapter
        }

        fun bind(kelas: KelasManagementItem) {
            val context = itemView.context

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
            val isExpanded = expandedKelasIds.contains(kelas.id_kelas)
            layoutExpandableKelas.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpandKelas.rotation = if (isExpanded) 90f else 0f

            kelasHeader.setOnClickListener {
                val currentlyExpanded = expandedKelasIds.contains(kelas.id_kelas)
                if (currentlyExpanded) {
                    expandedKelasIds.remove(kelas.id_kelas)
                    layoutExpandableKelas.visibility = View.GONE
                    ivExpandKelas.animate().rotation(0f).setDuration(200).start()
                } else {
                    expandedKelasIds.add(kelas.id_kelas)
                    layoutExpandableKelas.visibility = View.VISIBLE
                    ivExpandKelas.animate().rotation(90f).setDuration(200).start()

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
                                siswaAdapter.submitList(students)
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
                            siswaAdapter.submitList(students)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KelasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kelas_in_group, parent, false)
        return KelasViewHolder(view)
    }

    override fun onBindViewHolder(holder: KelasViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object KelasDiffCallback : DiffUtil.ItemCallback<KelasManagementItem>() {
    override fun areItemsTheSame(oldItem: KelasManagementItem, newItem: KelasManagementItem) = oldItem.id_kelas == newItem.id_kelas
    override fun areContentsTheSame(oldItem: KelasManagementItem, newItem: KelasManagementItem) = oldItem == newItem
}

class SiswaInClassAdapter(
    private val onDetailClick: (SiswaItem) -> Unit
) : ListAdapter<SiswaItem, SiswaInClassAdapter.SiswaViewHolder>(SiswaInGroupDiffCallback) {

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
        holder.bind(getItem(position), position)
    }
}

private object SiswaInGroupDiffCallback : DiffUtil.ItemCallback<SiswaItem>() {
    override fun areItemsTheSame(oldItem: SiswaItem, newItem: SiswaItem) = oldItem.nis == newItem.nis
    override fun areContentsTheSame(oldItem: SiswaItem, newItem: SiswaItem) = oldItem == newItem
}
