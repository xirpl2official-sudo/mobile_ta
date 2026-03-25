package com.xirpl2.SASMobile

import model.Jurusan

/**
 * Helper class untuk data Jurusan
 * Berisi data dummy dan warna untuk setiap jurusan
 */
object JurusanHelper {
    
    /**
     * Mendapatkan list semua jurusan dengan warna khas masing-masing
     * Warna diambil dari logo jurusan
     */
    fun getAllJurusan(): List<Jurusan> {
        return listOf(
            Jurusan(
                nama = "Mekatronika",
                namaLengkap = "Mekatronika",
                warna = "#4CAF50" // Hijau
            ),
            Jurusan(
                nama = "TKJ",
                namaLengkap = "Teknik Komputer dan Jaringan",
                warna = "#FFC107" // Kuning/Gold
            ),
            Jurusan(
                nama = "TEI",
                namaLengkap = "Teknik Elektronika Industri",
                warna = "#4CAF50" // Hijau
            ),
            Jurusan(
                nama = "Animasi",
                namaLengkap = "Animasi",
                warna = "#E91E63" // Pink/Magenta
            ),
            Jurusan(
                nama = "TAV",
                namaLengkap = "Teknik Audio Video",
                warna = "#81C784" // Hijau Muda
            ),
            Jurusan(
                nama = "DKV",
                namaLengkap = "Desain Komunikasi Visual",
                warna = "#2196F3" // Biru
            ),
            Jurusan(
                nama = "Broadcasting",
                namaLengkap = "Broadcasting",
                warna = "#F44336" // Merah
            ),
            Jurusan(
                nama = "RPL",
                namaLengkap = "Rekayasa Perangkat Lunak",
                warna = "#FF9800" // Orange
            )
        )
    }
}
