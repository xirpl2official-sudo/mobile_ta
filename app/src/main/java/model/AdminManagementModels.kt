package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class KelasManagementListResponse(
    val data: List<KelasManagementItem> = emptyList()
)

data class KelasManagementItem(
    val id_kelas: Int,
    val id_staff_wali: Int?,
    val jurusan: String,
    val label: String,
    val part: String,
    val siswa_count: Int,
    val tingkatan: Int,
    val wali_kelas: String?
)

data class KelasManagementDetailResponse(
    val data: KelasManagementDetail
)

data class KelasManagementDetail(
    val id_kelas: Int,
    val id_staff_wali: Int?,
    val jurusan: String,
    val label: String,
    val part: String,
    val students: List<SiswaItem> = emptyList(),
    val tingkatan: Int,
    val wali_kelas: String?
)

data class UpdateWaliKelasRequest(
    val id_staff: Int
)

data class CreateKelasRequest(
    val id_jurusan: Int,
    val tingkatan: Int,
    val label: String,
    val part: String? = null
)

data class MessageResponse(
    val status: Boolean? = null,
    val message: String
)
