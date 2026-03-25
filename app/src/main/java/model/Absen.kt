package com.example.sas_mobile.model

data class Absen(
    val id_absen: Int,
    val id_siswa: String,
    val id_sholat: Int,
    val status: String,
    val deskripsi: String,
    val created_at: String
)