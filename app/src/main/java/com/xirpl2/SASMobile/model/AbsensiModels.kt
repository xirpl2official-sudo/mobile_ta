package com.xirpl2.SASMobile.model

data class CreateAbsensiRequest(
    val id_jadwal: Int,
    val status: String,
    val tanggal: String,
    val deskripsi: String? = null
)

data class ManualAbsensiResponse(
    val id_absen: Int,
    val id_jadwal: Int?,
    val nis: String,
    val tanggal: String,
    val status: String,
    val deskripsi: String?,
    val created_at: String
)
