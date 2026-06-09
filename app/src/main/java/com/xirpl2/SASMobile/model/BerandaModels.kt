package com.xirpl2.SASMobile.model
import com.google.gson.annotations.SerializedName

data class JadwalSholatListResponse(
    val message: String? = null,
    val data: List<JadwalSholatData> = emptyList(),
    val pagination: PaginationInfo? = null,
    val filters: JadwalSholatFilters? = null
)

data class JadwalSholatTodayResponse(
    val hari: String? = null,
    val data: List<JadwalSholatData> = emptyList()
)

data class JadwalSholatFilters(
    val hari: String? = null,
    val jenis_sholat: String? = null,
    val jurusan: String? = null
)

data class JadwalSholatData(
    @SerializedName("id_jadwal")
    val id: Int,
    @SerializedName("id_waktu")
    val idWaktu: Int? = null,
    val hari: String? = null, 
    val jurusan: String? = null, 
    val kelas: String? = null,
    @SerializedName("waktu_sholat")
    val waktuSholat: WaktuSholatData? = null,
    @SerializedName("jurusans")
    val jurusans: List<JurusanResponse>? = null,
    @SerializedName("tanggal_khusus")
    val tanggalKhusus: String? = null
) {
    val jenis_sholat: String
        get() = waktuSholat?.jenisSholat?.namaJenis ?: ""
    val jam_mulai: String
        get() = waktuSholat?.waktuMulai ?: ""
    val jam_selesai: String
        get() = waktuSholat?.waktuSelesai ?: ""
}

data class JurusanResponse(
    @SerializedName("id_jurusan")
    val id: Int,
    @SerializedName("nama_jurusan")
    val nama: String
)

data class JadwalSholatDetailResponse(
    val message: String? = null,
    val data: JadwalSholatDetail? = null
)

data class JadwalSholatDetail(
    val id_jadwal: Int = 0,
    val hari: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null,
    val created_at: String? = null,
    @SerializedName("waktu_sholat")
    val waktuSholat: WaktuSholatData? = null
) {
    val jenis_sholat: String
        get() = waktuSholat?.jenisSholat?.namaJenis ?: ""
    val jam_mulai: String
        get() = waktuSholat?.waktuMulai ?: ""
    val jam_selesai: String
        get() = waktuSholat?.waktuSelesai ?: ""
}

data class WaktuSholatData(
    @SerializedName("waktu_mulai")
    val waktuMulai: String? = null,
    @SerializedName("waktu_selesai")
    val waktuSelesai: String? = null,
    @SerializedName("jenis_sholat")
    val jenisSholat: JenisSholatData? = null
)

data class JenisSholatData(
    @SerializedName("nama_jenis")
    val namaJenis: String? = null
)

// Closest Prayer Schedule — matches GET /v2/prayer-schedules/closest response
data class ClosestPrayerResponse(
    val message: String? = null,
    val data: ClosestPrayerData? = null
)

data class ClosestPrayerData(
    val current: JadwalSholatDetail? = null,
    val next: JadwalSholatDetail? = null
)

data class JadwalSholatUpdateRequest(
    val hari: String? = null,
    @SerializedName("id_waktu")
    val id_waktu: Int? = null,
    @SerializedName("waktu_mulai")
    val waktu_mulai: String? = null,
    @SerializedName("waktu_selesai")
    val waktu_selesai: String? = null,
    @SerializedName("jurusan_ids")
    val jurusan_ids: List<Int>? = null
)

data class JadwalSholatCreateRequest(
    @SerializedName("hari")
    val hari: String? = null,
    @SerializedName("tanggal_khusus")
    val tanggal_khusus: String? = null,
    @SerializedName("id_waktu")
    val id_waktu: Int,
    @SerializedName("jurusan_ids")
    val jurusan_ids: List<Int>? = null
)



data class RiwayatAbsensiResponse(
    val success: Boolean,
    val message: String,
    val data: RiwayatAbsensiData? = null
)

data class RiwayatAbsensiData(
    val id: Int,
    val tanggal: String,
    val jenis_sholat: String,
    val status: String, 
    val waktu_absen: String? = null
)

data class UserProfileResponse(
    val success: Boolean,
    val message: String,
    val data: UserData? = null
)

data class UserData(
    val id: Int,
    val nama: String,
    val nis: String,
    val jenis_kelamin: String, 
    val kelas: String,
    val email: String? = null,
    val no_hp: String? = null
)

data class StatisticsResponse(
    val message: String? = null,
    val data: StatisticsData? = null
)

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

data class StatistikAbsensiResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: StatisticsData? = null
)

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

data class HistorySiswaResponse(
    val message: String? = null,
    val data: HistorySiswaData? = null
)

data class HistorySiswaData(
    val siswa: SiswaInfo? = null,
    val periode: String = "",
    val start_date: String? = null,
    val end_date: String? = null,
    val statistik: HistoryStatistik? = null,
    val absensi: List<AbsensiHistoryItem>? = null
)

data class SiswaInfo(
    val nis: String,
    val nama_siswa: String,
    val kelas: String,
    val jurusan: String
)

data class HistoryStatistik(
    val total_absensi: Long,
    val total_hadir: Long,
    val total_izin: Long,
    val total_sakit: Long,
    val total_alpha: Long,
    val persentase_kehadiran: Double
)

