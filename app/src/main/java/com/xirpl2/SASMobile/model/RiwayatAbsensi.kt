package com.xirpl2.SASMobile.model


data class RiwayatAbsensi(
    val tanggal: String,
    val namaSholat: String,
    val status: StatusAbsensi,
    val waktuAbsen: String? = null 
)
