package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class DeviceRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getDeviceAuthInfo(token: String): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDeviceAuthInfo("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body()?.data)
                } else {
                    Result.failure(Exception("Gagal mengambil info perangkat: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registerDevice(token: String, request: HardwareAuthRequest): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerDevice("Bearer $token", request)
                if (response.isSuccessful) {
                    Result.success(response.body()?.data)
                } else {
                    Result.failure(Exception("Gagal mendaftarkan perangkat: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun verifyDevice(token: String, request: HardwareAuthRequest): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyDevice("Bearer $token", request)
                if (response.isSuccessful) {
                    Result.success(response.body()?.data)
                } else {
                    Result.failure(Exception("Verifikasi perangkat gagal: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Admin Methods
    suspend fun getAdminDeviceManagement(token: String): Result<List<DeviceManagementItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminDeviceManagement("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception("Gagal mengambil data perangkat admin: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getDeviceChangeRequests(token: String): Result<List<DeviceChangeRequestItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDeviceChangeRequests("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception("Gagal mengambil permintaan ganti perangkat: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun processDeviceChangeRequest(token: String, id: Int, action: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.processDeviceChangeRequest("Bearer $token", id, action)
                if (response.isSuccessful) {
                    Result.success(response.body()?.message ?: "Berhasil diproses")
                } else {
                    Result.failure(Exception("Gagal memproses permintaan: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resetDeviceByNIS(token: String, nis: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Use ChangeDeviceRequest from DeviceModels.kt
                val response = apiService.adminChangeDevice(
                    "Bearer $token", 
                    com.xirpl2.SASMobile.model.ChangeDeviceRequest(nis, "RESET", "Reset oleh admin")
                )
                if (response.isSuccessful) {
                    Result.success("Perangkat berhasil di-reset")
                } else {
                    Result.failure(Exception("Gagal reset perangkat: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
