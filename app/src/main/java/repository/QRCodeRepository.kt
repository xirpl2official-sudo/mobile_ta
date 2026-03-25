package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository untuk mengelola QR Code operations
 * - Generate QR code for staff (admin/guru/wali_kelas)
 * - Verify/scan QR code for students (siswa)
 */
class QRCodeRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Generate QR code for attendance display (Staff only)
     * Staff displays this QR code for students to scan
     * @param token Auth token (Bearer token for admin/guru/wali_kelas role)
     * @return Result with QRCodeData or error
     */
    suspend fun generateQRCode(token: String): Result<QRCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<QRCodeGenerateResponse> =
                    apiService.generateQRCode("Bearer $token")

                if (!response.isSuccessful) {
                    val errorMessage = when (response.code()) {
                        401 -> "Sesi telah berakhir, silakan login kembali"
                        403 -> "Anda tidak memiliki akses untuk generate QR code"
                        404 -> "Tidak ada jadwal sholat aktif saat ini"
                        else -> "Gagal generate QR code: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    /**
     * Scan and verify QR code to record student attendance (Student only)
     * Student scans QR code displayed by staff to record their attendance
     * @param authToken Auth token (Bearer token for siswa/student role)
     * @param qrToken Scanned token from QR code
     * @return Result with verification data or error
     */
    suspend fun verifyQRCode(authToken: String, qrToken: String): Result<QRCodeVerifyData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = QRCodeVerifyRequest(token = qrToken)
                val response: Response<QRCodeVerifyResponse> =
                    apiService.verifyQRCode("Bearer $authToken", request)

                if (!response.isSuccessful) {
                    val errorMessage = when (response.code()) {
                        400 -> parseErrorMessage(response) ?: "Token QR code tidak valid atau sudah kadaluarsa"
                        401 -> "Sesi telah berakhir, silakan login kembali"
                        403 -> "Anda tidak memiliki akses untuk verifikasi absensi"
                        409 -> "Siswa sudah tercatat hadir untuk jadwal ini"
                        else -> "Gagal verifikasi: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                if (body.data == null) {
                    return@withContext Result.failure(Exception(body.message))
                }

                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    /**
     * Parse error message from error response body
     */
    private fun parseErrorMessage(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            // Try to extract message from JSON error response
            if (errorBody != null && errorBody.contains("message")) {
                val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                regex.find(errorBody)?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
