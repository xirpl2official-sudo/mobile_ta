package com.xirpl2.SASMobile.model

enum class StatusAbsensi {
    HADIR,
    ALPHA,
    SAKIT,
    IZIN,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): StatusAbsensi {
            if (value.isNullOrBlank()) return UNKNOWN
            return when (value.trim().uppercase()) {
                "HADIR" -> HADIR
                "ALPHA" -> ALPHA
                "SAKIT" -> SAKIT
                "IZIN" -> IZIN
                else -> UNKNOWN
            }
        }
    }
}
