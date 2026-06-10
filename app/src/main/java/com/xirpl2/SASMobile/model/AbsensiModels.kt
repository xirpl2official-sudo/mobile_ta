package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class CreateAbsensiRequest(
    @SerializedName("id_jenis")
    val id_jenis: Int,
    val status: String,
    val tanggal: String
)

data class ManualAbsensiResponse(
    val id_absen: Int,
    @SerializedName("id_jenis")
    val id_jenis: Int?,
    @SerializedName("id_siswa")
    val id_siswa: Int?,
    val tanggal: String,
    val status: String
)