data class AbsensiHistoryItem(
    @SerializedName("id_absen")
    val id: Int = 0,
    val tanggal: String = "",
    val hari: String? = null,
    @SerializedName("jenis_sholat")
    val jenis_sholat: String? = null,
    val status: String = "ALPHA",
    val waktu_absen: String? = null
) {
    fun getPrayerName(): String = jenis_sholat ?: "Unknown"
}

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

data class PaginationInfo(
    val page: Int = 1,
    val limit: Int = 20,
    @SerializedName("total_pages")
    val totalPages: Int = 1,
    @SerializedName("total")
    val totalItems: Int = 0
)

data class HistoryFilters(
    val start_date: String? = null,
    val end_date: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    val nis: String? = null,
    val status: String? = null
)

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

data class StatistikStaffData(
    val total_siswa: Int = 0,
    val total_hadir: Int = 0,
    val total_izin: Int = 0,
    val total_sakit: Int = 0,
    val total_alpha: Int = 0
)

data class DuhaTodayResponse(
    val message: String? = null,
    val data: List<DuhaJurusanData> = emptyList()
)

data class DuhaJurusanData(
    val jurusan: String,
    val jadwal: List<DuhaJadwalData>
)

data class DuhaJadwalData(
    val id_jadwal: Int,
    val hari: String,
    val jurusan: String? = null,
    val created_at: String? = null,
    @SerializedName("waktu_sholat")
    val waktuSholat: WaktuSholatData? = null
) {
    val jenis_sholat: String
        get() = waktuSholat?.jenisSholat?.namaJenis ?: "Duha"
    val jam_mulai: String
        get() = waktuSholat?.waktuMulai ?: ""
    val jam_selesai: String
        get() = waktuSholat?.waktuSelesai ?: ""
}

data class GetNotificationsResponse(
    val message: String? = null,
    val data: List<NotificationItem> = emptyList(),
    val pagination: NotificationPagination? = null
)

data class NotificationPagination(
    @SerializedName("current_page")
    val currentPage: Int = 1,
    @SerializedName("page_size")
    val pageSize: Int = 20,
    @SerializedName("total_records")
    val totalRecords: Int = 0,
    @SerializedName("total_pages")
    val totalPages: Int = 1
)

data class NotificationItem(
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    val title: String,
    val message: String,
    val type: String, // reminder, warning, info, success, failure
    val priority: String, // urgent, warning, info
    @SerializedName("delivery_type")
    val deliveryType: String, // toast, notification_center
    @SerializedName("is_read")
    val isRead: Boolean,
    @SerializedName("is_archived")
    val isArchived: Boolean,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("delivered_at")
    val deliveredAt: String? = null,
    @SerializedName("related_id")
    val relatedId: Int? = null
)

data class BulkReadRequest(
    val ids: List<Int>
)

data class BarcodeData(
    val barcode: String,
    val token: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("jenis_sholat")
    val jenisSholat: String,
    @SerializedName("id_jenis")
    val idJenis: Int
)

data class BarcodeVerifyRequest(
    val barcode: String,
    @SerializedName("hardware_id")
    val hardwareId: String
)


// Duha Turns - flat response {hari, jurusans[]}
data class DuhaTurnsResponse(
    val hari: String? = null,
    val jurusans: List<DuhaTurnJurusan> = emptyList()
)

data class DuhaTurnJurusan(
    @SerializedName("id_jurusan")
    val idJurusan: Int = 0,
    @SerializedName("nama_jurusan")
    val namaJurusan: String = "",
    @SerializedName("id_Duha_group")
    val idDuhaGroup: Int? = null,
    @SerializedName("hari_Duha")
    val hariDuha: String? = null,
    val schedules: List<DuhaTurnSchedule> = emptyList()
)

data class DuhaTurnSchedule(
    @SerializedName("id_jadwal")
    val idJadwal: Int = 0,
    @SerializedName("waktu_mulai")
    val waktuMulai: String = "",
    @SerializedName("waktu_selesai")
    val waktuSelesai: String = ""
)

// Synchronized SMKN 2 Singosari Spec Models
data class JadwalDuhaKeahlianResponse(
    val data: List<JadwalDuhaKeahlian> = emptyList()
)

data class JadwalDuhaKeahlian(
    val hari: String,
    val jurusan1: JurusanDuhaSchedule? = null,
    val jurusan2: JurusanDuhaSchedule? = null
)

data class SholatDuhaDetailResponse(
    val data: SholatDuhaDetail? = null
)

data class SholatDuhaDetail(
    val id: Int,
    val judul: String,
    val hari: String,
    @SerializedName("waktu_mulai")
    val waktuMulai: String? = null,
    @SerializedName("waktu_selesai")
    val waktuSelesai: String? = null,
    val kelas: String
)

data class SholatZuhurDetailResponse(
    val data: SholatZuhurDetail? = null
)

data class SholatZuhurDetail(
    val id: Int,
    val judul: String,
    val hari: String,
    @SerializedName("waktu_mulai")
    val waktuMulai: String? = null,
    @SerializedName("waktu_selesai")
    val waktuSelesai: String? = null,
    val jurusan: String,
    val kelas: String
)
