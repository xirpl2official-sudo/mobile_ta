📋 Android Navigation QA Checklist

  1. Navigasi Dasar (Basic Navigation & Flow)
  *Pengujian ini penting untuk memastikan *user journey* berjalan sesuai desain (UI/UX) dan tidak ada rute mati (dead ends).*

   - [x] Forward Navigation: Berpindah dari Screen A ke Screen B menggunakan aksi UI (tombol/link) berfungsi dengan mulus.
   - [x] Bottom Navigation / Drawer (Jika ada): Berpindah antar tab utama melalui BottomNavigationView tidak memunculkan Fragment baru yang berlipat ganda,
     melainkan me-rute ulang dengan benar (popUpTo / launchSingleTop).
   - [x] Deep Linking: Mengakses halaman spesifik melalui URL atau Push Notification langsung membuka Screen yang dituju dan mengabaikan halaman pembuka jika
     perlu.
   - [x] Splash Screen / Onboarding: Setelah masuk ke beranda dari Splash/Onboarding, menekan tombol Back menutup aplikasi, bukan kembali ke Splash/Onboarding.

  2. Pengiriman Data (Passing Arguments & Safe Args)
  *Menguji Integritas data. Kehilangan data di tengah rute atau salah tipe data (*casting error*) adalah penyebab utama `NullPointerException` (NPE).*

   - [x] Valid Data: Data (Teks, ID, Objek Parcelable/Serializable) terkirim secara utuh dari Screen A dan tertampil dengan benar di Screen B.
   - [x] Empty Data (String): Mengirim string kosong ("") tidak menyebabkan crash dan ditangani UI dengan baik (misal: "Data tidak tersedia").
   - [x] Null Data (Nullable): Jika argumen diset opsional/nullable di nav_graph, mengirimkan null ditangani dengan logika fallback/default value.
   - [x] Boundary Value: Mengirim tipe numerik (Int/Long) dengan angka negatif, nol (0), atau nilai maksimal ditangani secara matematis dengan benar di halaman
     tujuan.
   - [x] Type-Safety (Safe Args): (Pengecekan Code-level) Memastikan perpindahan tidak menggunakan Bundle manual (putString), melainkan menggunakan kelas
     Directions hasil generate (contoh: HomeFragmentDirections.actionToDetail(id)).

  3. Manajemen Backstack & State (Back Navigation)
  UX yang buruk sering terjadi di sini (infinite loop halaman, state reset). Pengujian ini memastikan aplikasi kembali ke tempat yang logis sesuai ekspektasi
  user.

   - [x] System Back Button: Menekan tombol kembali fisik/sistem (gesture geser) di Android membawa user ke layar logis sebelumnya.
   - [x] Up Button (Toolbar/Appbar): Panah kembali di kiri atas navigasi UI membawa ke parent secara hierarki, dan perilakunya konsisten dengan tombol System
     Back.
   - [x] Form State Preservation: Jika user mengisi formulir di Screen A, pergi ke Screen B, lalu kembali lagi ke Screen A, isian formulir di Screen A tidak
     ter-reset (State tersimpan lewat ViewModel).
   - [x] PopUpTo (Circular Nav): Skenario A -> B -> C -> A. Ketika menekan tombol dari C ke A, menekan Back dari A menutup aplikasi (atau ke Home), tidak kembali
     ke C. (Pastikan atribut popUpTo dan popUpToInclusive dikonfigurasi pada aksi di nav_graph.xml).
   - [x] Logout Flow: Setelah proses Logout, semua backstack dihapus sehingga user tidak bisa menekan kembali ke area Dashboard/Profile yang butuh autentikasi.

  4. Edge Cases & Kondisi Ekstrem
  Inilah yang membedakan aplikasi rata-rata dengan standar industri. Cacat pada poin ini memicu IllegalArgumentException: Navigation action/destination cannot be
  found.

   - [x] Double-Click (Rapid Taps): Menekan tombol navigasi berkali-kali secepat kilat (spam click) tidak membuat halaman terbuka dua/tiga lapis, atau
     menyebabkan aplikasi crash. (Pastikan Anda menggunakan mekanisme pencegahan klik ganda pada UI atau memvalidasi destinasi saat ini di Navigation Controller
     sebelum memanggil aksi).
   - [x] Rotasi Layar di Perjalanan: Menekan tombol navigasi lalu segera merotasi perangkat sebelum animasi halaman selesai tidak menyebabkan layar nge-blank
     atau crash.
   - [x] Network Latency: Menekan navigasi yang membutuhkan pemanggilan API. Jika proses memakan waktu, ada loading state yang memblokir interaksi navigasi
     berulang.
   - [x] Background Death: Mengubah aplikasi ke latar belakang (Background), mensimulasikan "Process Death" (via Developer Options "Don't keep activities" atau
     Android Studio Logcat), dan membuka aplikasi kembali tidak merusak stack navigasi.

  5. Dialog & Bottom Sheet (Navigasi Overlay)
  Navigasi tidak hanya layar penuh, melainkan juga komponen mengambang. Ini sering terlewat.

   - [x] Membuka Dialog: Membuka DialogFragment via Navigation Component (<dialog> tag) bekerja dan data bisa dipassing via argumen.
   - [x] Menutup Dialog: Menekan area luar (dimmed area) atau tombol "Batal/Kembali" memanggil navigasi navigateUp() / popBackStack() tanpa error.
   - [x] Stacking Dialog: Jika Dialog A membuka Dialog B, menekan back menutup B lalu menekan back lagi menutup A.

  ---

  💡 Tips Pro QA dari Saya:
   * Wajib Gunakan "Don't keep activities": Untuk menguji State Preservation, selalu nyalakan opsi ini di Developer Options HP testing Anda. Jika aplikasi Anda
     aman saat opsi ini menyala, aplikasi Anda sudah lolos standar korporat.
   * Mencegah Double Click Crash: Masalah klasik di Nav Component adalah spam click. Tambahkan extension function pada NavController yang selalu mengecek apakah
     destinasi saat ini valid sebelum menjalankan aksi navigate(). Contoh: if (currentDestination?.id == R.id.currentFragment) { navigate(direction) }.

  Silakan centang (checklist) setiap kali Anda telah memvalidasi skenario tersebut. Jika Anda butuh bantuan menulis unit test (FragmentScenario) atau
  instrumented test (Espresso + TestNavHostController) untuk otomatisasi poin-poin di atas, beri tahu saya!

