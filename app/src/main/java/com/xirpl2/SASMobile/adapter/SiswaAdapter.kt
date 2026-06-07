package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.SiswaItem

class SiswaAdapter(
    private val onDetailClick: (SiswaItem) -> Unit,
    private val onMoreMenuClick: (View, SiswaItem) -> Unit = { _, _ -> },
    private val isReadOnly: Boolean = false,
    private val isUnregistered: Boolean = false
) : ListAdapter<SiswaItem, RecyclerView.ViewHolder>(SiswaDiffCallback()) {

    private val selectedNis = mutableSetOf<String>()
    private var isSelectionMode = false
    private var onSelectionChanged: ((Int) -> Unit)? = null

    private var fullList: List<SiswaItem> = emptyList()
    private var currentPage = 0
    private val pageSize = 20

    fun setFullList(list: List<SiswaItem>) {
        fullList = list
        currentPage = 0
        selectedNis.clear()
        onSelectionChanged?.invoke(0)
        refreshPage()
    }

    fun getTotalPages(): Int = if (fullList.isEmpty()) 1 else Math.ceil(fullList.size.toDouble() / pageSize).toInt()

    fun getCurrentPage(): Int = currentPage + 1

    fun getTotalItems(): Int = fullList.size

    fun goToPage(page: Int) {
        val zeroBased = page - 1
        if (zeroBased in 0 until getTotalPages()) {
            currentPage = zeroBased
            refreshPage()
        }
    }

    fun nextPage() { goToPage(currentPage + 2) }
    fun prevPage() { goToPage(currentPage) }

    private fun refreshPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullList.size)
        val pageItems = if (start < fullList.size) fullList.subList(start, end) else emptyList()
        submitList(pageItems.toList())
    }

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChanged = listener
    }

    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            if (!enabled) selectedNis.clear()
            notifyDataSetChanged()
            onSelectionChanged?.invoke(selectedNis.size)
        }
    }

    fun toggleSelection(nis: String) {
        if (selectedNis.contains(nis)) selectedNis.remove(nis) else selectedNis.add(nis)
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedNis.size)
    }

    fun selectAll(select: Boolean) {
        if (select) fullList.forEach { selectedNis.add(it.nis) } else selectedNis.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedNis.size)
    }

    fun getSelectedItems(): List<SiswaItem> = fullList.filter { selectedNis.contains(it.nis) }
    fun getSelectedCount(): Int = selectedNis.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = if (isUnregistered) R.layout.item_siswa_unregistered_row else R.layout.item_siswa
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return SiswaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SiswaViewHolder -> {
                val siswa = getItem(position)
                holder.bind(siswa, onDetailClick, onMoreMenuClick, isReadOnly, isSelectionMode, selectedNis.contains(siswa.nis)) {
                    toggleSelection(siswa.nis)
                }
            }
        }
    }

    class SiswaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNis: TextView = itemView.findViewById(R.id.tvNis)
        private val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        private val tvKelas: TextView = itemView.findViewById(R.id.tvKelas)
        private val tvJurusan: TextView? = itemView.findViewById(R.id.tvJurusan)
        private val tvWaliKelas: TextView? = itemView.findViewById(R.id.tvWaliKelas)
        private val btnDetail: MaterialButton? = itemView.findViewById(R.id.btnDetail)
        private val btnMoreMenu: ImageView? = itemView.findViewById(R.id.btnMoreMenu)
        private val cbRow: CheckBox? = itemView.findViewById(R.id.cbRow)
        private val tvJenisKelamin: TextView? = itemView.findViewById(R.id.tvJenisKelamin)
        private val tvStatusAkademik: TextView? = itemView.findViewById(R.id.tvStatusAkademik)

        fun bind(
            siswa: SiswaItem,
            onDetailClick: (SiswaItem) -> Unit,
            onMoreMenuClick: (View, SiswaItem) -> Unit,
            isReadOnly: Boolean,
            selectionMode: Boolean,
            isSelected: Boolean,
            onToggleSelection: () -> Unit
        ) {
            tvNis.text = siswa.nis

            val safeNis = siswa.nis ?: ""
            if (safeNis.contains("/") || safeNis.contains(".")) {
                tvNis.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.slate_400))
                tvNis.setTypeface(null, android.graphics.Typeface.ITALIC)
            } else {
                tvNis.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.blue_primary))
                tvNis.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            tvNama.text = siswa.nama_siswa
            tvKelas.text = siswa.kelas
            tvJurusan?.text = siswa.jurusan
            tvWaliKelas?.text = siswa.waliKelasName?.ifEmpty { "-" } ?: "-"
            tvJenisKelamin?.text = siswa.jenis_kelamin
            tvStatusAkademik?.text = siswa.statusAkademik?.uppercase() ?: "AKTIF"

            cbRow?.let {
                it.visibility = View.VISIBLE
                it.isChecked = isSelected
                it.setOnClickListener { onToggleSelection() }
            }

            itemView.setOnClickListener {
                if (selectionMode) onToggleSelection() else onDetailClick(siswa)
            }

            itemView.setOnLongClickListener {
                if (!selectionMode) onToggleSelection()
                true
            }

            btnDetail?.setOnClickListener { onDetailClick(siswa) }

            if (isReadOnly || selectionMode) {
                btnMoreMenu?.visibility = View.GONE
            } else {
                btnMoreMenu?.visibility = View.VISIBLE
                btnMoreMenu?.setOnClickListener { onMoreMenuClick(it, siswa) }
            }
        }
    }
}

class SiswaDiffCallback : DiffUtil.ItemCallback<SiswaItem>() {
    override fun areItemsTheSame(oldItem: SiswaItem, newItem: SiswaItem): Boolean {
        return oldItem.nis == newItem.nis
    }

    override fun areContentsTheSame(oldItem: SiswaItem, newItem: SiswaItem): Boolean {
        return oldItem == newItem
    }
}
