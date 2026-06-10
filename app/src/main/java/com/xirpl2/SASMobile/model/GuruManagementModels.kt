package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

// --- Guru Management Models ---

data class GuruListResponse(
    val data: List<GuruItem> = emptyList(),
    val meta: PaginationMeta? = null
)

data class PaginationMeta(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val total_pages: Int = 1
)

data class GuruDetailResponse(
    val data: GuruItem? = null
)

data class GuruItem(
    @SerializedName("id_staff")
    val id_staff: Int,
    @SerializedName("id_account")
    val id_account: Int,
    @SerializedName("nama")
    val nama: String,
    @SerializedName("nip")
    val nip: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("wali_kelas")
    val wali_kelas: String = "",
    @SerializedName("id_kelas_wali")
    val id_kelas_wali: Int? = null,
    @SerializedName("label_kelas")
    val label_kelas: String = "",
    @SerializedName("berlaku_mulai")
    val berlaku_mulai: String? = null
)

data class CreateGuruRequest(
    val email: String,
    val password: String,
    val nama: String,
    val nip: String
)

data class UpdateGuruRequest(
    val email: String? = null,
    val nama: String? = null,
    val nip: String? = null
)

data class AssignWaliKelasGuruRequest(
    val id_kelas: Int
)

// --- Wali Kelas Management Models ---

data class WaliKelasManagementListResponse(
    val data: List<WaliKelasManagementItem> = emptyList(),
    val meta: PaginationMeta? = null
)

data class WaliKelasManagementItem(
    val id_wali: Int,
    val id_kelas: Int,
    val kelas_label: String = "",
    val tingkatan: Int = 0,
    val jurusan: String = "",
    val part: String = "",
    val id_staff: Int,
    val nama_guru: String = "",
    val nip: String = "",
    val berlaku_mulai: String = "",
    val is_active: Boolean = true
)
