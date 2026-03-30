package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Response model untuk API Siswa (/api/siswa)
 * API returns data as an array directly
 * Format: { "message": "...", "data": [...], "pagination": {...} }
 */
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

/**
 * Request body for creating new student
 */
data class CreateSiswaRequest(
    val nis: String,
    val nama_siswa: String,
    @SerializedName("jk")
    val jenis_kelamin: String,  // "L" or "P"
    val kelas: String,           // "X", "XI", "XII"
    val jurusan: String
)

/**
 * Request body for updating existing student
 */
data class UpdateSiswaRequest(
    val nama_siswa: String,
    @SerializedName("jk")
    val jenis_kelamin: String,  // "L" or "P"
    val kelas: String,           // "X", "XI", "XII"
    val jurusan: String
)
