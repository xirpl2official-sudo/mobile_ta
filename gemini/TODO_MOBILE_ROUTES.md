# To-Do List & Bug Report: Implementasi Route Mobile TA

File ini berisi daftar route backend (`absensholat-api`) yang **belum** diimplementasikan ke dalam aplikasi mobile (`mobile_ta`), serta beberapa **bug/error/ketidaksesuaian** yang ditemukan pada `ApiService.kt`.

## 🐛 Bug / Ketidaksesuaian Endpoint di `mobile_ta`

Berdasarkan pengecekan di `app/src/main/java/.../api/ApiService.kt`, terdapat beberapa kesalahan penulisan endpoint:

1. **Endpoint `getDeviceAuthInfo` Salah**
   - 🔴 **Di Mobile (`ApiService.kt`):** `@GET("v2/device-auth/me")`
   - 🟢 **Di Backend (`Routes.go`):** `GET /api/v2/device-auth/info`
   - *Solusi:* Ubah `me` menjadi `info` di file `ApiService.kt`.

2. **Endpoint `submitAbsensi` Tidak Valid**
   - 🔴 **Di Mobile (`ApiService.kt`):** `@POST("v2/attendance")`
   - 🟢 **Di Backend (`Routes.go`):** Tidak ada route `POST /api/v2/attendance`. Absensi manual seharusnya menggunakan `POST /api/v2/students/{nis}/attendances`, atau menggunakan verifikasi QR/Barcode.
   - *Solusi:* Sesuaikan logika pengiriman absensi ke endpoint yang benar sesuai di backend.

3. **Duplikasi Deklarasi `getStatistikAbsensi` dan `getStatistics`**
   - 🔴 **Di Mobile (`ApiService.kt`):** Terdapat dua method yang mengarah ke endpoint yang persis sama `@GET("v2/analytics/attendance")` tapi memiliki tipe kembalian yang berbeda (`StatistikData` dan `StatisticsResponse`). Hal ini bisa menyebabkan error di Retrofit.
   - *Solusi:* Gabungkan atau hapus salah satu method yang tidak digunakan.

---

## 📝 To-Do List: Route yang Belum Diimplementasikan di Mobile

Berikut adalah daftar endpoint API yang tersedia di backend tetapi belum ada di `ApiService.kt` pada mobile app:

### 1. Authentication (`/auth`)
- [x] `POST /api/v2/auth/tokens/refresh` - Untuk melakukan refresh access token.
- [x] `DELETE /api/v2/auth/sessions/current` - Untuk proses logout session.
- [x] `POST /api/v2/auth/verify-account` - Untuk verifikasi akun.

### 2. Notifications (`/notifications`)
- [x] `PATCH /api/v2/notifications/:id/read` - Tandai notifikasi spesifik sebagai dibaca.
- [x] `DELETE /api/v2/notifications/:id` - Hapus notifikasi.
- [x] `POST /api/v2/notifications/bulk-read` - Tandai semua/banyak notifikasi sebagai dibaca.

### 3. Analytics & Dashboard (`/analytics`)
- [x] `GET /api/v2/analytics/charts` - Data untuk chart analitik (Admin/Guru).
- [x] `GET /api/v2/analytics/pending-attendance` - Cek absensi yang masih pending.

### 4. Attendance & QR Code / Barcode (`/attendance`)
- [x] `POST /api/v2/attendance/auto-mark` - Trigger absen otomatis (Admin).
- [x] `GET /api/v2/attendance/qr-codes/current/image` - Mengambil gambar QR Code.
- [x] `GET /api/v2/attendance/code/generate` - Generate kode absensi.
- [x] `POST /api/v2/attendance/code/verify` - Verifikasi kode absensi.

### 5. Pengajuan Izin (`/pengajuan-izin`)
*(Penting untuk fitur siswa)*
- [x] `POST /api/v2/pengajuan-izin` - Buat pengajuan izin baru (Siswa).
- [x] `GET /api/v2/pengajuan-izin` - Lihat daftar pengajuan izin.
- [x] `GET /api/v2/pengajuan-izin/:id` - Detail pengajuan izin.
- [x] `GET /api/v2/pengajuan-izin/:id/bukti` - Lihat bukti foto pengajuan.
- [x] `PATCH /api/v2/pengajuan-izin/:id/status` - Update status izin (Admin/Guru).
- [x] `DELETE /api/v2/pengajuan-izin/:id` - Hapus pengajuan izin.

### 6. Profil dan Device Auth (User Side)
- [x] `GET /api/v2/profile/devices` - Lihat device yang terhubung ke akun.
- [x] `DELETE /api/v2/profile/devices` - Unbind device sendiri.
- [x] `POST /api/v2/device/change-request` - Pengajuan ganti perangkat (Siswa/Guru).

### 7. Students (Siswa)
- [x] `GET /api/v2/students/filters` - Filter data siswa.
- [x] `GET /api/v2/students/unregistered` - Siswa yang belum register akun.
- [x] `GET /api/v2/students/:nis` - Get detail siswa by NIS.
- [x] `PATCH /api/v2/students/:nis/status` - Update status siswa.
- [x] `POST /api/v2/students/notify-wali-kelas` - Notifikasi wali kelas.

### 8. Prayer Schedules (Jadwal Sholat & Dhuha)
- [x] `GET /api/v2/prayer-schedules/today` - Jadwal sholat hari ini.
- [x] `GET /api/v2/prayer-schedules/closest` - Jadwal sholat terdekat.
- [x] `GET /api/v2/prayer-schedules/dhuha/turns` - Jadwal/giliran dhuha hari ini.

### 9. Lookups / General Data
- [x] `GET /api/v2/kelas` - List data kelas.
- [x] `GET /api/v2/jurusan` - List data jurusan.
- [x] `GET /api/v2/lookup/staff-guru` - List lookup data guru.

---

### 10. Fitur Admin Management (Opsional jika Mobile juga dipakai Admin)
*Jika aplikasi mobile ditujukan hanya untuk siswa/guru, daftar ini mungkin tidak perlu dibuat di mobile app, dan cukup di web dashboard admin.*
- [x] **Data Retention:** `GET/POST/DELETE` pada `/data-retention/backups`
- [x] **Reports (CSV/Excel):** `/reports/attendances/*`, `/reports/summary/*`
- [x] **Student Control:** `/admin/student-control/overview`, `transitions`, `bulk-progression`, dll.
- [x] **Management Kelas:** `/admin/management/kelas`
- [x] **Promotion Config:** `/admin/promotion/*`
- [x] **Device Management Admin:** `/admin/device-management/*`
- [x] **Wali Kelas:** `/class-teachers/*`
- [x] **Jenis & Waktu Sholat:** `/prayer-types/*` & `/prayer-times/*`
- [x] **Dhuha Groups:** `/dhuha-groups/*`

---
*Catatan: Pastikan untuk memprioritaskan perbaikan endpoint yang masuk kategori Bug terlebih dahulu sebelum menambahkan endpoint baru.*
