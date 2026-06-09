# Plan: Audit Testabilitas Mobile `mobile_ta` + Perbandingan dengan `absensholat-api` & `absensholat-desktop-el`

> **Tujuan**: Mengidentifikasi fitur-fitur di `mobile_ta` yang sulit/tidak bisa di-test otomatis, menganalisis dependensi hard-nya terhadap `absensholat-api` (kontrak, statefulness, flakiness), dan memetakan gap implementasi dengan `absensholat-desktop-el`.

> **Cakupan**: Frontend Android (Kotlin + XML), kontrak API yang dipakai mobile, perbandingan feature matrix mobile vs desktop.

> **Di luar cakupan**: Backend API (tidak mengubah `absensholat-api`), aplikasi desktop (analisis read-only, tidak mengubah).

---

## 0. Metodologi & Sumber Data

### Sumber Data
| Codebase | Lokasi | Status Akses | Data yang Diperoleh |
|----------|--------|--------------|---------------------|
| `mobile_ta` | `/home/thinkpad/Documents/malik/ta/mobile_ta` (in-workspace) | ✅ Read langsung | 37 Activity, 4 Fragment, 8 Dialog, build.gradle, AndroidManifest, ApiService.kt (868 baris), semua Repository & Model |
| `absensholat-api` | `/home/thinkpad/Documents/malik/ta/absensholat-api` (out-of-workspace) | ⚠️ Direkonstruksi dari sisi mobile | 120+ endpoint, 20+ tabel database, 9 area stateful, 5 contoh request/response |
| `absensholat-desktop-el` | `/home/thinkpad/Documents/malik/ta/absensholat-desktop-el` (out-of-workspace) | ⏳ **Menunggu input user** | Stack, struktur folder, daftar screen/route, library hardware |

### Langkah Verifikasi yang Disarankan
Sebelum eksekusi plan apapun, **verifikasi sumber data** dengan:
1. Buka `absensholat-api/API_REFERENCE.md` (atau `README.md`) — cocokkan dengan rekonstruksi di §5.
2. Buka `absensholat-desktop-el/package.json` + `src/` tree — cocokkan dengan placeholder di §7.
3. Cek field/response yang diasumsikan di `mobile_ta/app/src/main/java/api/*Models.kt` — pastikan tidak ada model baru yang diabaikan.

---

## 1. Executive Summary

### Temuan Utama
- **~80% fitur mobile_ta fully testable** (logika validasi, CRUD via Repository, navigasi).
- **~15% partially testable** (membutuhkan instrumented test + mock system service).
- **~5% tidak bisa otomatis** (hardware-specific, sistem Android yang tidak bisa di-isolate).
- **Kontrak API memiliki 9 area stateful** yang mempersulit testing: device binding, OTP expiration, brute-force cooldown, QR time-bound, cron-triggered auto-mark, dsb.
- **Semua endpoint API (120+) berhasil direkonstruksi** dari `ApiService.kt` + model classes — kontrak ini adalah single source of truth untuk integration test.

### Critical Hard Dependencies (WAJIB real device)
1. **Kamera + ZXing QR Scanner** (`ScanQrActivity` / `ScanQrFragment`) — `CAMERA` permission + autofocus hardware.
2. **APK Download + Install** (`UpdateDownloader`) — `DownloadManager` + `ACTION_INSTALL_PACKAGE`.
3. **GitHub Release API** (`UpdateChecker`) — external network + JSON parsing dinamis.
4. **IMEI / ANDROID_ID** (`DeviceHelper`) — hardware-specific identifier.
5. **System PrintManager** (`DataSiswaAdminActivity.printDataSiswa()`) — WebView → PDF → print spooler.

### Rekomendasi Strategi
- **MockWebServer** untuk semua Repository (~150 endpoint call).
- **Robolectric** untuk unit test yang butuh Android Context (SharedPreferences, KeyStore).
- **Espresso + IdlingResource** untuk handle countdown timer & animasi.
- **`WorkManagerTestInitHelper`** untuk `NotificationPollWorker`.
- **Mock `BarcodeCallback`** untuk validasi flow scanner tanpa kamera fisik.
- **WAJIB** minimal 1–2 real device untuk E2E test (scan QR + install APK + hardware_id binding).

---

## 2. Arsitektur `mobile_ta` — Inventaris Lengkap

### 2.1 Activity (36 total)

