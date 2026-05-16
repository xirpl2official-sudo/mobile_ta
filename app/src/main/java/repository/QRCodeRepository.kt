package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class QRCodeRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun generateQRCode(token: String): Result<QRCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<QRCodeGenerateResponse> =
                    apiService.getCurrentQRCode("Bearer $token")

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

                val verifyData = body.data ?: return@withContext Result.failure(Exception("Data verifikasi tidak ditemukan"))
                return@withContext Result.success(verifyData)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    private fun parseErrorMessage(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            
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
