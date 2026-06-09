package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

// --- FASE 3.1: Analytics Models ---

data class PendingAttendanceResponse(
    val message: String? = null,
    val data: List<PendingAttendanceItem> = emptyList()
)

data class PendingAttendanceItem(
    val nis: String,
    val nama_siswa: String,
    val kelas: String,
    val jurusan: String,
    val tanggal: String,
    val jenis_sholat: String,
    val status: String
)

// --- FASE 3.3: Student Update Status ---

data class UpdateStatusRequest(
    @SerializedName("status_akademik")
    val status: String
)

// --- FASE 3.4: Admin Student Control Models ---

data class StudentTransitionsResponse(
    val message: String? = null,
    val data: List<StudentTransition> = emptyList()
)

data class StudentTransition(
    val nis: String,
    val nama_siswa: String,
    val kelas_sekarang: String,
    val kelas_tujuan: String,
    val status: String
)

data class BulkProgressionRequest(
    @SerializedName("nis_list")
    val student_ids: List<String>,
    @SerializedName("target_class")
    val target_kelas: String,
    val action: String,
    val note: String? = null
)

data class BulkFieldsRequest(
    @SerializedName("nis_list")
    val student_ids: List<String>,
    val kelas: String? = null,
    val jurusan: String? = null,
    @SerializedName("status_akademik")
    val statusAkademik: String? = null,
    @SerializedName("id_tahun_masuk")
    val idTahunMasuk: Int? = null
)

data class AnnualRolloverRequest(
    val tahun_ajaran_baru: String
)

data class SequentialProgressionRequest(
    val start_kelas: String,
    val end_kelas: String
)

// --- FASE 3.5: Promotion Models (sudah ada PromotionRequest & PromotionResponse di tempat lain) ---

// --- FASE 3.6: Academic Years Models ---

data class AcademicYearListResponse(
    val message: String? = null,
    val data: List<AcademicYear> = emptyList(),
    val meta: PaginationMeta? = null
)

data class AcademicYearResponse(
    val message: String? = null,
    val data: AcademicYear? = null
)