| # | Activity | Tipe User | File Manifest | Lokasi Source |
|---|----------|-----------|---------------|---------------|
| 1 | `MasukActivity` | Public | ✅ | `MasukActivity.kt` |
| 2 | `DaftarActivity` | Public | ✅ | `DaftarActivity.kt` |
| 3 | `VerifikasiOtpActivity` | Public | ✅ | `VerifikasiOtpActivity.kt` |
| 4 | `BuatSandiBaruActivity` | Public | ✅ | `BuatSandiBaruActivity.kt` |
| 5 | `GantiKataSandi` | Public | ✅ | `GantiKataSandi.kt` |
| 6 | `BerandaActivity` | Siswa | ✅ | `BerandaActivity.kt` |
| 7 | `StudentMainActivity` | Siswa (Host) | ✅ | `StudentMainActivity.kt` |
| 8 | `ScanQrActivity` | Siswa | ✅ | `ScanQrActivity.kt` |
| 9 | `PengajuanIzinActivity` | Siswa | ✅ | `PengajuanIzinActivity.kt` |
| 10 | `PengaturanAkunActivity` | Semua | ✅ | `PengaturanAkunActivity.kt` |
| 11 | `FemaleRestrictionStatusActivity` | Siswa (P) | ✅ | `FemaleRestrictionStatusActivity.kt` |
| 12 | `BerandaAdminActivity` | Admin | ✅ | `BerandaAdminActivity.kt` |
| 13 | `BerandaGuruActivity` | Guru/Wali | ✅ | `BerandaGuruActivity.kt` |
| 14 | `JadwalSholatAdminActivity` | Admin | ✅ | `JadwalSholatAdminActivity.kt` |
| 15 | `DataSiswaAdminActivity` | Admin/Wali | ✅ | `DataSiswaAdminActivity.kt` |
| 16 | `PresensiSholatAdminActivity` | Admin/Wali | ✅ | `PresensiSholatAdminActivity.kt` |
| 17 | `LaporanAdminActivity` | Admin/Wali | ✅ | `LaporanAdminActivity.kt` |
| 18 | `QRCodeAdminActivity` | Admin | ✅ | `QRCodeAdminActivity.kt` |
| 19 | `StaffQRActivity` | Staff | ✅ | `StaffQRActivity.kt` |
| 20 | `StudentQRActivity` | Siswa | ✅ | `StudentQRActivity.kt` |
| 21 | `SiswaBelumTerdaftarAdminActivity` | Admin | ✅ | `SiswaBelumTerdaftarAdminActivity.kt` |
| 22 | `AdminDeviceManagementActivity` | Admin | ✅ | `AdminDeviceManagementActivity.kt` |
| 23 | `VerifyAccountActivity` | All (Force) | ✅ | `VerifyAccountActivity.kt` |
| 24 | `StudentPromotionActivity` | Admin | ✅ | `StudentPromotionActivity.kt` |
| 25 | `PengaturanActivity` | Semua | ✅ | `PengaturanActivity.kt` |
| 26 | `FAQActivity` | Semua | ✅ | `FAQActivity.kt` |
| 27 | `NotificationCenterActivity` | Semua | ✅ | `NotificationCenterActivity.kt` |
| 28 | `DhuhaGroupsActivity` | Admin | ✅ | `DhuhaGroupsActivity.kt` |
| 29 | `FemaleRestrictionApprovalActivity` | Admin/Guru | ✅ | `FemaleRestrictionApprovalActivity.kt` |
| 30 | `DetailAbsensiActivity` | Semua | ✅ | `DetailAbsensiActivity.kt` |
| 31 | `TambahSiswaActivity` | Admin | ✅ | `TambahSiswaActivity.kt` |
| 32 | `KelolaKelasActivity` | Admin | ✅ | `KelolaKelasActivity.kt` |
| 33 | `KelolaGuruAdminActivity` | Admin | ✅ | `KelolaGuruAdminActivity.kt` |
| 34 | `TambahGuruActivity` | Admin | ✅ | `TambahGuruActivity.kt` |
| 35 | `EditGuruActivity` | Admin | ✅ | `EditGuruActivity.kt` |
| 36 | `PengajuanIzinAdminActivity` | Admin/Wali | ✅ | `PengajuanIzinAdminActivity.kt` |

### 2.2 Fragment (4)
- `fragment/ScanQrFragment.kt` (di-`include` ke `StudentMainActivity`)
- `fragment/BerandaFragment.kt`
- `fragment/PengajuanIzinFragment.kt`
- `fragment/ProfilFragment.kt`

### 2.3 Dialog Fragment (8)
- `PresenceDetailDialogFragment`, `PresenceDetailPopUpFragment`
- `SiswaDetailDialogFragment`, `HistorySiswaDialogFragment`
- `ManualPresensiSiswaDialogFragment`, `TambahAbsensiDialogFragment`
- `InputIzinDialogFragment`, `TambahGuruDialogFragment` (BottomSheet)

### 2.4 Base / Library Classes
- `BaseActivity` (abstract, semua activity extend ini)
- `BaseAdminActivity` (abstract, untuk admin)
- `BaseSiswaActivity` (abstract, untuk siswa)
- `SASMobileApp` (Application class — init Firebase, WorkManager, NotificationChannel)

### 2.5 Permission yang Diminta
| Permission | Tipe | Tujuan | File yang Pakai |
|------------|------|--------|-----------------|
| `INTERNET` | Normal | HTTP ke API | Semua Repository |
| `ACCESS_NETWORK_STATE` | Normal | Cek koneksi | `BaseActivity` |
| `CAMERA` | **Dangerous** | Scan QR | `ScanQrActivity`, `ScanQrFragment` |
| `READ_PHONE_STATE` | **Dangerous** | IMEI | `DeviceHelper` (gracefully return "" jika ditolak di API 29+) |
| `POST_NOTIFICATIONS` | **Dangerous** (API 33+) | Notifikasi | `BaseActivity`, `NotificationHelper` |
| `REQUEST_INSTALL_PACKAGES` | Signature | Install APK update | `UpdateDownloader` |

