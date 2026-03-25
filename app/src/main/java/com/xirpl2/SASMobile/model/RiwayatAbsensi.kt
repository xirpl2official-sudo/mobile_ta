package com.xirpl2.SASMobile.model

// Data class untuk Riwayat Absensi
data class RiwayatAbsensi(
    val tanggal: String,
    val namaSholat: String,
    val status: StatusAbsensi,
    val waktuAbsen: String? = null // Format: "HH:mm"
)
