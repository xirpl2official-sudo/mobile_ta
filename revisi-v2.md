# 📋 Spesifikasi Tugas & Distribusi AI Sub-Agents

Dokumen ini berisi spesifikasi tugas terstruktur yang telah dipecah menjadi beberapa modul utama. Untuk memaksimalkan efisiensi, stabilitas sistem, dan paralelisme pengerjaan, proyek ini didistribusikan ke dalam **4 AI Sub-Agents** spesifik ditambah satu **Cross-Agent Task**.

---

## 🛠️ SUB-AGENT 1: UI/UX (Global & Styling Consistency)
**Fokus Utama:** Standardisasi komponen, perbaikan layout glitch, penanganan overflow, dan penyelarasan tema aplikasi.

### Task 1.1: Standardisasi Toolbar & Status Bar Global
*   **Deskripsi:** Menyelaraskan seluruh komponen toolbar di semua halaman aplikasi agar seragam dan konsisten.
*   **Perbaikan Spesifik:**
    *   **Halaman Tambah Guru:** Sesuaikan ukuran *toolbox panel* agar menempel pas dengan *statusbar*. Ukurannya harus sama persis dengan halaman lain.
    *   **Halaman Pengaturan Akun:** Perbaiki posisi *header* halaman agar tidak menabrak *statusbar*.
*   **Kriteria Selesai:** Toolbar di seluruh halaman memiliki `height`, `padding`, dan relasi posisi yang 100% identik terhadap *statusbar*.

### Task 1.2: Pembersihan Elemen UI Redundan
*   **Deskripsi:** Menghapus tombol-tombol atau menu aksi pada toolbox panel yang tidak digunakan untuk menjaga kebersihan layout.
*   **Komponen yang Dihapus:**
    *   **Halaman Kelola Siswa & Kelola Guru:** Hapus tombol menu titik tiga (`...`) pada *toolbox panel*.
    *   **Halaman Pengajuan Izin:** Hapus tombol menu `X` pada *toolbox panel* (Role Admin) dan tombol `X` di *header* aplikasi (Role Wali Kelas). Hapus juga tombol informasi (`i`) yang berada di pojok kiri bawah.
    *   **Halaman Pusat Bantuan:** Ubah ikon tombol centang (`✓`) menjadi tombol `X` untuk fungsi menutup halaman (*close*).

### Task 1.3: Perbaikan Layout Shift & Text Overflow
*   **Deskripsi:** Menyelesaikan masalah gangguan visual (*glitch*) saat terjadi interaksi pengguna.
*   **Perbaikan Spesifik:**
    *   **Tabel Kelola Siswa:** Perbaiki masalah *layout shift* dan gangguan *glitch* otomatis *scroll* ke bawah saat data pertama kali dimuat (*loading*) dan user melakukan *scroll* horizontal.
    *   **Header Tabel Kelola Siswa:** Amankan kolom "Keterangan" pada bagian *header* agar tidak mengalami *overflow* (teks turun ke bawah). Berikan ruang yang cukup atau gunakan properti CSS `white-space: nowrap;`.
    *   **Beranda Siswa:** Perbaiki teks *heading* pada komponen *Stat Card View* yang mengalami *overflow*.

### Task 1.4: Refactoring Tema Dark Mode
*   **Deskripsi:** Mengatur ulang skema dan palet warna pada mode gelap (*Dark Mode*).
*   **Spesifikasi Warna:** Gunakan warna gelap yang *friendly* dan memiliki tingkat kontras yang nyaman saat dipadukan dengan warna **Biru Utama** aplikasi. Hindari penggunaan warna hitam pekat (`#000000`); direkomendasikan menggunakan aksen *slate* atau *dark navy*.

---

## 💻 SUB-AGENT 2: Admin & Teacher Roles (Data, Filtering & Logic)
**Fokus Utama:** Manajemen data, logika pencarian/filtering, integrasi API, fungsionalitas tabel, serta pembatasan hak akses untuk data Admin, Wali Kelas, dan Guru.

### Task 2.1: Sinkronisasi Warna Card Jadwal Dhuha
*   **Deskripsi:** Menyesuaikan warna pada *Card Jadwal Jurusan Dhuha Hari Ini* di halaman Beranda agar sesuai (*match*) dengan warna khas masing-masing jurusan.
*   **Spesifikasi:** Logika ini mengadopsi dan meniru implementasi yang sudah berjalan pada *Project Desktop*.
*   **Cakupan Role:** Terapkan di halaman beranda **Admin**, **Wali Kelas**, dan **Guru**.

