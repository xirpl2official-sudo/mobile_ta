package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class GenerateCodeResponse(
    val message: String,
    val data: AttendanceCodeData
)

data class AttendanceCodeData(
    val code: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("id_template")
    val idTemplate: Int?,
    @SerializedName("jenis_sholat")
    val jenisSholat: String?,
    @SerializedName("id_jenis")
    val idJenis: Int?
)

data class VerifyCodeRequest(
    val code: String
)

data class AttendanceCreateRequest(
    val id_jadwal: Int,
    val tanggal: String,
    val status: String,
    val deskripsi: String? = null
)
