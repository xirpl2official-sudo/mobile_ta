package com.xirpl2.SASMobile.model

data class QRCodeGenerateResponse(
    val message: String,
    val data: QRCodeData
)

data class QRCodeData(
    val qr_code: String,           
    val token: String,             
    val expires_at: String,        
    val jenis_sholat: String,      
    val id_jadwal: Int             
)

data class QRCodeVerifyRequest(
    val token: String              
)

data class QRCodeVerifyResponse(
    val message: String,
    val data: QRCodeVerifyData?
)

data class QRCodeVerifyData(
    val siswa: SiswaVerifyInfo,    
    val absensi: AbsensiVerifyInfo 
)

data class SiswaVerifyInfo(
    val id: Int,
    val nis: String,
    val nama: String,
    val kelas: String,
    val jurusan: String? = null
)

data class AbsensiVerifyInfo(
    val id: Int,
    val id_jadwal: Int,
    val jenis_sholat: String,
    val tanggal: String,
    val waktu_absen: String,
    val status: String             
)
