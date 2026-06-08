package com.xirpl2.SASMobile.repository

import com.google.gson.Gson
import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class FemaleRestrictionRepository {

    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    suspend fun getRestrictionStatus(token: String): Result<FemaleRestrictionStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFemaleRestrictionStatus("Bearer $token")

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal memuat status halangan: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Data status tidak ditemukan"))
                }

                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun createApprovalRequest(
        token: String,
        idGuruApprover: Int,
        catatan: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SubmitApprovalRequest(
                    idGuruApprover = idGuruApprover,
                    catatan = catatan
                )
                val response = apiService.createFemaleRestrictionRequest("Bearer $token", request)

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal mengirim pengajuan: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                return@withContext Result.success(body?.message ?: "Pengajuan berhasil dikirim")
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun getPendingApprovals(token: String): Result<List<ApprovalRequest>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFemaleRestrictionPendingApprovals("Bearer $token")

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal memuat data: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                return@withContext Result.success(body?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun processApproval(
        token: String,
        id: Int,
        status: String,
        catatan: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ProcessApprovalRequest(
                    status = status,
                    catatan = catatan
                )
                val response = apiService.processFemaleRestrictionApproval("Bearer $token", id, request)

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal memproses: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                return@withContext Result.success(body?.message ?: "Berhasil diproses")
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun getHistory(token: String): Result<List<FemaleRestriction>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFemaleRestrictionHistory("Bearer $token")

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal memuat riwayat: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                return@withContext Result.success(body?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(Exception("Gagal terhubung ke server: ${e.message}"))
            }
        }
    }

    suspend fun getFemaleTeachers(token: String): Result<List<FemaleTeacherInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFemaleTeachers("Bearer $token")

                if (!response.isSuccessful) {
                    val errorMessage = parseErrorMessage(response)
                        ?: "Gagal memuat daftar guru: ${response.code()}"
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val body = response.body()
                return@withContext Result.success(body?.data ?: emptyList())
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
                    ?: map?.get("message") as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