### Task 2.2: Penyempurnaan Halaman Kelola Siswa & Siswa Belum Terdaftar
*   **Fitur Filtering (Kelola Siswa):** Ubah logika pencarian pada *Search Bar* menjadi *Case-Insensitive* (tidak sensitif terhadap huruf besar/kecil) dan wajib memfilter data berdasarkan komponen **Nama**.
*   **Halaman Siswa Belum Terdaftar:**
    *   Ubah desain *header* tabel agar seragam dan konsisten dengan halaman kelola lainnya.
    *   Ubah proses *fetching* data siswa yang belum terdaftar agar langsung mengambil data real-time dari API.
    *   Perbaiki fungsi *filtering* yang rusak dengan mengimplementasikan fungsi *Lookup* dari API.

### Task 2.3: Perbaikan Komponen Kelola Kelas & Jurusan
*   **Koreksi Aset Gambar:** Pada Jurusan ANM, ubah pencarian file gambar dari yang sebelumnya menggunakan kata kunci `am` menjadi `an`.
*   **Filter & Dropdown:**
    *   Aktifkan dan perbaiki fitur *Filter Jurusan* yang saat ini tidak berfungsi.
    *   Perbaiki fungsi *dropdown* di setiap *card* Jurusan dan *card* Kelas agar berfungsi penuh untuk kembali ke tampilan awal (*collapse/back*).
*   **String Formatting:** Perbaiki penamaan kelas di dalam *card* jurusan. Ubah data *hardcoded* yang awalnya hanya menampilkan `11 1` menjadi dinamis menggunakan format: `11 {jurusan} 1` (Contoh hasil: `11 RPL 1`).

### Task 2.4: Perbaikan Kelola Guru & Jadwal Sholat
*   **Jadwal Sholat UI:** Tambahkan komponen *Loading Spinner* sebagai indikator visual saat aplikasi sedang melakukan proses *fetching* data tipe sholat.
*   **Tombol Aksi:** Tambahkan tombol **Hapus** di setiap *card* tipe sholat khusus untuk hak akses Role Admin (saat ini tombol tersebut hilang).

### Task 2.5: Perombakan Total Halaman Laporan & Pengajuan Izin
*   **Restrukturisasi Layout Laporan (Admin & Wali Kelas):**
    *   Pindahkan komponen *Filter Laporan* beserta *Data Absensi* ke posisi **paling atas** halaman.
    *   Ubah orientasi layout *Filter Laporan* dari Vertikal menjadi **Horizontal** (meniru struktur halaman Kelola Siswa).
    *   Pindahkan posisi tombol **Unduh** ke bagian atas komponen *Filter Laporan*.
    *   Ganti sistem komponen *pagination* lama (tombol "Sebelumnya/Selanjutnya") menggunakan **Numbering Pagination** (`1`, `2`, `3`, dst).
    *   *Bugfix:* Selesaikan masalah *crash* sistem pada halaman laporan di role Wali Kelas dan samakan strukturnya dengan versi Admin.
*   **Halaman Pengajuan Izin (Admin & Wali Kelas):** Lakukan standardisasi komponen *Combo Box* dan *Filtering* agar gaya (*style*) UI-nya konsisten dengan halaman lainnya.

### Task 2.6: Scoping Hak Akses Wali Kelas & Pembersihan Menu
*   **Modifikasi Hak Akses (Kelola Siswa):** Batasi fungsionalitas untuk Role Wali Kelas. Wali kelas hanya diizinkan untuk melihat, mencari, dan mengedit data siswa yang terdaftar di **kelas yang diwalikannya sendiri**.
*   **Presensi Sholat (Wali Kelas):** Hapus tombol *Input Izin*. Tambahkan komponen *Filtering* baru yang struktur dan fungsinya sama persis dengan versi Admin.
*   **Deprecate Feature:** Hapus secara permanen halaman **Kenaikan Kelas** beserta tautan (*link*) menunya di komponen *Sidebar* karena fitur ini sudah tidak digunakan lagi.

---

## 📱 SUB-AGENT 3: Student Role & QR Feature
**Fokus Utama:** Optimasi halaman khusus siswa, restyling komponen mobile, sinkronisasi API QR Code, dan kalkulasi waktu (countdown).

