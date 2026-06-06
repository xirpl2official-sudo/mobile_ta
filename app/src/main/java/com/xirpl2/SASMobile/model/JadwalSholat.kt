package com.xirpl2.SASMobile.model


data class JadwalSholat(
    val id: Int = 0,
    val namaSholat: String,
    val jamMulai: String,
    val jamSelesai: String,
    val status: StatusSholat
)
