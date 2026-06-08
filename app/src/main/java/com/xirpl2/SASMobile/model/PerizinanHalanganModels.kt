package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class RequestHalanganBody(
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String
)

data class RequestHalanganData(
    val id: Int,
    @SerializedName("qr_token")
    val qrToken: String,
    @SerializedName("status_validasi")
    val statusValidasi: String,
    @SerializedName("is_istihadhah")
    val isIstihadhah: Boolean,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String
)

data class ValidateHalanganBody(
    @SerializedName("qr_token")
    val qrToken: String,
    val status: String,
    val catatan: String?
)

data class HalanganStatusData(
    val active: Boolean,
    val perizinan: HalanganPerizinan?
)

data class HalanganPerizinan(
    val id: Int,
    @SerializedName("siswa_id")
    val siswaId: Int,
    @SerializedName("guru_id")
    val guruId: Int?,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    @SerializedName("status_validasi")
    val statusValidasi: String,
    @SerializedName("qr_code_token")
    val qrCodeToken: String,
    @SerializedName("catatan_guru")
    val catatanGuru: String?,
    val siswa: SiswaItem?,
    val guru: StaffInfo?
)
