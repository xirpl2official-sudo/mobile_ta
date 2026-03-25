package com.xirpl2.SASMobile.model
import com.google.gson.annotations.SerializedName

/**
 * Response model untuk API Jadwal Sholat
 */
/**
 * Response model untuk API Jadwal Sholat (Paginated)
 */
data class JadwalSholatListResponse(
    val message: String,
    val data: List<JadwalSholatData>,
    val pagination: PaginationInfo? = null,
    val filters: JadwalSholatFilters? = null
)

data class JadwalSholatFilters(
    val hari: String? = null,
    val jenis_sholat: String? = null,
    val jurusan: String? = null
)

data class JadwalSholatData(
    @SerializedName("id_jadwal")
    val id: Int,
    @SerializedName("jenis_sholat")
    val jenis_sholat: String,
    @SerializedName("waktu_mulai")
    val jam_mulai: String,
    @SerializedName("waktu_selesai")
    val jam_selesai: String,
    var hari: String? = null, // Optional: jika API mengirim info hari
    var jurusan: String? = null // Optional: jurusan specific schedule
)

/**
 * Response model untuk GET jadwal sholat by ID
 */
data class JadwalSholatDetailResponse(
    val message: String,
    val data: JadwalSholatDetail? = null
)

data class JadwalSholatDetail(
    val id_jadwal: Int = 0,
    val jenis_sholat: String = "",
    @SerializedName("waktu_mulai")
    val jam_mulai: String = "",
    @SerializedName("waktu_selesai")
    val jam_selesai: String = "",
    val hari: String? = null,
    val jurusan: String? = null,
    val created_at: String? = null
)

/**
 * Request model untuk UPDATE jadwal sholat
 */
data class JadwalSholatUpdateRequest(
    val jenis_sholat: String? = null,
    @SerializedName("waktu_mulai")
    val jam_mulai: String? = null,
    @SerializedName("waktu_selesai")
    val jam_selesai: String? = null,
    val hari: String? = null,
    val jurusan: String? = null
)



/**
 * Response model untuk API Riwayat Absensi
 */
data class RiwayatAbsensiResponse(
    val success: Boolean,
    val message: String,
    val data: List<RiwayatAbsensiData>
)

data class RiwayatAbsensiData(
    val id: Int,
    val tanggal: String,
    val jenis_sholat: String,
    val status: String, // "HADIR", "ALPHA", "SAKIT", "IZIN"
    val waktu_absen: String? = null
)

/**
 * Response model untuk User Profile (untuk mendapatkan jenis kelamin)
 */
data class UserProfileResponse(
    val success: Boolean,
    val message: String,
    val data: UserData
)

data class UserData(
    val id: Int,
    val nama: String,
    val nis: String,
    val jenis_kelamin: String, // "L" atau "P"
    val kelas: String,
    val email: String? = null,
    val no_hp: String? = null
)

/**
 * Response wrapper untuk API Statistics (/api/statistics)
 * Format: {"message":"...", "data":{...}}
 */
data class StatisticsResponse(
    val message: String,
    val data: StatisticsData
)

/**
 * Response model untuk API Statistics (/api/statistics)
 * Statistik kehadiran hari ini
 */
data class StatisticsData(
    val tanggal: String,
    val total_siswa: Int,
    val total_absen_hari_ini: Int,
    val total_kehadiran_hari_ini: Int,
    val total_izin_hari_ini: Int = 0,
    val total_sakit_hari_ini: Int = 0,
    val total_alpha_hari_ini: Int,
    val total_tidak_hadir_hari_ini: Int,
    val persentase_kehadiran: Double,
    val persentase_izin: Double = 0.0,
    val persentase_sakit: Double = 0.0,
    val persentase_alpha: Double = 0.0,
    val rata_rata_kehadiran: Double
)

/**
 * Response model untuk Statistik Absensi
 */
data class StatistikAbsensiResponse(
    val success: Boolean,
    val message: String,
    val data: StatistikData
)

data class StatistikData(
    val total_hari: Int,
    val total_hadir: Int,
    val total_alpha: Int,
    val total_sakit: Int,
    val total_izin: Int,
    val persentase_kehadiran: Float
)

/**
 * Request body untuk submit absensi
 */
data class AbsensiRequest(
    val jadwal_sholat_id: Int,
    val latitude: Double,
    val longitude: Double,
    val foto: String? = null // Base64 encoded image
)

