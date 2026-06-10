package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class PerizinanHalanganRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun generateHalanganQR(token: String): Result<HalanganQRData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.generateHalanganQR("Bearer $token")
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

    suspend fun verifyHalangan(token: String, halanganToken: String): Result<HalanganVerifyData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyHalangan(
                    "Bearer $token",
                    VerifyHalanganBody(halanganToken = halanganToken)
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