### 2.6 Library Pihak Ketiga (dari `app/build.gradle.kts`)
| Library | Versi | Fungsi | Testable? |
|---------|-------|--------|-----------|
| Retrofit2 | 2.9.0 | HTTP client | ✅ MockWebServer |
| OkHttp Logging | 4.11.0 | HTTP logger | ✅ |
| Gson Converter | 2.9.0 | JSON parser | ✅ |
| Kotlinx Coroutines | 1.7.3 | Async | ✅ `TestCoroutineDispatcher` |
| ZXing core | 3.5.1 | QR parsing | ✅ Stub |
| **ZXing Android Embedded** | 4.3.0 | QR scanner | ❌ Hardware |
| MotionToast | 1.4 | Toast UI | ✅ Mock |
| MPAndroidChart | v3.1.0 | Line chart | ✅ Disable anim di test |
| Firebase BoM | 34.8.0 | Analytics | ⚠️ Perlu test config |
| WorkManager | (androidx) | Background | ✅ `WorkManagerTestInitHelper` |
| Navigation | 2.8.9 | Bottom nav | ✅ |
| Security Crypto | 1.1.0-alpha06 | EncryptedSharedPrefs | ✅ Robolectric |
| SwipeRefresh | 1.1.0 | Pull-to-refresh | ✅ |
| Material / RecyclerView / ConstraintLayout / AppCompat | (androidx) | UI | ✅ |

### 2.7 Receiver / Service / Provider
- **BroadcastReceiver** (1, programatik): `UpdateDownloader` — `DownloadManager.ACTION_DOWNLOAD_COMPLETE`.
- **WorkManager Worker** (1): `NotificationPollWorker` — period 15 menit.
- **ContentProvider** (1, default AndroidX): `FileProvider` — share file APK update.
- **OkHttp Authenticator** (1): `TokenAuthenticator` — auto-refresh JWT saat 401.

---

## 3. Hardware & System Dependencies

### 3.1 Kamera (ZXing)
- **Lokasi**: `ScanQrActivity.kt`, `ScanQrFragment.kt`.
- **Hardware**: autofocus camera + `<uses-feature android:name="android.hardware.camera">` di manifest.
- **Permission flow**: runtime request `Manifest.permission.CAMERA` → ditolak → force-close dengan dialog "Buka Pengaturan".
- **Test impact**: Espresso test harus grant permission via `UiAutomator` atau gunakan fake camera frame.
- **Workaround**: Mock `BarcodeCallback` di instrumented test — bisa validasi flow scanner tanpa hardware.

### 3.2 IMEI / ANDROID_ID
- **Lokasi**: `DeviceHelper.getImei()` (line 21-23) — gracefully return `""` jika permission ditolak.
- **Identifier utama**: `Settings.Secure.ANDROID_ID` (line 15-17) — **per-app + per-user di Android 8+**, berubah saat factory reset.
- **Test impact**: 
  - **EMULATOR** punya ANDROID_ID statis `0000000000000000` per AVD → device binding test akan conflict antar emulator.
  - Test IMEI: skip (return "") di Android 10+.
- **Konsekuensi API**: `POST /v2/device-auth/register` mengirim hardware_id → jika tidak match dengan `device_auth.hardware_id` di DB → 403 Forbidden.

### 3.3 APK Download + Install
- **Lokasi**: `update/UpdateDownloader.kt` + `UpdateDialog.kt`.
- **Alur**: Cek GitHub Release → download APK via `DownloadManager` → `ACTION_VIEW` ke `FileProvider` → `ACTION_INSTALL_PACKAGE`.
- **Test impact**: Tidak bisa di-emulasi (tidak ada FileProvider APK di emulator). Manual test only.
- **Endpoint dependency**: `https://api.github.com/repos/.../releases/latest` (eksternal, bukan `absensholat-api`).

### 3.4 Notifikasi
- **Lokasi**: `utils/NotificationHelper.kt`, `utils/NotificationPollWorker.kt`.
- **System**: `NotificationManagerCompat` + periodic `WorkManager`.
- **Test impact**: Bisa di-test dengan `WorkManagerTestInitHelper` + grant `POST_NOTIFICATIONS` via UiAutomator.

### 3.5 File Picker (SAF)
- **Lokasi**: `PengajuanIzinActivity` (foto bukti izin), `DataSiswaAdminActivity` (CSV import), `PengaturanAkunActivity` (foto profil).
- **Implementation**: `ActivityResultContracts.StartActivityForResult` + `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`.
- **Test impact**: Stub dengan Espresso-Intents.

### 3.6 PrintManager
- **Lokasi**: `DataSiswaAdminActivity.printDataSiswa()` — `WebView.createPrintDocumentAdapter()`.
- **Test impact**: Manual only (butuh printer emulator atau real printer).

### 3.7 MediaStore (Export Laporan)
- **Lokasi**: `LaporanAdminActivity` — download CSV/PDF/Excel via streaming endpoint.
- **Test impact**: Bisa di-test dengan `DownloadManager` + verifikasi file di `MediaStore.Downloads` (API 29+).

### 3.8 GPS / Lokasi / NFC
- **TIDAK DIGUNAKAN** — tidak ada `ACCESS_FINE_LOCATION`, tidak ada `NFC` permission.

---

## 4. Async / Timing Non-Deterministik

