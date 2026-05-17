package com.xirpl2.SASMobile.model

data class AuthResponse(
    val message: String,
    val data: AkunLoginResponse? = null
)

data class AkunLoginResponse(
    val id: Int? = null,
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
    val role: String? = null,                  
    val token: String? = null
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