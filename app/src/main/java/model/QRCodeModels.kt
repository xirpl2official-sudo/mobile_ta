package com.xirpl2.SASMobile.model

/**
 * Response wrapper for QR Code generation endpoint
 * GET /api/qrcode/generate
 */
data class QRCodeGenerateResponse(
    val message: String,
    val data: QRCodeData
)

/**
 * QR Code data from generate endpoint
 */
data class QRCodeData(
    val qr_code: String,           // Base64 encoded PNG: "data:image/png;base64,[PNG_DATA]"
    val token: String,             // Signed token for verification
    val expires_at: String,        // ISO 8601 format: "2026-01-21T14:35:00Z"
    val jenis_sholat: String,      // Prayer type: "Subuh", "Dzuhur", "Asr", "Maghrib", "Isya"
    val id_jadwal: Int             // Prayer schedule ID
)

/**
 * Request body for QR Code verification
 * POST /api/qrcode/verify
 */
data class QRCodeVerifyRequest(
    val token: String              // Scanned token from QR code
)

/**
 * Response wrapper for QR Code verification endpoint
 * POST /api/qrcode/verify
 */
data class QRCodeVerifyResponse(
    val message: String,
    val data: QRCodeVerifyData?
)

/**
 * Verification result data
 */
data class QRCodeVerifyData(
    val siswa: SiswaVerifyInfo,    // Student info
    val absensi: AbsensiVerifyInfo // Attendance record info
)

/**
 * Student info from verification
 */
data class SiswaVerifyInfo(
    val id: Int,
    val nis: String,
    val nama: String,
    val kelas: String,
    val jurusan: String? = null
)

/**
 * Attendance record info from verification
 */
data class AbsensiVerifyInfo(
    val id: Int,
    val id_jadwal: Int,
    val jenis_sholat: String,
    val tanggal: String,
    val waktu_absen: String,
    val status: String             // "HADIR", etc.
)
