package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class RequestHalanganBody(
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String
)

data class RequestHalanganData(
    val id: Int,
    @SerializedName("status_validasi")
    val statusValidasi: String,
    @SerializedName("is_istihadhah")
    val isIstihadhah: Boolean,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String
)

data class VerifyHalanganBody(
    @SerializedName("token")
    val halanganToken: String
)

data class HalanganStatusData(
    val active: Boolean,
    val perizinan: HalanganPerizinan?
)

data class HalanganPerizinan(
    val id: Int,
    @SerializedName("siswa_id")
    val siswaId: Int,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    @SerializedName("status_validasi")
    val statusValidasi: String
)