### Task 3.1: Optimalisasi Halaman QR Code (Admin & Siswa)
*   **Bypass Validasi Lokal:** Hapus sistem validasi lokal untuk penentuan tipe sholat. Logika penentuan tipe sholat sepenuhnya harus bergantung pada respons data dari API.
*   **QR Fetching:** Gambar/grafis QR Code wajib dimuat (*render*) langsung dari hasil *fetching* API atau menggunakan URL gambar langsung dari API.
*   **Fitur Tambahan:** Tambahkan mekanisme *Countdown Timer* (hitung mundur) untuk melakukan penyegaran (*refresh*) QR Code secara otomatis dalam waktu berkala.

### Task 3.2: Refactoring UI Halaman Siswa
*   **Pengajuan Perizinan Siswa:** Amankan bagian *header* aplikasi agar tidak menabrak *statusbar* dan ubah warna dasarnya menjadi warna **Biru Utama**.
*   **Pindai QR Code:**
    *   Samakan warna latar belakang (*background*) halaman dengan halaman utama lainnya demi konsistensi tema.
    *   Tambahkan komponen efek *Shadow* / Elevasi pada *card* putih di pinggiran kanvas QR untuk memberikan batas visual yang jelas dengan warna *background*.

### Task 3.3: Halaman Perangkat Siswa (Admin View)
*   **Deskripsi:** Tambahkan komponen *Filtering* di baris paling atas halaman yang menyediakan parameter data-data dasar yang diperlukan untuk mengelola perangkat siswa.

---

## 🔐 SUB-AGENT 4: Auth & Profile (Account & Sidebar Logic)
**Fokus Utama:** Manajemen state pengguna, alur navigasi profil, komponen akun, dan kondisional rendering berdasarkan hak akses (Role).

### Task 4.1: Pembenahan Navigasi Sidebar
*   **Deskripsi:** Mengubah perilaku klik pada komponen *Card Profil* yang terletak di dalam *Sidebar*.
*   **Alur Baru:** Saat *Card Profil* diklik, aplikasi harus langsung mengarahkan (*navigate*) pengguna ke halaman **Pengaturan Akun**.

### Task 4.2: Standardisasi Layout & Komponen Pengaturan Akun
*   **Tombol Kembali:** Pindahkan posisi tombol kembali (*Back Button*) ke area *header* sebelah **Kanan** (sebelumnya di sebelah kiri) untuk menyamakan standar *layouting* halaman lainnya.
*   **Modifikasi Foto Profil & Form Field:**
    *   Ubah label teks "Foto Profil" yang berada di samping *profil card* agar dinamis mengikuti nama user yang sedang login.
    *   Hapus kolom input / *text field* **Nama** dari form (karena nama tidak diizinkan diubah secara manual).
*   **Ubah Email Action:** Ubah elemen tautan teks (tag `<a>`) untuk fungsi ubah email menjadi sebuah komponen **Button** fisik yang seragam dengan tombol *Ubah Kata Sandi*.

### Task 4.3: Kondisional Rendering Data Profil berdasarkan Role
*   **Deskripsi:** Tampilkan data/field pada halaman Pengaturan Akun secara dinamis berdasarkan data akun pengguna yang sedang aktif dalam session:
    *   **Role Admin:** Menampilkan field `Nama` dan `Email`.
    *   **Role Wali Kelas:** Menampilkan field `NIP`, `Nama`, `Email`, dan `Kelas yang Diwalikan`.
    *   **Role Guru:** Menampilkan field data sesuai dengan spesifikasi data guru.
*   **Proteksi Fitur Ganti Perangkat:** Pastikan komponen *card* **"Ajukan Ganti Perangkat"** disembunyikan/dihapus untuk pengguna dengan role Admin, Wali Kelas, dan Guru. Fitur ganti perangkat ini **hanya boleh muncul** jika pengguna login menggunakan role **Siswa**.

---

## 📡 SISTEM NOTIFIKASI (Penugasan Bersama / Cross-Agent Task)
**Cakupan:** Sub-Agent 2 (Admin/Teacher) & Sub-Agent 3 (Student)

*   **Tugas Utama:** Melakukan migrasi dan perombakan pada sistem notifikasi aplikasi.
*   **Spesifikasi Teknis:** Ubah notifikasi yang awalnya menggunakan custom UI / toast internal aplikasi menjadi **Native System Notification** (menggunakan API notifikasi bawaan dari OS perangkat, baik Android Notification API maupun Desktop Notification API).
*   **Logika Pemicu:** Fungsi native ini harus dipicu secara real-time ketika terdapat *event* atau *trigger* data notifikasi baru yang masuk, baik pada panel admin maupun pada perangkat siswa.