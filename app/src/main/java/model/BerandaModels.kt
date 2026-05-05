package com.xirpl2.SASMobile.model
import com.google.gson.annotations.SerializedName

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
    var hari: String? = null, 
    var jurusan: String? = null, 
    var kelas: String? = null 
)

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
    val kelas: String? = null,
    val created_at: String? = null
)

data class JadwalSholatUpdateRequest(
    val jenis_sholat: String? = null,
    @SerializedName("waktu_mulai")
    val jam_mulai: String? = null,
    @SerializedName("waktu_selesai")
    val jam_selesai: String? = null,
    val hari: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null
)

data class JadwalSholatCreateRequest(
    val jenis_sholat: String,
    @SerializedName("waktu_mulai")
    val jam_mulai: String,
    @SerializedName("waktu_selesai")
    val jam_selesai: String,
    val hari: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null
)



data class RiwayatAbsensiResponse(
    val success: Boolean,
    val message: String,
    val data: List<RiwayatAbsensiData>
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
    val data: UserData
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
    val message: String,
    val data: StatisticsData
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

data class AbsensiRequest(
    val jadwal_sholat_id: Int,
    val latitude: Double,
    val longitude: Double,
    val foto: String? = null 
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
    val total_pages: Int = 1,
    val total_items: Int = 0
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