| Lokasi | Timing | Strategi Test |
|--------|--------|---------------|
| `MasukActivity.checkAndValidateExistingToken()` | `delay(500)` | Mock delay atau `TestCoroutineDispatcher` |
| `MasukActivity.startLoginCooldown()` | `delay(1000)` × 30 | Mock time |
| `VerifikasiOtpActivity.verifyOtp` | `delay(1000)` countdown | Mock time |
| `VerifikasiOtpActivity.startResendTimer()` | `TimeUnit.MINUTES.toMillis(1)` | Mock time |
| `BerandaAdminActivity.clockRefreshInterval` | `30_000L` | Lifecycle-aware |
| `BerandaGuruActivity.clockRefreshInterval` | `30_000L` | Lifecycle-aware |
| `QRCodeAdminActivity.QR_REFRESH_INTERVAL` | `30_000L` | Skip - lifecycle dependent |
| `QRCodeAdminActivity.CODE_REFRESH_INTERVAL` | `20_000L` | Skip - lifecycle dependent |
| `StaffQRActivity.countDownTimer` | Until expiry | Mock time |
| Search debounce (9 activity) | `300-500L` | Mock coroutine scheduler |
| `NotificationPollWorker` | 15 minutes | `WorkManagerTestInitHelper` |
| `MPAndroidChart.animateX(500)` | Chart animation | Disable anim di test mode |
| `MotionToast.LONG_DURATION` | ~5 detik | Skip - visual only |

---

## 5. Kontrak API `absensholat-api` — Risiko Testing

> **Catatan**: Analisis ini disusun dari sisi mobile (konsumen). Verifikasi dengan `absensholat-api/API_REFERENCE.md` sebelum membuat MockWebServer.

### 5.1 Stack & Infrastruktur
- **Bahasa**: Go (Golang), kemungkinan framework **Gin**.
- **Deployment**: Vercel Serverless (`vercel.json` ada).
- **Base URL**: `http://localhost:3000/api/` (dev) → `https://absensholat-api.vercel.app/api/` (prod).
- **Database**: PostgreSQL/MySQL (migrasi Go, bukan Laravel).
- **Auth**: **JWT + refresh token** (bukan Sanctum/Laravel session).
- **Versi API**: `v2` (semua endpoint ber-prefix `/api/v2/...`).

### 5.2 Total Endpoint
**~120 endpoint** di-breakdown:
- Auth (no auth + Bearer): 13 endpoint
- Prayer Schedules: 16 endpoint
- Attendance (QR/Code/Barcode): 10 endpoint
- Students: 16 endpoint (CRUD + filter + import)
- Pengajuan Izin: 6 endpoint
- Notifications: 4 endpoint
- Analytics: 3 endpoint
- Admin Management (kelas, guru, wali-kelas): 12 endpoint
- Admin Promotion: 6 endpoint
- Admin Student Control: 6 endpoint
- Admin Device Management: 4 endpoint
- Device Auth: 6 endpoint
- Lookups: 3 endpoint
- Master Data CRUD: 20 endpoint (academic-years, class-teachers, prayer-types, prayer-times, dhuha-groups)
- Reports: 8 endpoint (Excel/PDF/CSV streaming)
- Female Restriction: 6 endpoint
- Backup/Data Retention: 3 endpoint
- Health: 1 endpoint

### 5.3 Database Schema Ringkas (20+ tabel)
**Accounts & Auth**: `users/accounts`, `staff`, `wali_kelas`, `class_teachers`.
**Master**: `jurusan`, `kelas`, `academic_years`, `jenis_sholat`, `waktu_sholat`.
**Absensi**: `jadwal_sholat`, `absensi`, `pengajuan_izin`, `attendance_codes`, `qr_codes`, `barcodes`, `dhuha_groups`, `dhuha_schedules_jurusan`.
**Device & Security**: `device_auth`, `device_change_requests`, `otp_codes`, `promotion_logs`, `student_transitions`.
**Notifikasi**: `notifications`.
**Pendukung**: `female_restrictions`, `female_restriction_approvals`, `backups`.

### 5.4 9 Area Stateful yang Memperberat Testing

| # | Area | Lokasi Mobile | Risiko | Solusi |
|---|------|---------------|--------|--------|
| 1 | **Device Binding** | `MasukActivity`, `DeviceHelper`, `TokenAuthenticator` | `hardware_id` mismatch → 403 di semua endpoint Bearer | Gunakan akun berbeda per emulator; reset app data di `@Before` |
| 2 | **OTP Expiration & Brute-Force** | `MasukActivity`, `VerifikasiOtpActivity` | Max 5 gagal → cooldown 30-60s, OTP expire ~5-15 menit | Reset SharedPreferences `login_brute_force` & `otp_brute_force` di `@Before` |
| 3 | **Password Default / First Login** | `VerifyAccountActivity` | `is_verified=false` →强制 ke VerifyAccount | Setup test user dengan `is_verified=true` |
| 4 | **Token Refresh** | `TokenAuthenticator` | Mock harus return `token` baru (bukan null) di `data.token` | Verify di mock response shape |
| 5 | **QR Code Time-Bound** | `StaffQRActivity`, `ScanQrActivity` | `expires_at` di masa lalu → 400 "Token QR tidak valid" | Generate QR di test setup window |
| 6 | **Cron / Scheduled Jobs** | `NotificationPollWorker` | `auto_absen` & `notifications` berjalan di Vercel Cron | Cancel worker di `@Before` |
| 7 | **Geolocation / IP** | — | Tidak ada endpoint berbasis geolocation | N/A |
| 8 | **Sensor Hardware** | `ScanQrActivity`, `DeviceHelper` | Kamera butuh real autofocus, IMEI butuh READ_PHONE_STATE | Manual test untuk E2E |
| 9 | **Server-Side Rate-Limit** | `middleware/ratelimit.go` (inferred) | IP-based throttle pada login, OTP, register | Mock server-side untuk unit test; real untuk E2E |

