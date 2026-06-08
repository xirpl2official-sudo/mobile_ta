package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class FemaleRestrictionStatusResponse(
    val message: String,
    val data: FemaleRestrictionStatus? = null
)

data class FemaleRestrictionStatus(
    @SerializedName("is_restricted")
    val isRestricted: Boolean = false,
    val restriction: FemaleRestriction? = null,
    @SerializedName("pending_request")
    val pendingRequest: ApprovalRequest? = null
)

data class FemaleRestriction(
    val id: Int = 0,
    @SerializedName("id_siswa")
    val idSiswa: Int = 0,
    @SerializedName("restricted_at")
    val restrictedAt: String = "",
    @SerializedName("expires_at")
    val expiresAt: String = "",
    val status: String = "active",
    @SerializedName("remaining_days")
    val remainingDays: Int = 0
)

data class ApprovalRequest(
    val id: Int = 0,
    @SerializedName("id_restriction")
    val idRestriction: Int = 0,
    @SerializedName("id_siswa")
    val idSiswa: Int = 0,
    @SerializedName("siswa_name")
    val siswaName: String? = null,
    @SerializedName("siswa_nis")
    val siswaNis: String? = null,
    @SerializedName("siswa_kelas")
    val siswaKelas: String? = null,
    @SerializedName("siswa_jurusan")
    val siswaJurusan: String? = null,
    val status: String = "pending",
    val catatan: String? = null,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = "",
    @SerializedName("approver_name")
    val approverName: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null
)

data class ApprovalRequestListResponse(
    val message: String,
    val data: List<ApprovalRequest> = emptyList()
)

data class SubmitApprovalRequest(
    @SerializedName("id_guru_approver")
    val idGuruApprover: Int,
    val catatan: String? = null
)

data class ProcessApprovalRequest(
    val status: String,
    val catatan: String? = null
)

data class FemaleTeacherInfo(
    @SerializedName("id_staff")
    val idStaff: Int,
    val nama: String,
    val nip: String? = null,
    @SerializedName("jenis_kelamin")
    val jenisKelamin: String = ""
)

data class FemaleTeacherListResponse(
    val message: String,
    val data: List<FemaleTeacherInfo> = emptyList()
)

data class RestrictionHistoryResponse(
    val message: String,
    val data: List<FemaleRestriction> = emptyList()
)
