package com.xirpl2.SASMobile.model

data class AkunLoginResponse(
    val id: Int? = null,
    val nis: String? = null,           
    val nip: String? = null,           
    val nama_siswa: String? = null,    
    val nama: String? = null,          
    val jk: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null,
    val role: String,                  
    val token: String
) {
    fun getDisplayName(): String {
        return nama_siswa ?: nama ?: "User"
    }
    
    fun getIdentifier(): String {
        return nis ?: nip ?: ""
    }
    
    fun isStaff(): Boolean {
        return role.lowercase() in listOf("admin", "guru", "wali_kelas", "wali kelas")
    }
}