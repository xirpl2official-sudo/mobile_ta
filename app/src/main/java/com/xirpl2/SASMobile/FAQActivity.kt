package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.adapter.FAQAdapter
import com.xirpl2.SASMobile.model.FAQItem

class FAQActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerFAQ: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FAQAdapter

    private val allFaqs = listOf(
        FAQItem("Bagaimana cara melakukan absensi?", "Anda dapat melakukan absensi dengan memindai kode QR yang ditampilkan oleh Admin menggunakan fitur 'Scan QR' di dashboard siswa."),
        FAQItem("Apa yang harus saya lakukan jika lupa kata sandi?", "Klik 'Lupa kata sandi?' di halaman login. Masukkan NIS dan email Anda untuk menerima kode OTP, lalu ikuti langkah-langkah untuk mengatur ulang kata sandi."),
        FAQItem("Bagaimana cara mengajukan izin?", "Buka menu 'Pengajuan Izin' di dashboard Anda, klik tombol 'Tambah Izin', isi detail alasan dan tanggal, sertakan foto bukti, lalu kirim. Admin atau Wali Kelas akan memverifikasi pengajuan Anda."),
        FAQItem("Kenapa saya tidak bisa melakukan absensi?", "Pastikan Anda berada di lingkungan sekolah dan waktu sholat sudah masuk. Jika masih gagal, pastikan perangkat Anda sudah terdaftar dan kamera berfungsi dengan baik."),
        FAQItem("Bagaimana cara mengganti NIS atau Email?", "Perubahan data profil utama seperti NIS dan Email hanya dapat dilakukan oleh Admin Sekolah melalui menu Manajemen Siswa."),
        FAQItem("Apa fungsi dari fitur Riwayat Presensi?", "Fitur ini memungkinkan Anda untuk melihat rekaman kehadiran Anda di masa lalu, termasuk status kehadiran (Hadir, Izin, Sakit, Alpa) dan waktu Anda melakukan scan."),
        FAQItem("Siapa yang harus saya hubungi untuk bantuan teknis?", "Jika Anda mengalami kendala aplikasi, silakan hubungi tim IT Support di ruang IT atau melalui kontak Admin yang tertera di papan pengumuman sekolah.")
    )

    private var filteredFaqs = allFaqs.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        initViews()
        setupRecyclerView()
        setupSearch()

        findViewById<View>(R.id.iconBack).setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearchFAQ)
        recyclerFAQ = findViewById(R.id.recyclerFAQ)
        tvEmpty = findViewById(R.id.tvEmptyFAQ)
    }

    private fun setupRecyclerView() {
        adapter = FAQAdapter(filteredFaqs)
        recyclerFAQ.layoutManager = LinearLayoutManager(this)
        recyclerFAQ.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterFAQ(s.toString())
            }
        })
    }

    private fun filterFAQ(query: String) {
        filteredFaqs.clear()
        if (query.isEmpty()) {
            filteredFaqs.addAll(allFaqs)
        } else {
            val lowerQuery = query.lowercase()
            allFaqs.forEach {
                if (it.question.lowercase().contains(lowerQuery) || it.answer.lowercase().contains(lowerQuery)) {
                    filteredFaqs.add(it)
                }
            }
        }
        
        tvEmpty.visibility = if (filteredFaqs.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
    }
}
