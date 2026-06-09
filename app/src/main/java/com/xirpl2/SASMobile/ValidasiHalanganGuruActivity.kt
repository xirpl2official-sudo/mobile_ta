package com.xirpl2.SASMobile

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.xirpl2.SASMobile.model.HalanganPerizinan
import com.xirpl2.SASMobile.repository.PerizinanHalanganRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ValidasiHalanganGuruActivity : BaseAdminActivity() {

    private lateinit var rvPending: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var qrOverlay: View
    private lateinit var ivQRCode: ImageView
    private lateinit var tvQrSiswaInfo: TextView
    private lateinit var tvQrPeriode: TextView
    private lateinit var btnTutupQR: MaterialButton

    private val repository = PerizinanHalanganRepository()
    private val adapter = PendingHalanganAdapter { item -> showQR(item) }

    override fun getCurrentMenuItem(): AdminMenuItem = AdminMenuItem.VALIDASI_HALANGAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_validasi_halangan_guru)
        window.statusBarColor = 0xFF2886D6.toInt()

        initViews()
        setupDrawerAndSidebar()
        setupMenuIcon()

        rvPending.layoutManager = LinearLayoutManager(this)
        rvPending.adapter = adapter

        btnTutupQR.setOnClickListener { qrOverlay.visibility = View.GONE }
        qrOverlay.setOnClickListener { qrOverlay.visibility = View.GONE }

        loadPendingList()
    }

    private fun initViews() {
        rvPending = findViewById(R.id.rvPendingHalangan)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmptyPending)
        qrOverlay = findViewById(R.id.qrOverlay)
        ivQRCode = findViewById(R.id.ivQRCode)
        tvQrSiswaInfo = findViewById(R.id.tvQrSiswaInfo)
        tvQrPeriode = findViewById(R.id.tvQrPeriode)
        btnTutupQR = findViewById(R.id.btnTutupQR)
    }

    private fun loadPendingList() {
        val token = getAuthToken()
        if (token.isEmpty()) return
        progressBar.visibility = View.VISIBLE
        rvPending.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            repository.getPendingHalangan(token).fold(
                onSuccess = { list ->
                    progressBar.visibility = View.GONE
                    if (list.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvPending.visibility = View.VISIBLE
                        adapter.submitList(list)
                    }
                },
                onFailure = {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@ValidasiHalanganGuruActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showQR(item: HalanganPerizinan) {
        val nama = item.siswa?.namaSiswa ?: "Siswa"
        val kelas = item.siswa?.kelas ?: ""
        val jurusan = item.siswa?.jurusan ?: ""

        tvQrSiswaInfo.text = "$nama\n$kelas - $jurusan"
        tvQrPeriode.text = "${item.tanggalMulai} s/d ${item.tanggalSelesai}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val qrContent = item.id.toString()
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400)
                val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
                for (x in 0 until 400) {
                    for (y in 0 until 400) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                    }
                }
                withContext(Dispatchers.Main) {
                    ivQRCode.setImageBitmap(bitmap)
                    qrOverlay.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ValidasiHalanganGuruActivity, "Gagal generate QR", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class PendingHalanganAdapter(
        private val onItemClick: (HalanganPerizinan) -> Unit
    ) : RecyclerView.Adapter<PendingHalanganAdapter.VH>() {

        private var items = listOf<HalanganPerizinan>()

        fun submitList(list: List<HalanganPerizinan>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_halangan_pending, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], onItemClick)
        }

        override fun getItemCount() = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvNamaSiswa: TextView = view.findViewById(R.id.tvNamaSiswa)
            val tvInfoSiswa: TextView = view.findViewById(R.id.tvInfoSiswa)
            val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
            val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
            val btnTampilkanQR: MaterialButton = view.findViewById(R.id.btnTampilkanQR)

            fun bind(item: HalanganPerizinan, onClick: (HalanganPerizinan) -> Unit) {
                val nama = item.siswa?.namaSiswa ?: "-"
                val nis = item.siswa?.nis ?: ""
                val kelas = item.siswa?.kelas ?: ""
                val jurusan = item.siswa?.jurusan ?: ""

                tvNamaSiswa.text = nama
                tvInfoSiswa.text = "$nis  |  $kelas - $jurusan"
                tvTanggal.text = "Pengajuan: ${item.tanggalMulai} s/d ${item.tanggalSelesai}"

                val ctx = itemView.context
                when (item.statusValidasi) {
                    "istihadhah_check" -> {
                        tvStatusBadge.text = "Istihadhah"
                        tvStatusBadge.setTextColor(ctx.getColor(R.color.status_warning))
                    }
                    else -> {
                        tvStatusBadge.text = "Pending"
                        tvStatusBadge.setTextColor(ctx.getColor(R.color.blue_theme))
                    }
                }

                btnTampilkanQR.setOnClickListener { onClick(item) }
                itemView.setOnClickListener { onClick(item) }
            }
        }
    }
}