### 5.5 3 Quirk Mobile yang Wajib Di-mock Konsisten

1. **Login 409 di mobile = "sudah absen" → success**: `QRCodeRepository.parseErrorBodyAsVerifyResponse()` parse error body 409 sebagai success response. Mock server harus return shape yang sama untuk 409.
   ```json
   // 409 response di-handle sebagai success dengan data:
   {
     "valid": false,
     "nis": "12345",
     "nama_sholat": "Dzuhur",
     "status": "HADIR"
   }
   ```

2. **Refresh token rotate atau reuse**: Mobile keep refresh token lama jika `data.refresh_token` null/empty. Mock harus konsisten dengan production behavior (periksa `AuthResponse.refresh_token` di model).

3. **Hardware_id binding di header/body**: Beberapa endpoint (`/v2/attendance/qr-codes/verify`, `/v2/attendance/code/verify`, `/v2/attendance/barcode/verify`) menerima `hardware_id` di body. Mock harus valid ini untuk simulasi device binding fail.

### 5.6 Daftar Endpoint Flaky

| Endpoint | Risiko | Test Strategy |
|----------|--------|---------------|
| `POST /v2/auth/sessions` | Brute-force cooldown setelah 5 gagal | Reset `login_brute_force` di `@Before` |
| `POST /v2/auth/verify-otp` | OTP expire, cooldown 60s | Request OTP baru setiap test |
| `POST /v2/auth/reset-password` | OTP chain (forgot → verify → reset) | Reset `password_reset` SharedPreferences |
| `POST /v2/auth/tokens/refresh` | Token bisa benar-benar expired | Mock atau sediakan reset endpoint |
| `POST /v2/attendance/qr-codes/verify` | Butuh QR aktif di window yang sama | Generate QR di setup, verify dalam expiry |
| `POST /v2/attendance/code/verify` | Kode 6-digit expire (lihat `expires_in`) | Pakai kode baru |
| `POST /v2/device-auth/register` | Hardware_id binding | Setiap emulator punya akun berbeda |
| `POST /v2/device/change-request` | Max 1 pending per akun (409 jika duplikat) | Clear pending request di setup |
| `POST /v2/attendance/{nis}/attendances` | Duplicate detection | Cek dulu apakah sudah ada |
| `GET /v2/notifications` | Race dengan `NotificationPollWorker` | Cancel worker di `@Before` |
| `POST /v2/admin/promotion/execute` | **Side-effect masif** (promote/graduate ratusan siswa) | **JANGAN panggil di CI**; gunakan `/simulate` |
| `POST /v2/students/import` | Validator server-side mungkin reject | Pakai data test valid |
| `GET /v2/health` | Vercel cold-start 5-10s di first request | Add timeout di test (30s) |
| `GET /v2/prayer-schedules/today` | Timezone-dependent (server UTC) | Test dalam satu timezone |
| `GET /v2/female-restriction/status` | Status berubah seiring `expires_at` | Hindari test yang menunggu lewat expiry |

### 5.7 Konfigurasi Klien yang Mempengaruhi Test

| Setting | Nilai | Lokasi |
|---------|-------|--------|
| `BASE_URL` | `http://127.0.0.1:3000/api/` (debug) | `RetrofitClient.kt:15` |
| Timeouts | connect 15s, read 30s, write 30s | `RetrofitClient.kt:43-45` |
| Logging | BODY in debug, NONE in release | `RetrofitClient.kt:24-28` |
| Cleartext traffic | Diizinkan untuk `10.0.2.2`, `127.0.0.1`, `192.168.1.*` | `res/xml/network_security_config.xml` |
| `adb reverse` | Wajib untuk physical device | `USB_DEVICE_SETUP.md`, `setup_usb_device.sh` |
| Encrypted storage | AES-256 GCM (`androidx.security:security-crypto`) | `SecurePreferences.kt` |
| Token storage | 2 store: `user_session` (lengkap) + `UserData` (subset) | `SecurePreferences.kt:13-19` |
| Dual-write | Login & refresh update **kedua** store | `MasukActivity.kt:265-296` |

---

## 6. Test Strategy Pyramid

### 6.1 Layer 1: Unit Tests (JVM, `app/src/test/`)
**Target**: Repository, Validator, Util, Model parsing.
**Tools**: JUnit 4, MockK atau Mockito-Kotlin, MockWebServer, `kotlinx-coroutines-test`.

| Test Class | Target | Mock |
|------------|--------|------|
| `AuthRepositoryTest` | Login, refresh, profile, logout | `ApiService` + MockWebServer |
| `QRCodeRepositoryTest` | Verify QR, parse 409 as success | `ApiService` + MockWebServer |
| `StudentRepositoryTest` | CRUD siswa, filter, import | `ApiService` + MockWebServer |
| `PengajuanIzinRepositoryTest` | Submit izin, upload foto | `ApiService` + MockWebServer |
| `*ModelsTest` | JSON parsing, edge cases | Gson `fromJson` |
| `SecurePreferencesTest` | Dual-store consistency | Robolectric (perlu Android Context) |
| `DeviceHelperTest` | ANDROID_ID, IMEI fallback | Robolectric |
| `TokenAuthenticatorTest` | Refresh flow, anti-loop | MockWebServer + 401 simulation |
| `MasukActivityBruteForceTest` | Cooldown logic | Robolectric |
| `VerifikasiOtpCooldownTest` | OTP retry cooldown | Robolectric |
| `NotificationPollWorkerTest` | Periodic work | `WorkManagerTestInitHelper` |

