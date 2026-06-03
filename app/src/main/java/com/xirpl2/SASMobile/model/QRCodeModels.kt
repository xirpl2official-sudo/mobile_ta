package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class QRCodeGenerateResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: QRCodeData? = null
)

data class QRCodeData(
    @SerializedName("qr_code")
    val qr_code: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("expires_at")
    val expires_at: String,
    @SerializedName("jenis_sholat")
    val jenis_sholat: String,
    @SerializedName("id_jenis")
    val id_jenis: Int
)

data class QRCodeVerifyRequest(
    @SerializedName("token")
    val token: String
)

data class QRCodeVerifyResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: QRCodeVerifyData? = null
)

data class QRCodeVerifyData(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("nis")
    val nis: String,
    @SerializedName("nama_siswa")
    val nama_siswa: String,
    @SerializedName("kelas")
    val kelas: String,
    @SerializedName("jurusan")
    val jurusan: String? = null,
    @SerializedName("jenis_sholat")
    val jenis_sholat: String,
    @SerializedName("tanggal")
    val tanggal: String,
    @SerializedName("status")
    val status: String
)
