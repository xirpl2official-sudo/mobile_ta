package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class RequestHalanganBody(
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String
)

data class RequestHalanganData(
    val id: Int,
    @SerializedName("status_validasi")
    val statusValidasi: String,
    @SerializedName("is_istihadhah")
    val isIstihadhah: Boolean,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String
)

data class VerifyHalanganBody(
    @SerializedName("halangan_id")
    val halanganId: Int
)

data class HalanganStatusData(
    val active: Boolean,
    val perizinan: HalanganPerizinan?
)

data class HalanganPerizinan(
    val id: Int,
    @SerializedName("siswa_id")
    val siswaId: Int,
    @SerializedName("guru_id")
    val guruId: Int?,
    @SerializedName("tanggal_mulai")
    val tanggalMulai: String,
    @SerializedName("tanggal_selesai")
    val tanggalSelesai: String,
    @SerializedName("status_validasi")
    val statusValidasi: String,
    @SerializedName("catatan_guru")
    val catatanGuru: String?,
    val siswa: PendingSiswaInfo?
)

data class PendingSiswaInfo(
    @SerializedName("nama_siswa")
    val namaSiswa: String?,
    val nis: String?,
    val kelas: String?,
    val jurusan: String?
)
