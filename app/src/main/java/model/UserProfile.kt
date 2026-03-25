package com.xirpl2.SASMobile.model

data class UserProfile(
    val email: String,
    val is_google_acct: Boolean,
    val jk: String,
    val jurusan: String? = null,
    val kelas: String? = null,
    val nama_siswa: String? = null,
    val nis: String? = null,
    val token: String? = null,
    val name: String? = null,
    val username: String? = null,
    val role: String? = null
)
