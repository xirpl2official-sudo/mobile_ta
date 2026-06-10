package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    val message: String? = null,
    val data: AkunLoginResponse? = null
)

data class AkunLoginResponse(
    val id: Int? = null,
    @SerializedName("id_staff")
    val id_staff: Int? = null,
    val nis: String? = null,
    val nip: String? = null,
    val nama_siswa: String? = null,
    val nama: String? = null,
    val name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val is_google_acct: Boolean? = null,
    val jk: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null,
    @SerializedName("class_name")
    val class_name: String? = null,
    @SerializedName("id_kelas")
    val id_kelas: Int? = null,
    val role: String? = null,
    val is_verified: Boolean? = null,
    val token: String? = null,
    @SerializedName("refresh_token")
    val refresh_token: String? = null
) {
    fun getDisplayName(): String {
        return nama_siswa ?: nama ?: name ?: "User"
    }
    
    fun getIdentifier(): String {
        return nis ?: nip ?: username ?: ""
    }
    
    fun isStaff(): Boolean {
        return role?.lowercase() in listOf("admin", "guru", "wali_kelas", "wali kelas")
    }
}