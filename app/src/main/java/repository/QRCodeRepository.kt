package com.xirpl2.SASMobile.repository

import com.google.gson.Gson
import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class QRCodeRepository {

    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

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

                val qrData = body.data ?: return@withContext Result.failure(Exception("Data QR code kosong"))
                return@withContext Result.success(qrData)
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
                    if (response.code() == 409) {
                        val parsed = parseErrorBodyAsVerifyResponse(response)
                        if (parsed?.data != null) {
                            return@withContext Result.success(parsed.data)
                        }
                        return@withContext Result.failure(
                            Exception(parsed?.message ?: "Siswa sudah tercatat hadir untuk jadwal ini")
                        )
                    }

                    val errorMessage = when (response.code()) {
                        400 -> parseErrorMessage(response) ?: "Token QR code tidak valid atau sudah kadaluarsa"
                        401 -> "Sesi telah berakhir, silakan login kembali"
                        403 -> "Anda tidak memiliki akses untuk verifikasi absensi"
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

    private fun parseErrorBodyAsVerifyResponse(response: Response<*>): QRCodeVerifyResponse? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                gson.fromJson(errorBody, QRCodeVerifyResponse::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseErrorMessage(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val map = gson.fromJson(errorBody, Map::class.java)
                map?.get("message") as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
