package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SiswaListResponse(
    val message: String,
    val data: List<SiswaItem> = emptyList(),
    val pagination: SiswaPaginationInfo? = null
)

data class SiswaItem(
    @SerializedName(value = "id_siswa", alternate = ["idSiswa"])
    val id_siswa: Int = 0,
    @SerializedName(value = "nis", alternate = ["NIS"])
    val nis: String = "",
    @SerializedName(value = "nama_siswa", alternate = ["namaSiswa", "name", "nama"])
    val nama_siswa: String = "",
    @SerializedName(value = "jk", alternate = ["jenis_kelamin", "jenisKelamin", "gender"])
    val jenis_kelamin: String = "",
    @SerializedName(value = "kelas", alternate = ["class"])
    val kelas: String = "",
    @SerializedName(value = "jurusan", alternate = ["major"])
    val jurusan: String = "",
    @SerializedName(value = "id_kelas")
    val id_kelas: Int? = null,
    @SerializedName(value = "id_jurusan")
    val id_jurusan: Int? = null,
    @SerializedName(value = "device_status", alternate = ["deviceStatus"])
    val deviceStatus: String? = null,
    @SerializedName(value = "hardware_id", alternate = ["hardwareId"])
    val hardwareId: String? = null,
    @SerializedName(value = "wali_kelas_name", alternate = ["waliKelasName", "wali_kelas", "waliKelas"])
    val waliKelasName: String? = null,
    @SerializedName(value = "status_akademik", alternate = ["statusAkademik", "status"])
    val statusAkademik: String? = null,
    @SerializedName(value = "agama", alternate = ["religion"])
    val agama: String? = null,
    val isSelected: Boolean = false
) : Serializable

data class SiswaPaginationInfo(
    val page: Int = 1,
    @SerializedName(value = "page_size", alternate = ["limit"])
    val page_size: Int = 100,
    val total_pages: Int = 1,
    @SerializedName(value = "total_items", alternate = ["total", "recordsTotal", "totalItems"])
    val total_items: Int = 0
)

data class CreateSiswaRequest(
    val nis: String,
    val nama_siswa: String,
    @SerializedName("jk")
    val jenis_kelamin: String,
    val id_kelas: Int? = null,
    val id_jurusan: Int? = null,
    val id_tahun_masuk: Int? = null,
    val agama: String? = null,
    val class_status: String? = null,
    val status_akademik: String? = null
)

data class UpdateSiswaRequest(
    val nama_siswa: String? = null,
    @SerializedName("jk")
    val jenis_kelamin: String? = null,
    val id_kelas: Int? = null,
    val id_jurusan: Int? = null,
    val id_tahun_masuk: Int? = null,
    val agama: String? = null,
    val class_status: String? = null,
    val status_akademik: String? = null
)

data class UpdateSiswaByNISRequest(
    val nis: String,
    val nama_siswa: String? = null,
    @SerializedName("jk")
    val jenis_kelamin: String? = null,
    val id_kelas: Int? = null,
    val id_jurusan: Int? = null,
    val id_tahun_masuk: Int? = null,
    val agama: String? = null,
    val class_status: String? = null,
    val status_akademik: String? = null
)

data class SiswaListPaginatedResponse(
    val message: String? = null,
    val data: List<SiswaItem> = emptyList(),
    @SerializedName(value = "pagination", alternate = ["meta"])
    val pagination: SiswaPaginationInfo? = null
)

data class StudentDetailResponse(
    val id_siswa: Int = 0,
    val nis: String = "",
    val nama_siswa: String = "",
    @SerializedName("jk")
    val jenis_kelamin: String = "",
    val kelas: String = "",
    val jurusan: String = "",
    val part: String = "",
    val agama: String = "",
    @SerializedName("wali_kelas_name")
    val waliKelasName: String? = null,
    @SerializedName("status_akademik")
    val statusAkademik: String? = null,
    @SerializedName("class_status")
    val classStatus: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("is_registered")
    val isRegistered: Boolean = false,
    @SerializedName("id_kelas")
    val idKelas: Int? = null,
    @SerializedName("id_jurusan")
    val idJurusan: Int? = null,
    @SerializedName("id_tahun_masuk")
    val idTahunMasuk: Int? = null,
    @SerializedName("id_account")
    val idAccount: Int? = null,
    @SerializedName("academic_year")
    val academicYear: String? = null,
    @SerializedName("current_semester")
    val currentSemester: Int? = null,
    @SerializedName("keterangan_promosi")
    val keteranganPromosi: String? = null,
    @SerializedName("last_promotion_at")
    val lastPromotionAt: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