### 6.2 Layer 2: Instrumented Tests (Android, `app/src/androidTest/`)
**Target**: Activity flow, UI interaction, permission handling.
**Tools**: Espresso, UiAutomator, `WorkManagerTestInitHelper`, `ActivityScenarioRule`.

| Test Class | Target | Setup |
|------------|--------|-------|
| `MasukActivityTest` | Login form, error display, navigation by role | MockWebServer + reset brute-force |
| `DaftarActivityTest` | Form validation, password strength | MockWebServer |
| `VerifikasiOtpActivityTest` | OTP input, auto-focus, cooldown | MockWebServer |
| `ScanQrActivityTest` | Camera permission, mock BarcodeCallback | `BarcodeView.setDecoderFactory` stub |
| `PengajuanIzinActivityTest` | Date picker, file picker stub | Espresso-Intents |
| `NotificationCenterActivityTest` | Mark as read, list display | MockWebServer |
| `DataSiswaAdminActivityTest` | Pagination, search, filter, bulk action | MockWebServer |
| `LaporanAdminActivityTest` | Export PDF/Excel/CSV | DownloadManager + file system verify |
| `PengaturanActivityTest` | Theme switcher, recreate | UiAutomator |
| `BaseActivityUpdateCheckTest` | GitHub API response, dialog | MockWebServer |
| `NotificationPollWorkerTest` | Periodic work execution | `WorkManagerTestInitHelper` |

### 6.3 Layer 3: E2E Tests (Real Device + Real Backend)
**Target**: Hardware-specific, full integration.
**Tools**: UiAutomator, real device farm, real `absensholat-api` deployment.

| Test Scenario | Device | Backend |
|---------------|--------|---------|
| Scan QR dengan kamera fisik | 2 device (1 staff generate, 1 siswa scan) | Real API + test data |
| Install APK update | 1 device | Real GitHub Release |
| Multi-device hardware_id conflict | 2 device dengan akun sama | Real API |
| Print PDF ke printer | 1 device + printer emulator | Real API |
| Notifikasi 15-menit polling | 1 device, tunggu 15 menit | Real API |
| Auto-mark absen via cron | 1 device, tunggu trigger time | Real API + Vercel cron |
| Dark mode + edge-to-edge | 1 device, multiple orientation | Real API |

### 6.4 Layer 4: Manual Test Checklist (tidak bisa otomatis)
- Hardware-specific IMEI/ANDROID_ID retrieval di berbagai device.
- Real APK download + install + relaunch.
- PrintManager PDF flow ke printer fisik.
- Network state switching (4G/WiFi).
- Real camera autofocus & exposure di QR scan.
- File picker system UI.
- GitHub Release fetching (response dinamis).

---

## 7. Perbandingan Mobile (`mobile_ta`) vs Desktop (`absensholat-desktop-el`)

> **Status**: ⏳ **MENUNGGU INPUT USER**
> 
> Workspace saya (`mobile_ta`) tidak dapat membaca folder `absensholat-desktop-el` secara langsung. Untuk melengkapi section ini, mohon paste-kan:
> 1. Struktur folder utama (`tree -L 3 absensholat-desktop-el` atau `ls -R`).
> 2. Isi `package.json` (dependencies + scripts).
> 3. Daftar screen/route/view (Vue Router? React Router? Next.js pages?).
> 4. Library hardware yang dipakai (QR scanner, fingerprint, geolocation).
> 5. Cara autentikasi (login flow di desktop).
> 6. Apakah ada folder `tests/`, `e2e/`, `cypress/`, `playwright/`, `__tests__/`?

### 7.1 Template Feature Matrix (akan diisi setelah input)

| Fitur | Mobile `mobile_ta` | Desktop `absensholat-desktop-el` | Catatan |
|-------|---------------------|----------------------------------|---------|
| Login (JWT + refresh) | ✅ `MasukActivity` | ⏳ TBD | Apakah desktop pakai same flow? |
| Scan QR Presensi | ✅ Kamera + ZXing | ⏳ TBD (WebRTC? `html5-qrcode`? library Electron?) | Hardware requirement beda |
| Generate QR (Staff) | ✅ `StaffQRActivity` | ⏳ TBD | |
| Cetak QR (Admin) | ✅ `QRCodeAdminActivity` | ⏳ TBD | Print ke printer = advantage desktop |
| Manajemen Siswa (CRUD) | ✅ `DataSiswaAdminActivity` | ⏳ TBD | |
| Manajemen Kelas/Guru | ✅ `KelolaKelasActivity`, `KelolaGuruAdminActivity` | ⏳ TBD | |
| Pengajuan Izin | ✅ `PengajuanIzinActivity` | ⏳ TBD | Upload foto = mobile advantage |
| Laporan (Excel/PDF/CSV) | ✅ `LaporanAdminActivity` | ⏳ TBD | Export to file = advantage desktop |
| Notifikasi | ✅ `NotificationHelper` + `NotificationPollWorker` | ⏳ TBD | System notification vs web push |
| Device Management | ✅ `AdminDeviceManagementActivity` | ⏳ TBD | Hardware_id binding = mobile only? |
| Pengaturan Akun (Profil) | ✅ `PengaturanAkunActivity` | ⏳ TBD | Foto profil = mobile advantage |
| Dark Mode / Theme | ✅ `PengaturanActivity` | ⏳ TBD | |
| Auto-update APK | ✅ `UpdateChecker` + `UpdateDownloader` | ⏳ TBD | Mungkin pakai `electron-updater`? |
| Female Restriction (P) | ✅ `FemaleRestrictionStatusActivity` | ⏳ TBD | |
| Student Promotion | ✅ `StudentPromotionActivity` | ⏳ TBD | |

