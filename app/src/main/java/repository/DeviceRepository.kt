package com.xirpl2.SASMobile.repository

import android.util.Log
import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class DeviceRepository {

    private val apiService = RetrofitClient.apiService
    private val TAG = "DeviceRepository"

    private fun extractErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null) {
                val json = JSONObject(errorBody)
                json.optString("message", "Unknown error (code ${response.code()})")
            } else {
                "No response body (code ${response.code()})"
            }
        } catch (e: Exception) {
            "Error parsing response: ${e.message} (code ${response.code()})"
        }
    }

    suspend fun getDeviceAuthInfo(token: String): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDeviceAuthInfo("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body()?.data)
                } else {
                    val errorMsg = extractErrorMessage(response)
                    Log.e(TAG, "getDeviceAuthInfo failed: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getDeviceAuthInfo exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun registerDevice(token: String, request: HardwareAuthRequest): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "registerDevice: hardware_id=${request.hardwareId}, device_name=${request.deviceName}")
                val response = apiService.registerDevice("Bearer $token", request)
                if (response.isSuccessful) {
                    Log.d(TAG, "registerDevice success: ${response.body()?.message}")
                    Result.success(response.body()?.data)
                } else if (response.code() == 409) {
                    // 409 = device already registered for this account = treat as success
                    val errorMsg = extractErrorMessage(response)
                    Log.w(TAG, "registerDevice 409 (already registered): $errorMsg")
                    Result.success(null)
                } else {
                    val errorMsg = extractErrorMessage(response)
                    Log.e(TAG, "registerDevice failed (${response.code()}): $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "registerDevice exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun verifyDevice(token: String, request: HardwareAuthRequest): Result<DeviceInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "verifyDevice: hardware_id=${request.hardwareId}")
                val response = apiService.verifyDevice("Bearer $token", request)
                if (response.isSuccessful) {
                    Log.d(TAG, "verifyDevice success")
                    Result.success(response.body()?.data)
                } else {
                    val errorMsg = extractErrorMessage(response)
                    Log.w(TAG, "verifyDevice failed (${response.code()}): $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "verifyDevice exception", e)
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
                    val errorMsg = extractErrorMessage(response)
                    Result.failure(Exception(errorMsg))
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
                    val errorMsg = extractErrorMessage(response)
                    Result.failure(Exception(errorMsg))
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
                    val errorMsg = extractErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resetDeviceByNIS(token: String, nis: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.adminChangeDevice(
                    "Bearer $token",
                    com.xirpl2.SASMobile.model.ChangeDeviceRequest(nis, "RESET", "Reset oleh admin")
                )
                if (response.isSuccessful) {
                    Result.success("Perangkat berhasil di-reset")
                } else {
                    val errorMsg = extractErrorMessage(response)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
