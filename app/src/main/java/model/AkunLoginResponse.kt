package com.xirpl2.SASMobile.model

/**
 * Login response data
 * Supports both student (siswa) and staff (admin/guru/wali_kelas) login
 */
data class AkunLoginResponse(
    val id: Int? = null,
    val nis: String? = null,           // For students
    val nip: String? = null,           // For staff
    val nama_siswa: String? = null,    // For students
    val nama: String? = null,          // For staff
    val jk: String? = null,
    val jurusan: String? = null,
    val kelas: String? = null,
    val role: String,                  // "siswa", "admin", "guru", "wali_kelas"
    val token: String
) {
    /**
     * Get display name (works for both student and staff)
     */
    fun getDisplayName(): String {
        return nama_siswa ?: nama ?: "User"
    }
    
    /**
     * Get identifier (NIS for students, NIP for staff)
     */
    fun getIdentifier(): String {
        return nis ?: nip ?: ""
    }
    
    /**
     * Check if user is staff (admin, guru, or wali_kelas)
     */
    fun isStaff(): Boolean {
        return role.lowercase() in listOf("admin", "guru", "wali_kelas", "wali kelas")
    }
}