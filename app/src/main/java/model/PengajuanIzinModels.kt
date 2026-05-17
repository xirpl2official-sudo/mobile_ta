package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class CreatePengajuanIzinRequest(
    @SerializedName("jenis_izin")
    val jenisIzin: String, // izin, sakit
    val keterangan: String,
    @SerializedName("tanggal_awal")
    val tanggalAwal: String,
    @SerializedName("tanggal_akhir")
    val tanggalAkhir: String
)

data class PengajuanIzinList(
    val message: String,
    val data: List<PengajuanIzin> = emptyList(),
    val pagination: PaginationInfo? = null
)

data class PengajuanIzin(
    val id_pengajuan: Int,
    val id_siswa: Int,
    @SerializedName("jenis_izin")
    val jenisIzin: String,
    val keterangan: String,
    @SerializedName("tanggal_awal")
    val tanggalAwal: String,
    @SerializedName("tanggal_akhir")
    val tanggalAkhir: String,
    val status: String, // pending, disetujui, ditolak
    @SerializedName("catatan_verifikasi")
    val catatanVerifikasi: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val siswa: SiswaItem? = null,
    @SerializedName("staff_approver")
    val staffApprover: StaffInfo? = null,
    @SerializedName("bukti_foto")
    val buktiFoto: String? = null
)

data class StaffInfo(
    val id_staff: Int,
    val nama: String,
    val nip: String? = null
)

data class UpdateStatusPengajuanIzinRequest(
    val id_pengajuan: Int,
    val status: String, // disetujui, ditolak
    @SerializedName("catatan_verifikasi")
    val catatanVerifikasi: String? = null
)