/**
 * Response untuk submit absensi
 */
data class AbsensiResponse(
    val success: Boolean,
    val message: String,
    val data: AbsensiData? = null
)

data class AbsensiData(
    val id: Int,
    val waktu_absen: String,
    val status: String
)

/**
 * Response model untuk API History Siswa (/api/history/siswa)
 * Mengambil riwayat absensi sholat untuk siswa yang sedang login per minggu
 */
data class HistorySiswaResponse(
    val message: String,
    val data: HistorySiswaData? = null
)

data class HistorySiswaData(
    val week: Int = 0,
    val periode: String = "",
    val absensi: List<AbsensiHistoryItem>? = null
)

data class AbsensiHistoryItem(
    val id: Int = 0,
    val tanggal: String = "",
    val hari: String? = null,
    val jenis_sholat: String? = null,
    val jam_mulai: String? = null,
    val jam_selesai: String? = null,
    val status: String = "ALPHA",
    val waktu_absen: String? = null
) {
    // Helper to get the prayer name
    fun getPrayerName(): String = jenis_sholat ?: "Unknown"
}

/**
 * Response model untuk API History Staff (/api/history/staff)
 * Untuk admin/guru/wali_kelas melihat riwayat absensi semua siswa
 */
data class HistoryStaffResponse(
    val message: String,
    val data: HistoryStaffData? = null
)

data class HistoryStaffData(
    val statistik: LaporanStatistik? = null,
    val pagination: PaginationInfo? = null,
    val filters: HistoryFilters? = null,
    val absensi: List<AbsensiStaffItem> = emptyList()
)

/**
 * Statistik laporan dari handlers.LaporanStatistik
 */
data class LaporanStatistik(
    val total_siswa: Int = 0,
    val total_absensi: Int = 0,
    val total_hadir: Int = 0,
    val total_izin: Int = 0,
    val total_sakit: Int = 0,
    val total_alpha: Int = 0,
    val persentase_hadir: Double = 0.0,
    val persentase_izin: Double = 0.0,
    val persentase_sakit: Double = 0.0,
    val persentase_alpha: Double = 0.0,
    val rata_rata_kehadiran: Double = 0.0
)

/**
 * Pagination info dari handlers.PaginationInfo
 */
data class PaginationInfo(
    val page: Int = 1,
    val limit: Int = 20,
    val total_pages: Int = 1,
    val total_items: Int = 0
)

/**
 * Filter history dari handlers.HistoryFilters
 */
data class HistoryFilters(
    val start_date: String? = null,
    val end_date: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    val nis: String? = null,
    val status: String? = null
)

/**
 * Item absensi staff dari handlers.AbsensiStaffItem
 */
data class AbsensiStaffItem(
    val id_absen: Int = 0,
    val nis: String = "",
    val nama_siswa: String = "",
    val kelas: String? = null,
    val jurusan: String? = null,
    val tanggal: String = "",
    val hari: String? = null,
    val jenis_sholat: String? = null,
    val status: String = "alpha",
    val deskripsi: String? = null
)

/**
 * Data model untuk statistik absensi
 */
data class StatistikStaffData(
    val total_siswa: Int = 0,
    val total_hadir: Int = 0,
    val total_izin: Int = 0,
    val total_sakit: Int = 0,
    val total_alpha: Int = 0
)

/**
 * Response model untuk API /jadwal-sholat/dhuha-today
 */
data class DhuhaTodayResponse(
    val message: String,
    val data: List<DhuhaJurusanData>
)

data class DhuhaJurusanData(
    val jurusan: String,
    val jadwal: List<DhuhaJadwalData>
)

data class DhuhaJadwalData(
    val id_jadwal: Int,
    val hari: String,
    val jenis_sholat: String,
    @SerializedName("waktu_mulai")
    val jam_mulai: String,
    @SerializedName("waktu_selesai")
    val jam_selesai: String,
    val jurusan: String,
    val created_at: String
)

/**
 * Response model untuk API /notifications
 * Returns list of students who haven't marked attendance for active prayers
 */
data class NotificationResponse(
    val message: String,
    val data: List<NotificationItem>,
    val count: Int
)

data class NotificationItem(
    val nis: String,
    val nama_siswa: String,
    val kelas: String,
    val jurusan: String,
    val jenis_sholat: String,
    val waktu_mulai: String,
    val id_jadwal: Int
)
