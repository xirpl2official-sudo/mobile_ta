package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class PerizinanHalanganRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun requestHalangan(token: String, tanggalMulai: String): Result<RequestHalanganData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.requestHalangan(
                    "Bearer $token",
                    RequestHalanganBody(tanggalMulai = tanggalMulai)
                )
                if (!response.isSuccessful) {
                    val msg = parseError(response)
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Response data kosong"))
                }
                Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun validateHalangan(token: String, qrToken: String, status: String, catatan: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.validateHalangan(
                    "Bearer $token",
                    ValidateHalanganBody(qrToken = qrToken, status = status, catatan = catatan)
                )
                if (!response.isSuccessful) {
                    val msg = parseError(response)
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body()
                Result.success(body?.message ?: "Validasi berhasil")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getHalanganStatus(token: String, siswaId: Int): Result<HalanganStatusData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getHalanganStatus("Bearer $token", siswaId)
                if (!response.isSuccessful) {
                    val msg = parseError(response)
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Response data kosong"))
                }
                Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                JSONObject(errorBody).optString("message", response.message())
            } else {
                response.message()
            }
        } catch (_: Exception) {
            response.message()
        }
    }
}
