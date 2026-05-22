package com.xirpl2.SASMobile.model


data class RiwayatAbsensi(
    val id: Int = 0,
    val tanggal: String,
    val namaSholat: String,
    val status: StatusAbsensi,
    val waktuAbsen: String? = null
)
