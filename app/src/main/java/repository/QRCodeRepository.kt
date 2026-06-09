package com.xirpl2.SASMobile.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xirpl2.SASMobile.model.AttendanceCodeData
import com.xirpl2.SASMobile.model.QRCodeData
import com.xirpl2.SASMobile.model.QRCodeVerifyData
import com.xirpl2.SASMobile.network.RetrofitClient
import com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyRequest as GeneratedQRCodeVerifyRequest
import com.xirpl2.SASMobile.network.generated.model.VerifyCodeRequest as GeneratedVerifyCodeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class QRCodeRepository {

    private val attendanceApi = RetrofitClient.attendanceApi
    private val gson = Gson()

    // -- Helpers: Map<String,Any> -> legacy typed models (response surface unchanged) --

    private fun mapToQRCodeData(map: Map<String, Any>?): QRCodeData? {
        if (map == null) return null
        return gson.fromJson(gson.toJson(map), QRCodeData::class.java)
    }

    private fun mapToQRCodeVerifyData(map: Map<String, Any>?): QRCodeVerifyData? {
        if (map == null) return null
        return gson.fromJson(gson.toJson(map), QRCodeVerifyData::class.java)
    }

    private fun mapToAttendanceCodeData(map: Map<String, Any>?): AttendanceCodeData? {
        if (map == null) return null
        return gson.fromJson(gson.toJson(map), AttendanceCodeData::class.java)
    }

    suspend fun generateQRCode(token: String): Result<QRCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<com.xirpl2.SASMobile.network.generated.model.QRCodeGenerateResponse> =
                    attendanceApi.v2AttendanceQrCodesCurrentGet()

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

                val qrData = mapToQRCodeData(body.data)
                    ?: return@withContext Result.failure(Exception("Data QR code kosong"))
                return@withContext Result.success(qrData)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun verifyQRCode(authToken: String, qrToken: String): Result<QRCodeVerifyData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyResponse> =
                    attendanceApi.v2AttendanceQrCodesVerifyPost(
                        GeneratedQRCodeVerifyRequest(token = qrToken)
                    )

                if (!response.isSuccessful) {
                    if (response.code() == 409) {
                        val parsed = parseErrorBodyAsVerifyResponse(response)
                        if (parsed?.data != null) {
                            val verifyData = mapToQRCodeVerifyData(parsed.data)
                            if (verifyData != null) {
                                return@withContext Result.success(verifyData)
                            }
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

                val verifyData = mapToQRCodeVerifyData(body.data)
                    ?: return@withContext Result.failure(Exception("Data verifikasi tidak ditemukan"))
                return@withContext Result.success(verifyData)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    private fun parseErrorBodyAsVerifyResponse(
        response: Response<*>
    ): com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyResponse? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                gson.fromJson(errorBody, com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyResponse::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun generateAttendanceCode(token: String): Result<AttendanceCodeData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = attendanceApi.v2AttendanceCodeGenerateGet()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                }
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                val data = mapToAttendanceCodeData(body.data)
                    ?: return@withContext Result.failure(Exception("Data kode kosong"))
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    private fun parseErrorMessage(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val map = gson.fromJson(errorBody, Map::class.java)
                map?.get("error") as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
