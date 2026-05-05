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
    val jurusan: String = ""
) : Serializable

data class SiswaPaginationInfo(
    val page: Int = 1,
    val page_size: Int = 100,
    val total_pages: Int = 1,
    val total_items: Int = 0
)

data class CreateSiswaRequest(
    val nis: String,
    val nama_siswa: String,
    @SerializedName("jk")
    val jenis_kelamin: String,  
    val kelas: String,           
    val jurusan: String
)

data class UpdateSiswaRequest(
    val nama_siswa: String,
    @SerializedName("jk")
    val jenis_kelamin: String,  
    val kelas: String,           
    val jurusan: String
)
