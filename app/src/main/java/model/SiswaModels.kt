package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SiswaListResponse(
    val message: String,
    val data: List<SiswaItem> = emptyList(),
    val pagination: SiswaPaginationInfo? = null
)

data class SiswaItem(
    val id_siswa: Int = 0,
    val nis: String = "",
    val nama_siswa: String = "",
    @SerializedName("jk")
    val jenis_kelamin: String = "",
    val kelas: String = "",
    val jurusan: String = "",
    @SerializedName("device_status")
    val deviceStatus: String? = null,
    @SerializedName("hardware_id")
    val hardwareId: String? = null,
    @SerializedName("wali_kelas_name")
    val waliKelasName: String? = null,
    @SerializedName("status_akademik")
    val statusAkademik: String? = null
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
    val kelas: String? = null,
    val jurusan: String? = null,
    val id_kelas: Int? = null,
    val id_jurusan: Int? = null,
    val id_tahun_masuk: Int? = null,
    val class_status: String? = null,
    val status_akademik: String? = null
)

data class UpdateSiswaRequest(
    val nama_siswa: String? = null,
    @SerializedName("jk")
    val jenis_kelamin: String? = null,
    val kelas: String? = null,
    val jurusan: String? = null,
    val id_kelas: Int? = null,
    val id_jurusan: Int? = null,
    val id_tahun_masuk: Int? = null,
    val class_status: String? = null,
    val status_akademik: String? = null
)

data class SiswaListPaginatedResponse(
    val message: String? = null,
    val data: List<SiswaItem> = emptyList(),
    @SerializedName(value = "pagination", alternate = ["meta"])
    val pagination: SiswaPaginationInfo? = null,
    val filters: Map<String, Any>? = null
)
