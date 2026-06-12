package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.DuhaScheduleAdapter
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.JadwalDuhaKeahlian
import com.xirpl2.SASMobile.model.JadwalSholatData

sealed class PrayerScheduleItem {
    data class DuhaKeahlian(val data: List<JadwalDuhaKeahlian>) : PrayerScheduleItem()
    data class PrayerCard(val jadwal: JadwalSholatData) : PrayerScheduleItem()
}

class PrayerScheduleAdapter(
    private var items: List<PrayerScheduleItem>,
    var canEdit: Boolean = true,
    private val onEditPrayer: (jenisSholat: String) -> Unit,
    private val onDeletePrayer: (jadwal: JadwalSholatData) -> Unit = {},
    private val onDuhaKeahlianSwap: (row1: Int, col1: Int, row2: Int, col2: Int) -> Unit,
    private val onSaveDuhaKeahlian: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_Duha_KEAHLIAN = 0
        private const val TYPE_PRAYER_CARD = 1
    }

    private var DuhaAdapter: DuhaScheduleAdapter? = null
    private var isDuhaEditMode = false

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PrayerScheduleItem.DuhaKeahlian -> TYPE_Duha_KEAHLIAN
            is PrayerScheduleItem.PrayerCard -> TYPE_PRAYER_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_Duha_KEAHLIAN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_prayer_duha_keahlian, parent, false)
                DuhaKeahlianViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_prayer_schedule_card, parent, false)
                PrayerCardViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PrayerScheduleItem.DuhaKeahlian -> (holder as DuhaKeahlianViewHolder).bind(item)
            is PrayerScheduleItem.PrayerCard -> (holder as PrayerCardViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<PrayerScheduleItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setDuhaEditMode(editMode: Boolean) {
        isDuhaEditMode = editMode
        DuhaAdapter?.isEditMode = editMode
    }

    fun getDuhaAdapter(): DuhaScheduleAdapter? = DuhaAdapter

    inner class DuhaKeahlianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditDuha)
        private val btnSave: MaterialButton = itemView.findViewById(R.id.btnSaveDuha)
        private val rvDuhaSchedule: RecyclerView = itemView.findViewById(R.id.rvDuhaSchedule)

        fun bind(item: PrayerScheduleItem.DuhaKeahlian) {
            if (DuhaAdapter == null) {
                DuhaAdapter = DuhaScheduleAdapter { row1, col1, row2, col2 ->
                    onDuhaKeahlianSwap(row1, col1, row2, col2)
                }
                DuhaAdapter?.submitList(item.data)
                rvDuhaSchedule.apply {
                    layoutManager = LinearLayoutManager(itemView.context)
                    adapter = DuhaAdapter
                    isNestedScrollingEnabled = false
                }
            } else {
                DuhaAdapter?.submitList(item.data)
            }

            if (canEdit) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    btnEdit.visibility = View.GONE
                    btnSave.visibility = View.VISIBLE
                    isDuhaEditMode = true
                    DuhaAdapter?.isEditMode = true
                }
                btnSave.setOnClickListener {
                    btnSave.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    isDuhaEditMode = false
                    DuhaAdapter?.isEditMode = false
                    onSaveDuhaKeahlian()
                }
            } else {
                btnEdit.visibility = View.GONE
                btnSave.visibility = View.GONE
            }
        }

        private fun getCurrentDuhaData(): List<JadwalDuhaKeahlian> {
            return (items.find { it is PrayerScheduleItem.DuhaKeahlian } as? PrayerScheduleItem.DuhaKeahlian)?.data ?: emptyList()
        }
    }

    inner class PrayerCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPrayerName: TextView = itemView.findViewById(R.id.tvPrayerName)
        private val tvPrayerDay: TextView = itemView.findViewById(R.id.tvPrayerDay)
        private val tvPrayerTime: TextView = itemView.findViewById(R.id.tvPrayerTime)
        private val tvPrayerJurusan: TextView = itemView.findViewById(R.id.tvPrayerJurusan)
        private val tvPrayerClass: TextView = itemView.findViewById(R.id.tvPrayerClass)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditPrayer)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeletePrayer)

        fun bind(item: PrayerScheduleItem.PrayerCard) {
            val jadwal = item.jadwal

            // Add extra margin for first prayer card after Duha Keahlian table
            val cardView = itemView as com.google.android.material.card.MaterialCardView
            val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
            
            // Check if this is the first prayer card (position after Duha Keahlian)
            val isFirstCard = bindingAdapterPosition == 1 && items.firstOrNull() is PrayerScheduleItem.DuhaKeahlian
            
            if (isFirstCard) {
                // Add extra top margin (80dp) for first card to avoid FAB overlap
                layoutParams.topMargin = (40 * itemView.context.resources.displayMetrics.density).toInt()
            } else {
                // Normal margin for other cards
                layoutParams.topMargin = (8 * itemView.context.resources.displayMetrics.density).toInt()
            }
            cardView.layoutParams = layoutParams

            tvPrayerName.text = jadwal.jenis_sholat

            if (!jadwal.hari.isNullOrEmpty()) {
                tvPrayerDay.text = jadwal.hari
                tvPrayerDay.visibility = View.VISIBLE
            } else {
                tvPrayerDay.visibility = View.GONE
            }

            if (!jadwal.jam_mulai.isNullOrEmpty()) {
                tvPrayerTime.text = "Waktu: ${jadwal.jam_mulai} - ${jadwal.jam_selesai}"
                tvPrayerTime.visibility = View.VISIBLE
            } else {
                tvPrayerTime.visibility = View.GONE
            }

            if (!jadwal.jurusan.isNullOrEmpty()) {
                tvPrayerJurusan.text = "Jurusan: ${jadwal.jurusan}"
                tvPrayerJurusan.visibility = View.VISIBLE
            } else {
                tvPrayerJurusan.visibility = View.GONE
            }

            if (!jadwal.kelas.isNullOrEmpty()) {
                tvPrayerClass.text = "Kelas: ${jadwal.kelas}"
                tvPrayerClass.visibility = View.VISIBLE
            } else {
                tvPrayerClass.text = "Kelas: Semua Kelas"
                tvPrayerClass.visibility = View.VISIBLE
            }

            if (canEdit) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    onEditPrayer(jadwal.jenis_sholat)
                }
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    onDeletePrayer(jadwal)
                }
            } else {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }
        }
    }
}