### 7.2 Template Asumsi Awal (perlu verifikasi)
Berdasarkan pattern industri:
- **Desktop kemungkinan** pakai **Electron + Vue/React** (nama folder `*-el`暗示 Electron + Vue/Element?).
- **Hardware dependencies lebih sedikit** di desktop (tidak ada kamera, tidak ada IMEI, tidak ada APK install).
- **Advantage desktop**: keyboard input lebih cepat untuk admin (search, bulk input), print ke printer native, file system access langsung.
- **Advantage mobile**: scan QR, foto bukti izin, foto profil, location-aware (tidak dipakai di sini), push notification.
- **Shared**: login flow, CRUD data master, laporan, pengajuan izin, notifikasi (web push atau polling).

### 7.3 Gap yang Perlu Diinvestigasi
Setelah data desktop tersedia, identifikasi:
1. **Fitur yang ada di mobile tapi TIDAK ada di desktop** (atau sebaliknya).
2. **Implementasi berbeda** untuk fitur yang sama (misal: scan QR di mobile vs input kode manual di desktop).
3. **Pattern testing desktop** (jika ada) — apakah bisa di-reuse untuk mobile?

---

## 8. Rekomendasi Test Prioritas

### 8.1 HIGH VALUE (kerjakan duluan)
1. **Auth flow** (`AuthRepository` + `MasukActivity` + `VerifikasiOtpActivity`) — gerbang utama aplikasi.
2. **QR Code verification** (`QRCodeRepository` + `ScanQrActivity`) — fitur paling kritis.
3. **Semua Repository** — 1 test per endpoint = 150 test value.
4. **`TokenAuthenticator`** — refresh logic + anti-loop.
5. **`SecurePreferences`** — dual-store consistency + migration.
6. **`NotificationPollWorker`** — periodic background.
7. **Admin CRUD** (kelas, guru, siswa) — bulk action + import CSV.
8. **Brute-force & cooldown** (`MasukActivity`, `VerifikasiOtpActivity`).
9. **`DeviceHelper`** — hardware_id handling + IMEI fallback.
10. **`BaseActivity.checkForUpdate()`** — GitHub API response parsing.

### 8.2 MEDIUM VALUE
1. **Chart rendering** (`MPAndroidChart`) — screenshot test.
2. **Edge-to-edge insets** — UiAutomator multi-device.
3. **Search debounce** (9 activity) — coroutine test.
4. **Countdown timer** (QR, OTP resend, login cooldown) — mock time.
5. **Dark mode switcher** — visual regression.

### 8.3 LOW VALUE / SKIP
1. Hardware-specific IMEI/ANDROID_ID.
2. Real APK download + install.
3. PrintManager PDF.
4. Network state switching.
5. Real camera autofocus.
6. File picker system UI.
7. GitHub Release fetching.

---

## 9. Implementation Roadmap

### Phase 1: Setup Test Infrastructure (1-2 hari)
1. Tambahkan dependencies ke `app/build.gradle.kts`:
   ```kotlin
   testImplementation("junit:junit:4.13.2")
   testImplementation("org.mockito:mockito-core:5.7.0")
   testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
   testImplementation("org.robolectric:robolectric:4.11.1")
   testImplementation("androidx.test:core:1.5.0")
   
   androidTestImplementation("androidx.test.ext:junit:1.1.5")
   androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
   androidTestImplementation("androidx.test:runner:1.5.2")
   androidTestImplementation("androidx.test:rules:1.5.0")
   androidTestImplementation("androidx.test:work:1.0.0")
   androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
   ```
2. Setup folder structure: `app/src/test/java/...` dan `app/src/androidTest/java/...`.
3. Buat `BaseTestRule` untuk MockWebServer + reset SharedPreferences + cancel WorkManager.

### Phase 2: Unit Tests untuk Repository (3-5 hari)
1. `AuthRepositoryTest` — login, refresh, profile, verify-account, change-password, forgot-password, verify-otp, reset-password.
2. `QRCodeRepositoryTest` — verify QR, verify code, verify barcode, parse 409 as success.
3. `StudentRepositoryTest` — CRUD, filter, import, bulk progression.
4. `PengajuanIzinRepositoryTest` — submit, approve/reject, upload.
5. `JadwalSholatRepositoryTest` — today, closest, dhuha, dzuhur, CRUD.
6. `NotificationRepositoryTest` — list, mark read, bulk read, delete.
7. `AnalyticsRepositoryTest` — statistics, charts, pending.
8. `DeviceManagementRepositoryTest` — list, approve/reject, change-device.
9. `FemaleRestrictionRepositoryTest` — status, request, approve, history.
10. `PromotionRepositoryTest` — config, simulate, execute (skip execute di CI).

### Phase 3: Unit Tests untuk Util & Logic (2-3 hari)
1. `SecurePreferencesTest` — dual-store, migration, encryption.
2. `DeviceHelperTest` — ANDROID_ID, IMEI fallback, edge cases.
3. `TokenAuthenticatorTest` — refresh, anti-loop, network error.
4. `MasukActivityBruteForceTest` — cooldown, counter reset.
5. `VerifikasiOtpCooldownTest` — retry logic, timer.
6. `NotificationPollWorkerTest` — periodic, foreground, background.
7. `*ModelsTest` — JSON parsing, null safety, default values.

