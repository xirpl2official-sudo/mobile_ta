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
    val status: String // aktif, nonaktif, alumni, keluar
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
    val student_ids: List<String>,
    val target_kelas: String
)

data class BulkFieldsRequest(
    val student_ids: List<String>,
    val fields: Map<String, Any>
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
    val data: List<AcademicYear> = emptyList()
)

data class AcademicYearResponse(
    val message: String? = null,
    val data: AcademicYear
)

data class AcademicYear(
    @SerializedName("id_tahun_ajaran")
    val id: Int,
    val tahun_mulai: String,
    val tahun_selesai: String,
    val is_active: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class AcademicYearRequest(
    val tahun_mulai: String,
    val tahun_selesai: String,
    val is_active: Boolean = false
)

// --- FASE 3.8: Device Management Models ---

data class DeviceManagementListResponse(
    val message: String? = null,
    val data: List<DeviceManagementItem> = emptyList()
)

data class DeviceManagementItem(
    val id: Int,
    val user_id: Int,
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
    val data: List<DeviceChangeRequestItem> = emptyList()
)

data class DeviceChangeRequestItem(
    val id: Int,
    val user_id: Int,
    val hardware_id_lama: String,
    val hardware_id_baru: String,
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
    val data: ClassTeacher
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
    val data: PrayerType
)

data class PrayerType(
    @SerializedName("id_jenis_sholat")
    val id: Int,
    val nama_jenis: String,
    val urutan: Int,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PrayerTypeRequest(
    val nama_jenis: String,
    val urutan: Int
)

// --- FASE 3.12: Prayer Times Models ---

data class PrayerTimeListResponse(
    val message: String? = null,
    val data: List<PrayerTime> = emptyList()
)

data class PrayerTimeResponse(
    val message: String? = null,
    val data: PrayerTime
)

data class PrayerTime(
    @SerializedName("id_waktu_sholat")
    val id: Int,
    val id_jenis_sholat: Int,
    val waktu_mulai: String,
    val waktu_selesai: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class PrayerTimeRequest(
    val id_jenis_sholat: Int,
    val waktu_mulai: String,
    val waktu_selesai: String
)

// --- FASE 3.13: Dhuha & Jurusan Models ---

data class JurusanDhuhaSchedulesResponse(
    val message: String? = null,
    val data: List<JurusanDhuhaSchedule> = emptyList()
)

data class JurusanDhuhaSchedule(
    val id_jurusan: Int,
    val nama_jurusan: String,
    val hari_dhuha: String?
)

data class DhuhaDayRequest(
    val hari_dhuha: String
)

data class DhuhaGroupListResponse(
    val message: String? = null,
    val data: List<DhuhaGroup> = emptyList()
)

data class DhuhaGroupResponse(
    val message: String? = null,
    val data: DhuhaGroup
)

data class DhuhaGroup(
    @SerializedName("id_group")
    val id: Int,
    val nama_group: String,
    val jadwal: List<DhuhaJadwalItem>
)

data class DhuhaJadwalItem(
    val hari: String,
    val jurusan: List<String>
)

data class DhuhaGroupRequest(
    val nama_group: String,
    val jadwal: List<DhuhaJadwalItem>
)

data class WeeklyDhuhaGroupRequest(
    val groups: List<Map<String, Any>>
)

// --- FASE 3.16: Data Retention Models ---

data class BackupStatusResponse(
    val message: String? = null,
    val data: BackupStatus
)

data class BackupStatus(
    val last_backup_at: String?,
    val next_backup_at: String?,
    val backup_size_mb: Double?,
    val status: String // pending, completed, failed
)

data class BackupConfirmRequest(
    val backup_id: String,
    val confirmed: Boolean
)
