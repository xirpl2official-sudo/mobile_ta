package com.xirpl2.SASMobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.xirpl2.SASMobile.DhuhaScheduleAdapter
import com.xirpl2.SASMobile.R
import com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian
import com.xirpl2.SASMobile.model.JadwalSholatData

sealed class PrayerScheduleItem {
    data class DhuhaKeahlian(val data: List<JadwalDhuhaKeahlian>) : PrayerScheduleItem()
    data class PrayerCard(val jadwal: JadwalSholatData) : PrayerScheduleItem()
}

class PrayerScheduleAdapter(
    private var items: List<PrayerScheduleItem>,
    var canEdit: Boolean = true,
    private val onEditPrayer: (jenisSholat: String) -> Unit,
    private val onDhuhaKeahlianSwap: (row1: Int, col1: Int, row2: Int, col2: Int) -> Unit,
    private val onSaveDhuhaKeahlian: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DHUHA_KEAHLIAN = 0
        private const val TYPE_PRAYER_CARD = 1
    }

    private var dhuhaAdapter: DhuhaScheduleAdapter? = null
    private var isDhuhaEditMode = false

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PrayerScheduleItem.DhuhaKeahlian -> TYPE_DHUHA_KEAHLIAN
            is PrayerScheduleItem.PrayerCard -> TYPE_PRAYER_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DHUHA_KEAHLIAN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_prayer_dhuha_keahlian, parent, false)
                DhuhaKeahlianViewHolder(view)
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
            is PrayerScheduleItem.DhuhaKeahlian -> (holder as DhuhaKeahlianViewHolder).bind(item)
            is PrayerScheduleItem.PrayerCard -> (holder as PrayerCardViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<PrayerScheduleItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setDhuhaEditMode(editMode: Boolean) {
        isDhuhaEditMode = editMode
        dhuhaAdapter?.isEditMode = editMode
    }

    fun getDhuhaAdapter(): DhuhaScheduleAdapter? = dhuhaAdapter

    inner class DhuhaKeahlianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditDhuha)
        private val btnSave: MaterialButton = itemView.findViewById(R.id.btnSaveDhuha)
        private val rvDhuhaSchedule: RecyclerView = itemView.findViewById(R.id.rvDhuhaSchedule)

        fun bind(item: PrayerScheduleItem.DhuhaKeahlian) {
            if (dhuhaAdapter == null) {
                dhuhaAdapter = DhuhaScheduleAdapter(item.data) { row1, col1, row2, col2 ->
                    onDhuhaKeahlianSwap(row1, col1, row2, col2)
                    dhuhaAdapter?.let { it.updateData(getCurrentDhuhaData()) }
                }
                rvDhuhaSchedule.apply {
                    layoutManager = LinearLayoutManager(itemView.context)
                    adapter = dhuhaAdapter
                    isNestedScrollingEnabled = false
                }
            } else {
                dhuhaAdapter?.updateData(item.data)
            }

            if (canEdit) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    btnEdit.visibility = View.GONE
                    btnSave.visibility = View.VISIBLE
                    isDhuhaEditMode = true
                    dhuhaAdapter?.isEditMode = true
                }
                btnSave.setOnClickListener {
                    btnSave.visibility = View.GONE
                    btnEdit.visibility = View.VISIBLE
                    isDhuhaEditMode = false
                    dhuhaAdapter?.isEditMode = false
                    onSaveDhuhaKeahlian()
                }
            } else {
                btnEdit.visibility = View.GONE
                btnSave.visibility = View.GONE
            }
        }

        private fun getCurrentDhuhaData(): List<JadwalDhuhaKeahlian> {
            return (items.find { it is PrayerScheduleItem.DhuhaKeahlian } as? PrayerScheduleItem.DhuhaKeahlian)?.data ?: emptyList()
        }
    }

    inner class PrayerCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPrayerName: TextView = itemView.findViewById(R.id.tvPrayerName)
        private val tvPrayerDay: TextView = itemView.findViewById(R.id.tvPrayerDay)
        private val tvPrayerTime: TextView = itemView.findViewById(R.id.tvPrayerTime)
        private val tvPrayerJurusan: TextView = itemView.findViewById(R.id.tvPrayerJurusan)
        private val tvPrayerClass: TextView = itemView.findViewById(R.id.tvPrayerClass)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditPrayer)

        fun bind(item: PrayerScheduleItem.PrayerCard) {
            val jadwal = item.jadwal

            tvPrayerName.text = "Sholat ${jadwal.jenis_sholat}"

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
            } else {
                btnEdit.visibility = View.GONE
            }
        }
    }
}