### Phase 4: Instrumented Tests untuk Critical Flow (3-4 hari)
1. `MasukActivityTest` — login form, error display, role-based navigation.
2. `VerifikasiOtpActivityTest` — input flow, auto-focus, cooldown.
3. `ScanQrActivityTest` — camera permission grant, mock BarcodeCallback.
4. `PengajuanIzinActivityTest` — date picker, file picker stub.
5. `DataSiswaAdminActivityTest` — pagination, search debounce, bulk action.
6. `LaporanAdminActivityTest` — export PDF/Excel/CSV + verify file.
7. `NotificationCenterActivityTest` — mark as read, list display.
8. `BaseActivityUpdateCheckTest` — GitHub response + dialog.

### Phase 5: E2E Tests (1-2 hari setup, ongoing)
1. Setup test data di `absensholat-api` (akun siswa, guru, admin).
2. Setup 2 physical device (1 staff, 1 siswa).
3. Tulis E2E test untuk scan QR end-to-end.
4. Tulis E2E test untuk multi-device hardware_id conflict.
5. Tulis E2E test untuk install APK update.

### Phase 6: CI Integration (1 hari)
1. Setup GitHub Actions / GitLab CI untuk run unit test + instrumented test.
2. **PENTING**: Jangan panggil `POST /v2/admin/promotion/execute` di CI.
3. Setup MockWebServer fixture untuk semua Repository.
4. Document manual test checklist untuk hardware-specific.

---

## 10. Verification (Cara Validasi Plan)

### 10.1 Verifikasi Kontrak API
1. Buka `absensholat-api/API_REFERENCE.md` (atau `README.md` jika tidak ada).
2. Cocokkan daftar endpoint di §5.2 dengan dokumentasi.
3. Cocokkan schema database di §5.3 dengan file migrasi.
4. Identifikasi field/response baru yang belum ada di `mobile_ta/app/src/main/java/api/*Models.kt`.

### 10.2 Verifikasi Hardware Dependencies
1. Cek semua permission di `mobile_ta/AndroidManifest.xml` cocok dengan §2.5.
2. Cek semua `Settings.Secure.*` atau `TelephonyManager.get*()` calls — apakah ada yang terlewat.
3. Cek `FileProvider` declarations — apakah untuk share APK atau juga untuk fitur lain.

### 10.3 Verifikasi Perbandingan Desktop
1. **Minta user paste** struktur `absensholat-desktop-el/` (lihat §7).
2. Cocokkan feature matrix di §7.1 dengan implementasi desktop.
3. Identifikasi pattern testing desktop (jika ada) yang bisa di-reuse.

### 10.4 Smoke Test
Setelah implementasi, jalankan:
```bash
cd /home/thinkpad/Documents/malik/ta/mobile_ta
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests (butuh device)
```

---

## 11. Catatan & Asumsi

### Asumsi yang Perlu Diverifikasi
- **Kontrak API**: Direkonstruksi dari `ApiService.kt` (mobile). Verifikasi dengan `absensholat-api/API_REFERENCE.md` jika tersedia.
- **Database schema**: Direkonstruksi dari model response. Field/constraint server-side mungkin lebih ketat.
- **Rate-limit values**: 5 gagal → 30s cooldown untuk login, 60s untuk OTP — diasumsikan dari pola umum. Verifikasi dengan `absensholat-api/middleware/ratelimit.go`.
- **Token expiration**: Access token & refresh token expiration TIDAK terlihat jelas dari mobile. Diasumsikan ~15 menit access, ~30 hari refresh.
- **Vercel cold-start**: `GET /v2/health` 5-10s di first request. Threshold timeout test = 30s.

### Out of Scope
- Backend `absensholat-api` (tidak mengubah).
- Desktop `absensholat-desktop-el` (hanya analisis read-only).
- Performance testing (load, stress).
- Security audit (penetration testing).
- UI/UX audit (sudah dilakukan di fase sebelumnya).

### Risiko Implementasi
- **MockWebServer drift**: Mock response bisa outdated jika API berubah. Mitigasi: contract test + update mock saat API version bump.
- **Hardware test flakiness**: Real device bisa berbeda behavior. Mitigasi: multi-device test matrix.
- **CI cost**: Instrumented test butuh device farm. Mitigasi: prioritize unit test, gunakan Firebase Test Lab / BrowserStack untuk instrumented.
- **Time budget**: Phase 1-4 = ~10-15 hari kerja. Phase 5-6 = ongoing. Total = ~3-4 minggu untuk MVP coverage.

---

## 12. Deliverable Setelah Eksekusi

Jika plan ini disetujui dan dijalankan:
1. **Test infrastructure** siap di `mobile_ta` (folder `test/`, `androidTest/`, base rules, dependencies).
2. **~80-100 unit test** mencakup 10 Repository + 7 util.
3. **~15-20 instrumented test** untuk critical Activity flow.
4. **Manual test checklist** untuk hardware-specific + E2E.
5. **CI pipeline** running unit test di setiap push, instrumented test nightly.
6. **Updated feature matrix** mobile vs desktop (setelah input struktur desktop).

---

**Status Plan**: ⏳ Menunggu input struktur `absensholat-desktop-el/` untuk melengkapi §7.
