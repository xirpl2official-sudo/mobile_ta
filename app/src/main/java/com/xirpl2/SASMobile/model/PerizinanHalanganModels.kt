package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class VerifyHalanganBody(
    @SerializedName("token")
    val halanganToken: String
)

data class HalanganQRGenerateResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: HalanganQRData?
)

data class HalanganQRData(
    @SerializedName("qr_code")
    val qrCode: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("expires_at")
    val expiresAt: String,
    @SerializedName("jenis")
    val jenis: String
)

data class HalanganVerifyResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: HalanganVerifyData?
)

data class HalanganVerifyData(
    @SerializedName("id_halangan")
    val idHalangan: Int,
    @SerializedName("nama_siswa")
    val namaSiswa: String,
    @SerializedName("kelas")
    val kelas: String,
    @SerializedName("jurusan")
    val jurusan: String,
    @SerializedName("tanggal")
    val tanggal: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val msg: String? = null
)

data class HalanganPendingItem(
    @SerializedName("id_halangan")
    val idHalangan: Int,
    val nis: String,
    @SerializedName("nama_siswa")
    val namaSiswa: String,
    val kelas: String,
    val jurusan: String,
    val jk: String,
    val tanggal: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class HalanganPendingListResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<HalanganPendingItem>?
)
