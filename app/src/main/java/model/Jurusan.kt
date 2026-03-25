package model

/**
 * Model data untuk Jurusan/Program Keahlian
 */
data class Jurusan(
    val nama: String,
    val namaLengkap: String,
    val warna: String, // Hex color code (e.g., "#E91E63")
    val jumlahSiswa: Int = 0 // Opsional, untuk data dari API nanti
)