package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository untuk mengelola data Beranda dari API
 * Menggunakan coroutines untuk async operations
 */
class BerandaRepository {

    private val apiService = RetrofitClient.apiService

    /**
     * Get jadwal sholat dari API
     * @param token Auth token user
     * @return Result dengan data atau error
     */
    suspend fun getJadwalSholat(token: String): Result<List<JadwalSholatData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<JadwalSholatListResponse> =
                    apiService.getJadwalSholat(
                        token = "Bearer $token",
                        page = 1,
                        pageSize = 100,
                        sortBy = "id_jadwal"
                    )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                // Return the data directly as it's a list
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get riwayat absensi siswa dari API history/siswa
     * @param week Minggu ke berapa (0 = minggu ini, 1 = minggu lalu, dst)
     */
    suspend fun getHistorySiswa(token: String, week: Int = 0): Result<HistorySiswaData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<HistorySiswaResponse> =
                    apiService.getHistorySiswa("Bearer $token", week)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                // Handle nullable data - return default empty data if null
                val data = body.data ?: HistorySiswaData(week = 0, periode = "", absensi = emptyList())
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get user profile (untuk mendapatkan jenis kelamin)
     */
    suspend fun getUserProfile(token: String): Result<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<ApiResponse<UserData>> =
                    apiService.getUserProfile("Bearer $token")

                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get statistik absensi
     */
    suspend fun getStatistikAbsensi(
        token: String,
        bulan: Int? = null,
        tahun: Int? = null
    ): Result<StatistikData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<ApiResponse<StatistikData>> =
                    apiService.getStatistikAbsensi("Bearer $token", bulan, tahun)

                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get statistik total siswa
     */
    suspend fun getTotalSiswa(token: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<ApiResponse<Int>> =
                    apiService.getTotalSiswa("Bearer $token")

                handleApiResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get jadwal sholat by ID
     */
    suspend fun getJadwalSholatById(token: String, id: Int): Result<JadwalSholatDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getJadwalSholatById("Bearer $token", id)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
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

    /**
     * Create new jadwal sholat
     */
    suspend fun createJadwalSholat(
        token: String,
        request: JadwalSholatCreateRequest
    ): Result<JadwalSholatDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createJadwalSholat("Bearer $token", request)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Data tidak tersedia"))
                }
                
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update jadwal sholat
     */
    suspend fun updateJadwalSholat(
        token: String,
        id: Int,
        request: JadwalSholatUpdateRequest
    ): Result<JadwalSholatDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateJadwalSholat("Bearer $token", id, request)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body?.data == null) {
                    return@withContext Result.failure(Exception("Data tidak tersedia"))
                }
                
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete jadwal sholat
     */
    suspend fun deleteJadwalSholat(token: String, id: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteJadwalSholat("Bearer $token", id)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body == null || !body.status) {
                    return@withContext Result.failure(Exception(body?.message ?: "Gagal menghapus"))
                }
                
                return@withContext Result.success(body.message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get statistics for today's attendance
     * No authentication required
     * Response format: {"message":"...", "data":{...}}
     */
    suspend fun getStatistics(): Result<StatisticsData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<StatisticsResponse> =
                    apiService.getStatistics()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Respons body kosong"))
                }

                // Directly return the data since response has "message" and "data" fields
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get Dhuha prayer schedules for today (max 2 jurusan)
     */
    suspend fun getDhuhaToday(token: String): Result<List<DhuhaJurusanData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<DhuhaTodayResponse> =
                    apiService.getDhuhaToday("Bearer $token")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Helper function untuk menangani respons API secara konsisten
     */
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Result<T> {
        if (!response.isSuccessful) {
            return Result.failure(
                Exception("HTTP Error: ${response.code()} - ${response.message()}")
            )
        }

        val body = response.body()
        if (body == null) {
            return Result.failure(Exception("Respons body kosong"))
        }

        // Sesuaikan dengan model ApiResponse.kt: gunakan "status", bukan "success"
        if (body.status) {
            if (body.data != null) {
                return Result.success(body.data)
            } else {
                return Result.failure(Exception("Data tidak tersedia"))
            }
        } else {
            // message di modelmu tidak nullable, jadi aman pakai langsung
            return Result.failure(Exception(body.message))
        }
    }

    /**
     * Get history for staff (admin/guru/wali_kelas)
     * Returns attendance summary and list of all students' attendance
     */
    suspend fun getHistoryStaff(
        token: String,
        startDate: String? = null,
        endDate: String? = null,
        kelas: String? = null,
        jurusan: String? = null,
        status: String? = null,
        nis: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Result<HistoryStaffData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<HistoryStaffResponse> =
                    apiService.getHistoryStaff(
                        token = "Bearer $token",
                        startDate = startDate,
                        endDate = endDate,
                        kelas = kelas,
                        jurusan = jurusan,
                        status = status,
                        jenisSholat = null,
                        search = null,
                        nis = nis,
                        page = page,
                        limit = limit
                    )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data
                if (data == null) {
                    return@withContext Result.failure(Exception("Data kosong"))
                }

                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get list of all students with pagination
     * Supports search, kelas, jurusan, and jk (gender) filters
     * Returns the full response with data as List<SiswaItem> and pagination info
     */
    suspend fun getSiswaList(
        token: String,
        page: Int = 1,
        pageSize: Int = 100,
        search: String? = null,
        kelas: String? = null,
        jurusan: String? = null,
        jk: String? = null
    ): Result<SiswaListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<SiswaListResponse> =
                    apiService.getSiswa(
                        "Bearer $token",
                        page,
                        pageSize,
                        search,
                        kelas,
                        jurusan,
                        jk
                    )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
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

    /**
     * Create a new student
     */
    suspend fun createSiswa(
        token: String,
        request: CreateSiswaRequest
    ): Result<SiswaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSiswa("Bearer $token", request)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                if (!body.status) {
                    return@withContext Result.failure(Exception(body.message))
                }

                val data = body.data
                if (data == null) {
                    return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
                }

                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Update existing student
     */
    suspend fun updateSiswa(
        token: String,
        nis: String,
        request: UpdateSiswaRequest
    ): Result<SiswaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateSiswa("Bearer $token", nis, request)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                if (!body.status) {
                    return@withContext Result.failure(Exception(body.message))
                }

                val data = body.data
                if (data == null) {
                    return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
                }

                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Delete student
     */
    suspend fun deleteSiswa(
        token: String,
        nis: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteSiswa("Bearer $token", nis)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                if (!body.status) {
                    return@withContext Result.failure(Exception(body.message))
                }

                return@withContext Result.success(body.message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get history staff (for admin dashboard statistics)
     */
    suspend fun getHistoryStaff(
        token: String,
        startDate: String? = null,
        endDate: String? = null,
        kelas: String? = null,
        jurusan: String? = null,
        status: String? = null,
        jenisSholat: String? = null,
        search: String? = null,
        nis: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Result<HistoryStaffData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<HistoryStaffResponse> =
                    apiService.getHistoryStaff(
                        token = "Bearer $token",
                        startDate = startDate,
                        endDate = endDate,
                        kelas = kelas,
                        jurusan = jurusan,
                        status = status,
                        jenisSholat = null,
                        search = null,
                        nis = nis,
                        page = page,
                        limit = limit
                    )

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data kosong"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    /**
     * Submit attendance based on location (Auto Absen)
     */
    suspend fun submitAbsensi(
        token: String, 
        latitude: Double, 
        longitude: Double, 
        jadwalId: Int,
        foto: String? = null
    ): Result<AbsensiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AbsensiRequest(
                    jadwal_sholat_id = jadwalId,
                    latitude = latitude,
                    longitude = longitude,
                    foto = foto
                )
                
                val response = apiService.submitAbsensi("Bearer $token", request)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }
                
                if (!body.status) {
                    return@withContext Result.failure(Exception(body.message))
                }
                
                if (body.data == null) {
                    return@withContext Result.failure(Exception("Data tidak tersedia"))
                }
                
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    /**
     * Create manual absensi (for Izin/Sakit/Alpha)
     */
    suspend fun createAbsensi(
        token: String,
        nis: String,
        request: CreateAbsensiRequest
    ): Result<ManualAbsensiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createAbsensi("Bearer $token", nis, request)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }
                
                if (!body.status) {
                    return@withContext Result.failure(Exception(body.message))
                }
                
                val data = body.data
                if (data == null) {
                    return@withContext Result.failure(Exception("Data tidak tersedia"))
                }
                
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}