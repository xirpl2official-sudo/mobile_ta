package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class TahunAjaranListResponse(
    val data: List<TahunAjaranData> = emptyList()
)

data class TahunAjaranResponse(
    val data: TahunAjaranData? = null
)

data class TahunAjaranData(
    val id: Int,
    @SerializedName("is_current")
    val isCurrent: Boolean,
    @SerializedName("status_promosi")
    val statusPromosi: String,
    val tahun: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String?
)

data class TahunAjaranRequest(
    val tahun: String,
    @SerializedName("is_current")
    val isCurrent: Boolean? = null,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String? = null
)

data class PromotionRequest(
    val exceptions: List<PromotionException> = emptyList()
)

data class PromotionException(
    val nis: String,
    val type: String, // mutasi, keluar, tetap
    val reason: String? = null
)

data class PromotionResponse(
    val status: String,
    val message: String,
    val data: PromotionDetail? = null
)

data class PromotionDetail(
    @SerializedName("log_id")
    val logId: Int,
    @SerializedName("promoted_count")
    val promotedCount: Int,
    @SerializedName("graduated_count")
    val graduatedCount: Int,
    @SerializedName("mutasi_count")
    val mutasiCount: Int,
    @SerializedName("keluar_count")
    val keluarCount: Int,
    @SerializedName("tetap_count")
    val tetapCount: Int
)

data class StudentControlOverviewResponse(
    val data: StudentControlOverview? = null
)

data class StudentControlOverview(
    @SerializedName("active_students")
    val activeStudents: Int,
    @SerializedName("graduated_students")
    val graduatedStudents: Int,
    @SerializedName("total_classes")
    val totalClasses: Int,
    @SerializedName("current_academic_year")
    val currentAcademicYear: String?
)
