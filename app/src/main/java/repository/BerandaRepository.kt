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
                    apiService.getPrayerSchedules(
                        token = "Bearer $token",
                        page = 1,
                        limit = 100
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

    suspend fun getJadwalSholatToday(token: String): Result<List<JadwalSholatData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerSchedulesToday("Bearer $token")

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
                    apiService.getMyAttendanceHistory("Bearer $token", week)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data ?: HistorySiswaData(periode = "", absensi = emptyList())
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getUserProfile(token: String): Result<AkunLoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<AuthResponse> =
                    apiService.getProfile("Bearer $token")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Respons body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data tidak tersedia"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getJadwalSholatById(token: String, id: Int): Result<JadwalSholatDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerSchedule("Bearer $token", id)
                
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
                val response = apiService.createPrayerSchedule("Bearer $token", request)
                
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
                val response = apiService.updatePrayerSchedule("Bearer $token", id, request)
                
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
                val response = apiService.deletePrayerSchedule("Bearer $token", id)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Gagal menghapus"))
                }
                
                return@withContext Result.success(body.message)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStatistics(token: String, tanggal: String? = null): Result<StatisticsData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<StatisticsResponse> =
                    apiService.getAttendanceAnalytics("Bearer $token", tanggal)

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

    suspend fun getSiswaList(
        token: String,
        page: Int = 1,
        pageSize: Int = 100,
        search: String? = null,
        kelas: String? = null,
        jurusan: String? = null,
        jk: String? = null
    ): Result<SiswaListPaginatedResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<SiswaListPaginatedResponse> =
                    apiService.getStudents(
                        "Bearer $token",
                        page,
                        pageSize,
                        search,
                        kelas,
                        jurusan
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
                val response = apiService.createStudent("Bearer $token", request)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
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
                val response = apiService.updateStudent("Bearer $token", nis, request)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStudentDetail(token: String, nis: String): Result<com.google.gson.JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentDetail("Bearer $token", nis)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
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
                val response = apiService.deleteStudent("Bearer $token", nis)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
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

    suspend fun getHistoryStaff(
        token: String,
        kelas: String? = null,
        jurusan: String? = null,
        jenisSholat: String? = null,
        search: String? = null,
        tanggal: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int? = null,
        limit: Int? = null,
        filters: Map<String, String>? = null
    ): Result<HistoryStaffData> {
        return withContext(Dispatchers.IO) {
            try {
                // If filters map is provided with nis, use it as search parameter
                val nisFilter = filters?.get("nis")
                
                val response: Response<HistoryStaffResponse> =
                    apiService.getAttendanceHistory(
                        "Bearer $token",
                        kelas ?: filters?.get("kelas"),
                        jurusan ?: filters?.get("jurusan"),
                        jenisSholat,
                        search ?: nisFilter,
                        tanggal,
                        startDate,
                        endDate,
                        page,
                        limit
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
        nis: String,
        request: CreateAbsensiRequest
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.submitAttendance("Bearer $token", nis, request)                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
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

    // Alias for createAbsensi to maintain compatibility
    suspend fun createAbsensi(
        token: String,
        nis: String,
        request: CreateAbsensiRequest
    ): Result<String> = submitAbsensi(token, nis, request)

    suspend fun verifyQRCode(
        token: String,
        qrToken: String
    ): Result<QRCodeVerifyData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = QRCodeVerifyRequest(token = qrToken)
                val response = apiService.verifyQRCode("Bearer $token", request)
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }
                
                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Response body kosong"))
                }
                
                val data = body.data ?: return@withContext Result.failure(Exception("Data verifikasi kosong"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getDhuhaToday(token: String): Result<List<DhuhaJurusanData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDhuhaToday("Bearer $token")

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

    suspend fun getAdminManagementKelas(token: String): Result<List<KelasManagementItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminManagementKelas("Bearer $token")
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                return@withContext Result.success(response.body()?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getAdminManagementKelasDetail(token: String, id: Int): Result<KelasManagementDetail> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminManagementKelasDetail("Bearer $token", id)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val data = response.body()?.data ?: return@withContext Result.failure(Exception("Data tidak ditemukan"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateWaliKelas(token: String, idKelas: Int, idStaff: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateWaliKelas("Bearer $token", idKelas, UpdateWaliKelasRequest(idStaff))
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                return@withContext Result.success(response.body()?.message ?: "Berhasil update")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStaffGuruLookup(token: String): Result<List<StaffInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStaffGuruLookup("Bearer $token")
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                return@withContext Result.success(response.body()?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
