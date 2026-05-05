package com.xirpl2.SASMobile

import model.Jurusan

object JurusanHelper {
    
    fun getAllJurusan(): List<Jurusan> {
        return listOf(
            Jurusan(
                nama = "Mekatronika",
                namaLengkap = "Mekatronika",
                warna = "#4CAF50" 
            ),
            Jurusan(
                nama = "TKJ",
                namaLengkap = "Teknik Komputer dan Jaringan",
                warna = "#FFC107" 
            ),
            Jurusan(
                nama = "TEI",
                namaLengkap = "Teknik Elektronika Industri",
                warna = "#4CAF50" 
            ),
            Jurusan(
                nama = "Animasi",
                namaLengkap = "Animasi",
                warna = "#E91E63" 
            ),
            Jurusan(
                nama = "TAV",
                namaLengkap = "Teknik Audio Video",
                warna = "#81C784" 
            ),
            Jurusan(
                nama = "DKV",
                namaLengkap = "Desain Komunikasi Visual",
                warna = "#2196F3" 
            ),
            Jurusan(
                nama = "Broadcasting",
                namaLengkap = "Broadcasting",
                warna = "#F44336" 
            ),
            Jurusan(
                nama = "RPL",
                namaLengkap = "Rekayasa Perangkat Lunak",
                warna = "#FF9800" 
            )
        )
    }
}
