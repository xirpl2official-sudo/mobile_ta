# Laporan Analisis dan Rekomendasi Perbaikan: DeadObjectException pada SASMobile

Laporan ini menganalisis kesalahan `android.os.DeadObjectException` dan kegagalan transaksi Binder yang terjadi selama transisi antara `PresensiSholatAdminActivity` dan `JadwalSholatAdminActivity` pada aplikasi SASMobile.

## 1. Root Cause Analysis (Analisis Akar Masalah)

Berdasarkan log dan kode sumber, penyebab utama masalah ini adalah **instabilitas pada komunikasi Binder antar-proses** selama transisi aktivitas yang intensif.

### A. Lifecycle Race Condition pada Navigasi
Metode `navigateTo` di `BaseAdminActivity` menggunakan `DrawerListener` untuk memulai aktivitas baru segera setelah drawer ditutup, diikuti dengan pemanggilan `finish()`.
*   **Masalah:** Pemanggilan `finish()` yang sangat cepat setelah `startActivity()` dapat menyebabkan kondisi balapan (*race condition*). Sistem (melalui Binder) mencoba mengirimkan perintah `dispatchAppVisibility` ke aktivitas yang sedang dalam proses penghancuran.
*   **Dampak:** Muncul `DeadObjectException` karena objek Binder yang mewakili aktivitas tersebut sudah dianggap "mati" oleh sistem.

### B. Overload Transaksi Binder
Kesalahan Binder (error -22, -32) dan SurfaceFlinger menunjukkan bahwa pipa komunikasi antar-proses (IPC) terbebani.
*   **Penyebab:** Penggunaan flag `FLAG_ACTIVITY_REORDER_TO_FRONT` dikombinasikan dengan pemanggilan `finish()` yang tidak sinkron dapat menyebabkan manajer aktivitas sistem (ActivityManager) kesulitan mengelola tumpukan task (Task ID 1281).
*   **Penyebab:** Data yang terlalu besar atau frekuensi pembaruan UI yang terlalu tinggi saat transisi (seperti pemuatan data otomatis di `onCreate`) dapat memenuhi buffer Binder.

### C. Resource Leak pada Background Task
Penggunaan `Handler.postDelayed` dan `lifecycleScope.launch` tanpa pembersihan manual atau pengecekan status aktivitas di `onDestroy` dapat memicu pembaruan UI pada aktivitas yang sudah tidak valid.

---

## 2. Immediate Fixes (Perbaikan Segera)

### A. Stabilisasi Navigasi di BaseAdminActivity
Gunakan penundaan kecil atau pastikan `finish()` dipanggil hanya jika diperlukan untuk menjaga stabilitas tumpukan aktivitas.

### B. Pengecekan Status Aktivitas
Selalu gunakan `isFinishing` atau `isDestroyed` sebelum melakukan operasi UI di dalam callback asinkron atau Handler.

### C. Pembersihan Resource
Pastikan semua referensi ke UI (seperti Adapter atau Handler) dibersihkan atau dibatalkan di `onDestroy`.

---

## 3. Best Practices Implementation (Implementasi Praktik Terbaik)

1.  **Gunakan Lifecycle-Aware Components:** Manfaatkan `lifecycleScope` secara maksimal dan hindari penggunaan `Handler` mentah jika memungkinkan.
2.  **Manajemen Task yang Bersih:** Jika ingin mempertahankan satu instance aktivitas, gunakan `launchMode="singleTop"` di `AndroidManifest.xml` daripada memanipulasi flag secara manual di setiap intent.
3.  **Debouncing UI Updates:** Gunakan mekanisme debounce untuk pencarian (seperti yang sudah ada di `PresensiSholatAdminActivity`) dan pastikan `Runnable` tersebut dibatalkan di `onDestroy`.

---

## 4. Code Examples (Contoh Kode)

### A. Perbaikan pada BaseAdminActivity.kt
Ubah metode `navigateTo` untuk menghindari tabrakan Binder:

```kotlin
// Di BaseAdminActivity.kt
protected fun navigateTo(activityClass: Class<out BaseAdminActivity>) {
    if (this::class.java == activityClass) {
        closeSidebar()
        return
    }

    if (!::drawerLayout.isInitialized) {
        startActivity(Intent(this, activityClass))
        finish()
        return
    }

    drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerClosed(drawerView: View) {
            drawerLayout.removeDrawerListener(this)
            
            val intent = Intent(this@BaseAdminActivity, activityClass)
            // Menggunakan SINGLE_TOP lebih stabil daripada REORDER_TO_FRONT dalam banyak kasus
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            startActivity(intent)
            
            // Berikan sedikit jeda sebelum finish untuk stabilitas Binder
            window.decorView.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    finish()
                }
            }, 100)
        }
    })
    closeSidebar()
}
```

### B. Perbaikan pada PresensiSholatAdminActivity.kt
Tambahkan pembersihan resource:

```kotlin
// Di PresensiSholatAdminActivity.kt
override fun onDestroy() {
    // 1. Batalkan semua callback search
    searchRunnable?.let { searchHandler.removeCallbacks(it) }
    
    // 2. Bersihkan referensi adapter untuk mencegah memory leak
    if (::recyclerPresensi.isInitialized) {
        recyclerPresensi.adapter = null
    }
    
    super.onDestroy()
}

// Pastikan loadData mengecek status
private fun loadData() {
    if (isFinishing || isDestroyed) return
    // ... sisa kode loadData
}
```

### C. Perbaikan pada JadwalSholatAdminActivity.kt
Gunakan `lifecycleScope` dengan benar dan hindari `runOnUiThread` yang redundan:

```kotlin
// Di JadwalSholatAdminActivity.kt
private fun loadJadwalList() {
    val token = getAuthToken()
    if (token.isEmpty()) return

    // lifecycleScope secara otomatis berhenti saat onDestroy
    lifecycleScope.launch {
        repository.getJadwalSholat(token).fold(
            onSuccess = { list ->
                // lifecycleScope.launch berjalan di Main thread secara default
                // Pastikan aktivitas masih ada
                if (!isFinishing && !isDestroyed) {
                    jadwalList = list
                    updateJadwalUI()
                }
            },
            onFailure = { error ->
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@JadwalSholatAdminActivity, 
                        "Gagal: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
```

---

## 5. Testing Recommendations (Rekomendasi Pengujian)

1.  **Stress Test Navigasi:** Lakukan perpindahan menu secara cepat di Sidebar (minimal 10-20 kali berturut-turut) untuk melihat apakah Binder mengalami hang atau crash.
2.  **Monkey Testing:** Gunakan perintah `adb shell monkey -p com.xirpl2.SASMobile 500` untuk mensimulasikan input pengguna secara acak dan cepat.
3.  **LeakCanary:** Integrasikan library `LeakCanary` untuk mendeteksi apakah ada aktivitas yang tertahan di memori setelah `finish()` dipanggil, yang seringkali menjadi pemicu `DeadObjectException`.
4.  **Monitor Logcat:** Perhatikan pesan "Binder transaction failed" atau "Window already focused, ignoring focus gain". Jika muncul, berarti ada tumpang tindih aktivitas yang perlu diperbaiki.

---
**Disusun Oleh:** Gemini CLI Specialist
**Tanggal:** 16 Mei 2026
