package com.xirpl2.SASMobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xirpl2.SASMobile.adapter.FAQAdapter
import com.xirpl2.SASMobile.model.FAQItem

class FAQActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerFAQ: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvRoleLabel: TextView
    private lateinit var adapter: FAQAdapter

    private lateinit var allFaqs: List<FAQItem>
    private var filteredFaqs = mutableListOf<FAQItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        val topBar = findViewById<View>(R.id.topBarContent)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, view.paddingBottom)
            insets
        }

        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.blue_theme)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        initViews()
        loadRoleBasedFaqs()
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
        tvRoleLabel = findViewById(R.id.tvRoleLabel)
    }

    private fun loadRoleBasedFaqs() {
        val role = com.xirpl2.SASMobile.utils.SecurePreferences.getUserSession(this)
            .getString("user_role", "")?.lowercase()?.trim() ?: ""

        when {
            role.contains("admin") -> {
                tvRoleLabel.text = "Menu Bantuan Admin"
                allFaqs = getAdminFaqs()
            }
            role.contains("wali") -> {
                tvRoleLabel.text = "Menu Bantuan Wali Kelas"
                allFaqs = getWaliKelasFaqs()
            }
            role == "guru" -> {
                tvRoleLabel.text = "Menu Bantuan Guru"
                allFaqs = getGuruFaqs()
            }
            else -> {
                tvRoleLabel.text = "Menu Bantuan Siswa"
                allFaqs = getSiswaFaqs()
            }
        }

        filteredFaqs = allFaqs.toMutableList()
    }

    private fun getAdminFaqs() = listOf(
        FAQItem(
            "Bagaimana cara menambah siswa baru?",
            "Buka menu 'Kelola Siswa', lalu klik tombol 'Tambah'. Isi data NIS, nama, kelas, jurusan, dan jenis kelamin, lalu klik 'Kirim'. Siswa akan otomatis terdaftar di sistem."
        ),
        FAQItem(
            "Bagaimana cara mengelola jadwal sholat?",
            "Buka menu 'Jadwal Sholat'. Di sana Anda bisa menambah, mengedit, atau menghapus jadwal sholat (Duha, Zuhur, Jumat). Setiap jadwal bisa diatur per jurusan atau umum."
        ),
        FAQItem(
            "Bagaimana cara mengatur wali kelas?",
            "Buka menu 'Kelola Kelas', lalu cari kelas yang ingin diatur. Klik tombol 'Atur Wali' pada kelas tersebut, lalu pilih guru yang akan menjadi wali kelas."
        ),
        FAQItem(
            "Bagaimana cara melihat laporan kehadiran?",
            "Buka menu 'Laporan'. Anda bisa memfilter data berdasarkan tanggal, kelas, jurusan, dan jenis sholat. Laporan bisa diunduh dalam format Excel, PDF, atau CSV."
        ),
        FAQItem(
            "Bagaimana cara generate QR Code untuk presensi?",
            "Buka menu 'QR Code'. Pilih jadwal sholat yang ingin di-generate QR Code-nya. QR Code akan ditampilkan dan bisa diperlihatkan kepada siswa untuk scan."
        ),
        FAQItem(
            "Bagaimana cara mengelola pengajuan izin?",
            "Buka menu 'Pengajuan Izin'. Anda akan melihat daftar pengajuan yang menunggu verifikasi. Klik pada pengajuan untuk melihat detail, lalu terima atau tolak dengan memberikan catatan."
        ),
        FAQItem(
            "Bagaimana cara mengimpor data siswa dari file?",
            "Buka menu 'Kelola Siswa', lalu klik tombol 'Impor'. Pilih file CSV yang sudah sesuai format template, lalu unggah. Sistem akan memproses dan menambahkan data siswa secara massal."
        ),
        FAQItem(
            "Bagaimana cara mengunduh data siswa?",
            "Buka menu 'Kelola Siswa', lalu klik tombol 'Unduh'. Data akan diekspor ke format CSV sesuai filter yang aktif. Anda bisa memilih filter kelas, jurusan, atau gender sebelum mengunduh."
        ),
        FAQItem(
            "Apa itu 'Siswa Belum Terdaftar'?",
            "Menu 'Siswa Belum Terdaftar' menampilkan siswa yang sudah melakukan scan QR tetapi NIS-nya belum terdaftar di sistem. Anda bisa langsung mendaftarkan siswa dari menu ini."
        ),
        FAQItem(
            "Siapa yang harus saya hubungi untuk bantuan teknis?",
            "Jika mengalami kendala aplikasi, silakan hubungi layanan informasi resmi SMK Negeri 2 Singosari melalui kontak yang tercantum di bagian bawah halaman ini."
        )
    )

    private fun getWaliKelasFaqs() = listOf(
        FAQItem(
            "Bagaimana cara melihat data siswa di kelas saya?",
            "Buka menu 'Kelola Siswa'. Karena Anda adalah wali kelas, data akan otomatis difilter sesuai kelas yang Anda ampu. Anda bisa melihat daftar siswa beserta detailnya."
        ),
        FAQItem(
            "Bagaimana cara melihat kehadiran siswa di kelas saya?",
            "Buka menu 'Laporan'. Karena Anda adalah wali kelas, laporan akan otomatis menampilkan data kehadiran siswa di kelas Anda. Gunakan filter tanggal untuk periode tertentu."
        ),
        FAQItem(
            "Bagaimana cara mengajukan kenaikan kelas?",
            "Buka menu 'Kenaikan Kelas'. Pilih siswa yang akan dinaikkan kelasnya, tentukan kelas tujuan, lalu konfirmasi. Data siswa akan otomatis diperbarui."
        ),
        FAQItem(
            "Bagaimana cara memverifikasi pengajuan izin siswa?",
            "Buka menu 'Pengajuan Izin'. Pengajuan dari siswa di kelas Anda akan ditandai secara khusus. Klik pada pengajuan untuk melihat detail, lalu terima atau tolak."
        ),
        FAQItem(
            "Bagaimana cara melihat riwayat presensi siswa?",
            "Buka menu 'Laporan', lalu pilih siswa tertentu atau gunakan filter untuk melihat riwayat kehadiran per siswa selama periode waktu tertentu."
        ),
        FAQItem(
            "Bagaimana cara mencetak laporan kelas?",
            "Buka menu 'Laporan', atur filter sesuai kebutuhan, lalu gunakan fitur cetak atau unduh dalam format Excel/PDF untuk keperluan administrasi."
        ),
        FAQItem(
            "Siapa yang harus saya hubungi untuk bantuan teknis?",
            "Jika mengalami kendala aplikasi, silakan hubungi layanan informasi resmi SMK Negeri 2 Singosari melalui kontak yang tercantum di bagian bawah halaman ini."
        )
    )

    private fun getGuruFaqs() = listOf(
        FAQItem(
            "Bagaimana cara melihat jadwal sholat?",
            "Buka menu 'Jadwal Sholat' untuk melihat jadwal sholat lengkap beserta waktu dan jurusan yang berlaku."
        ),
        FAQItem(
            "Bagaimana cara memverifikasi pengajuan izin siswa?",
            "Buka menu 'Pengajuan Izin'. Pengajuan dari siswa yang Anda tangani akan ditampilkan. Klik pada pengajuan untuk melihat detail, lalu terima atau tolak."
        ),
        FAQItem(
            "Bagaimana cara melihat laporan kehadiran?",
            "Buka menu 'Laporan'. Anda bisa memfilter data berdasarkan tanggal dan melihat rekap kehadiran siswa."
        ),
        FAQItem(
            "Bagaimana cara mengunduh laporan?",
            "Di menu 'Laporan', gunakan tombol 'Unduh' atau 'Export' untuk mengunduh laporan dalam format Excel, PDF, atau CSV."
        ),
        FAQItem(
            "Siapa yang harus saya hubungi untuk bantuan teknis?",
            "Jika mengalami kendala aplikasi, silakan hubungi layanan informasi resmi SMK Negeri 2 Singosari melalui kontak yang tercantum di bagian bawah halaman ini."
        )
    )

    private fun getSiswaFaqs() = listOf(
        FAQItem(
            "Bagaimana cara melakukan absensi?",
            "Anda dapat melakukan absensi dengan memindai kode QR yang ditampilkan oleh Admin menggunakan fitur 'Scan QR' di dashboard siswa."
        ),
        FAQItem(
            "Apa yang harus saya lakukan jika lupa kata sandi?",
            "Klik 'Lupa kata sandi?' di halaman login. Masukkan NIS dan email Anda untuk menerima kode OTP, lalu ikuti langkah-langkah untuk mengatur ulang kata sandi."
        ),
        FAQItem(
            "Bagaimana cara mengajukan izin?",
            "Buka menu 'Pengajuan Izin' di dashboard Anda, klik tombol 'Tambah Izin', isi detail alasan dan tanggal, sertakan foto bukti, lalu kirim. Admin atau Wali Kelas akan memverifikasi pengajuan Anda."
        ),
        FAQItem(
            "Kenapa saya tidak bisa melakukan absensi?",
            "Pastikan Anda berada di lingkungan sekolah dan waktu sholat sudah masuk. Jika masih gagal, pastikan perangkat Anda sudah terdaftar dan kamera berfungsi dengan baik."
        ),
        FAQItem(
            "Bagaimana cara mengganti NIS atau Email?",
            "Perubahan data profil utama seperti NIS dan Email hanya dapat dilakukan oleh Admin Sekolah melalui menu Manajemen Siswa."
        ),
        FAQItem(
            "Apa fungsi dari fitur Riwayat Presensi?",
            "Fitur ini memungkinkan Anda untuk melihat rekaman kehadiran Anda di masa lalu, termasuk status kehadiran (Hadir, Izin, Sakit, Alpa) dan waktu Anda melakukan scan."
        ),
        FAQItem(
            "Siapa yang harus saya hubungi untuk bantuan teknis?",
            "Jika Anda mengalami kendala aplikasi, silakan hubungi layanan informasi resmi SMK Negeri 2 Singosari melalui kontak yang tercantum di bagian bawah halaman ini."
        )
    )

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