data class AcademicYear(
    @SerializedName("id_tahun_masuk")
    val id: Int,
    val tahun: String,
    val is_active: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class AcademicYearRequest(
    val tahun: String,
    val is_active: Boolean = false
)

// --- FASE 3.8: Device Management Models ---

data class DeviceManagementListResponse(
    val message: String? = null,
    val data: List<DeviceManagementItem> = emptyList()
)

data class DeviceManagementItem(
    val id: Int,
    @SerializedName("account_id")
    val user_id: Int,
    val user_name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val hardware_id: String,
    val device_name: String?,
    val device_model: String?,
    val os_version: String?,
    val is_verified: Boolean,
    val last_auth_at: String?,
    val created_at: String
)

data class DeviceChangeRequestListResponse(
    val message: String? = null,
    val data: List<DeviceChangeRequestItem> = emptyList(),
    val count: Int = 0
)

data class DeviceChangeRequestItem(
    val id: Int,
    @SerializedName("account_id")
    val user_id: Int,
    @SerializedName("old_hardware_id")
    val hardware_id_lama: String,
    @SerializedName("new_hardware_id")
    val hardware_id_baru: String,
    @SerializedName("alasan")
    val reason: String?,
    val status: String, // pending, approved, rejected
    val created_at: String
)

// --- FASE 3.10: Class Teachers Models ---

data class ClassTeacherListResponse(
    val message: String? = null,
    val data: List<ClassTeacher> = emptyList()
)

data class ClassTeacherResponse(
    val message: String? = null,
    val data: ClassTeacher? = null
)

data class ClassTeacher(
    @SerializedName("id_class_teacher")
    val id: Int,
    val id_staff: Int,
    val id_kelas: Int,
    val nama_staff: String,
    val nama_kelas: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class ClassTeacherRequest(
    val id_staff: Int,
    val id_kelas: Int
)

// --- FASE 3.11: Prayer Types Models ---

data class PrayerTypeListResponse(
    val message: String? = null,
    val data: List<PrayerType> = emptyList()
)

data class PrayerTypeResponse(
    val message: String? = null,
    val data: PrayerType? = null
)

data class PrayerType(
    @SerializedName("id_jenis")
    val id: Int,
    val nama_jenis: String,
    val urutan: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PrayerTypeRequest(
    val nama_jenis: String,
    val butuh_giliran: Boolean = false
)

// --- FASE 3.12: Prayer Times Models ---

data class PrayerTimeListResponse(
    val message: String? = null,
    val data: List<PrayerTime> = emptyList()
)

data class PrayerTimeResponse(
    val message: String? = null,
    val data: PrayerTime? = null
)

data class PrayerTime(
    @SerializedName("id_waktu")
    val id: Int,
    @SerializedName("id_jenis")
    val id_jenis_sholat: Int,
    val waktu_mulai: String,
    val waktu_selesai: String,
    @SerializedName("jenis_sholat")
    val jenisSholat: JenisSholatData? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PrayerTimeRequest(
    @SerializedName("id_jenis")
    val id_jenis_sholat: Int,
    val waktu_mulai: String,
    val waktu_selesai: String,
    val berlaku_mulai: String
)

// --- FASE 3.13: Duha & Jurusan Models ---

data class JurusanDuhaSchedulesResponse(
    val message: String? = null,
    val data: List<JurusanDuhaSchedule> = emptyList()
)

data class JurusanDuhaSchedule(
    val id_jurusan: Int,
    val nama_jurusan: String,
    val hari_Duha: String?
)

data class DuhaDayRequest(
    val hari_Duha: String
)

data class JadwalDuhaTimeUpdateRequest(
    val waktu_mulai: String,
    val waktu_selesai: String,
    val hari: String
)

data class JadwalDuhaKeahlianUpdateRequest(
    @SerializedName("hari")
    val hari: String,
    @SerializedName("id_jurusan")
    val idJurusan: Int,
    @SerializedName("waktu_mulai")
    val waktuMulai: String,
    @SerializedName("waktu_selesai")
    val waktuSelesai: String
)

data class DuhaGroupListResponse(
    val message: String? = null,
    val data: List<DuhaGroup> = emptyList()
)

data class DuhaGroupResponse(
    val message: String? = null,
    val data: DuhaGroup? = null
)

data class DuhaGroup(
    @SerializedName("id_giliran")
    val id: Int,
    val hari: String,
    val jurusan: String,
    @SerializedName("id_jurusan")
    val idJurusan: Int,
    val students: List<String> = emptyList()
)

data class DuhaGroupRequest(
    val hari: String,
    val jurusan: String,
    @SerializedName("id_jurusan")
    val idJurusan: Int,
    val students: List<String> = emptyList()
)

data class WeeklyDuhaGroupRequest(
    val groups: List<DuhaGroupRequest>
)

// --- FASE 3.16: Data Retention Models ---

data class BackupStatusResponse(
    val message: String? = null,
    @SerializedName("has_pending")
    val hasPending: Boolean = false,
    @SerializedName("pending_ranges")
    val pendingRanges: List<BackupPendingRange> = emptyList(),
    @SerializedName("recent_backups")
    val recentBackups: List<BackupItem> = emptyList()
)

data class BackupPendingRange(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val count: Int = 0
)

data class BackupItem(
    val id: Int,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("file_format")
    val fileFormat: String,
    @SerializedName("exported_at")
    val exportedAt: String,
    @SerializedName("auto_delete_after")
    val autoDeleteAfter: String,
    @SerializedName("deleted_at")
    val deletedAt: String? = null,
    @SerializedName("created_by")
    val createdBy: Int? = null
)

data class BackupConfirmRequest(
    @SerializedName("backup_id")
    val backupId: Int,
    val confirmed: Boolean
)

// --- Students Tambahan: Typed Requests ---

data class NotifyWaliKelasRequest(
    @SerializedName("nis_list")
    val nisList: List<String>,
    val message: String
)

data class ImportStudentItem(
    val nis: String,
    val nama_siswa: String,
    val jk: String,
    val agama: String? = null,
    val tingkatan: String? = null,
    val jurusan: String? = null,
    val part: String? = null
)

data class ImportStudentsRequest(
    val students: List<ImportStudentItem>
)
