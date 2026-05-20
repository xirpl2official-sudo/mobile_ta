package com.xirpl2.SASMobile.model

data class QRCodeGenerateResponse(
    val message: String,
    val data: QRCodeData
)

data class QRCodeData(
    val qr_code: String,
    val token: String,
    val expires_at: String,
    val jenis_sholat: String,
    val id_jenis: Int
)

data class QRCodeVerifyRequest(
    val token: String
)

data class QRCodeVerifyResponse(
    val message: String,
    val data: QRCodeVerifyData?
)

data class QRCodeVerifyData(
    val valid: Boolean,
    val nis: String,
    val nama_siswa: String,
    val kelas: String,
    val jurusan: String? = null,
    val jenis_sholat: String,
    val tanggal: String,
    val status: String
)
