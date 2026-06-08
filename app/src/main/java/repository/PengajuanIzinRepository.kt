package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PengajuanIzinRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getPengajuanIzinList(
        token: String,
        status: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Result<PengajuanIzinList> {
        return withContext(Dispatchers.IO) {
            try {
                val filters = mutableMapOf<String, String>()
                status?.let { filters["status"] = it }
                page?.let { filters["page"] = it.toString() }
                limit?.let { filters["limit"] = it.toString() }

                val response = apiService.getPengajuanIzinList("Bearer $token", filters)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        errorBody?.let { org.json.JSONObject(it).optString("error", response.message()) }
                            ?: response.message()
                    } catch (_: Exception) { response.message() }
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code()}: $errorMsg")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                return@withContext Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updatePengajuanIzinStatus(
        token: String,
        id: Int,
        status: String,
        catatan: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateStatusPengajuanIzinRequest(
                    id_pengajuan = id,
                    status = status,
                    catatanVerifikasi = catatan
                )
                val response = apiService.updatePengajuanIzinStatus("Bearer $token", id, request)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        errorBody?.let { org.json.JSONObject(it).optString("error", response.message()) }
                            ?: response.message()
                    } catch (_: Exception) { response.message() }
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code()}: $errorMsg")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                return@withContext Result.success(body.message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getPengajuanIzinDetail(
        token: String,
        id: Int
    ): Result<PengajuanIzin> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPengajuanIzinDetail("Bearer $token", id)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        errorBody?.let { org.json.JSONObject(it).optString("error", response.message()) }
                            ?: response.message()
                    } catch (_: Exception) { response.message() }
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code()}: $errorMsg")
                    )
                }

                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Data tidak ditemukan"))
                }

                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getBuktiFoto(
        token: String,
        id: Int
    ): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBuktiFoto("Bearer $token", id)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = try {
                        errorBody?.let { org.json.JSONObject(it).optString("error", response.message()) }
                            ?: response.message()
                    } catch (_: Exception) { response.message() }
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code()}: $errorMsg")
                    )
                }

                val body = response.body()
                val data = body?.data
                // Try multiple potential field names for the URL
                val url = data?.get("url")?.asString 
                    ?: data?.get("bukti_foto")?.asString
                    ?: data?.get("bukti")?.asString
                    ?: data?.get("file")?.asString
                    ?: data?.get("path")?.asString
                
                return@withContext Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
