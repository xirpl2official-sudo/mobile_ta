package com.xirpl2.SASMobile.repository

import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class BerandaRepository {

    private val apiService = RetrofitClient.apiService

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

                
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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

                
                val data = body.data ?: HistorySiswaData(week = 0, periode = "", absensi = emptyList())
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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
                if (body == null || body.status != true) {
                    return@withContext Result.failure(Exception(body?.message ?: "Gagal menghapus"))
                }
                
                return@withContext Result.success(body.message ?: "Berhasil")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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

                
                return@withContext Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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

        
        if (body.status == true) {
            if (body.data != null) {
                return Result.success(body.data)
            } else {
                return Result.failure(Exception("Data tidak tersedia"))
            }
        } else {
            
            return Result.failure(Exception(body.message ?: "Gagal memproses permintaan"))
        }
    }

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

                if (body.status != true) {
                    return@withContext Result.failure(Exception(body.message ?: "Gagal membuat siswa"))
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

                if (body.status != true) {
                    return@withContext Result.failure(Exception(body.message ?: "Gagal memperbarui siswa"))
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

                if (body.status != true) {
                    return@withContext Result.failure(Exception(body.message ?: "Gagal menghapus siswa"))
                }

                return@withContext Result.success(body.message ?: "Berhasil")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

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
                
                if (body.status != true) {
                    return@withContext Result.failure(Exception(body.message ?: "Gagal submit absensi"))
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
                
                if (body.status != true) {
                    return@withContext Result.failure(Exception(body.message ?: "Gagal membuat absensi"))
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