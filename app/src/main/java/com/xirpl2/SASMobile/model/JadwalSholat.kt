package com.xirpl2.SASMobile.model

// Data class untuk Jadwal Sholat
data class JadwalSholat(
    val id: Int = 0,
    val namaSholat: String,
    val jamMulai: String,
    val jamSelesai: String,
    val status: StatusSholat
)
