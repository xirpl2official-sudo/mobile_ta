package com.xirpl2.SASMobile.repository

import android.net.Uri
import com.xirpl2.SASMobile.model.*
import com.xirpl2.SASMobile.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    suspend fun getPrayerTimes(token: String): Result<List<PrayerTime>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerTimes("Bearer $token")
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

    suspend fun getPrayerTypes(token: String): Result<List<PrayerType>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerTypes("Bearer $token")
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

    suspend fun createPrayerType(
        token: String,
        request: PrayerTypeRequest
    ): Result<PrayerType> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createPrayerType("Bearer $token", request)
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

    suspend fun createPrayerTime(
        token: String,
        request: PrayerTimeRequest
    ): Result<PrayerTime> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createPrayerTime("Bearer $token", request)
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

    suspend fun getJadwalDuhaKeahlian(token: String): Result<List<com.xirpl2.SASMobile.model.JadwalDuhaKeahlian>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getJadwalDuhaKeahlian("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(response.body()?.data ?: emptyList())
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateJurusanDuhaDay(token: String, id: Int, hari: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateJurusanDuhaDay("Bearer $token", id, com.xirpl2.SASMobile.model.DuhaDayRequest(hari_Duha = hari))
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateJadwalDuhaTime(token: String, idJurusan: Int, request: com.xirpl2.SASMobile.model.JadwalDuhaTimeUpdateRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateJadwalDuhaTime("Bearer $token", idJurusan, request)
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(response.message()))
                }
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

    suspend fun getClosestPrayerSchedule(token: String): Result<ClosestPrayerData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getClosestPrayerSchedule("Bearer $token")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                val data = body?.data
                if (data == null) {
                    return@withContext Result.failure(Exception("Data tidak tersedia"))
                }

                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getHistorySiswa(
        token: String,
        week: Int = 0,
        filter: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<HistorySiswaData> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<HistorySiswaResponse> =
                    apiService.getMyAttendanceHistory("Bearer $token", week, filter, startDate, endDate)

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
                    val errorMsg = try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody.isNullOrBlank()) {
                            "HTTP ${response.code()}"
                        } else {
                            "HTTP ${response.code()}: $errorBody"
                        }
                    } catch (e: Exception) {
                        "HTTP ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(errorMsg))
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
                    apiService.getAttendanceAnalytics("Bearer $token", tanggal, tanggal)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    return@withContext Result.failure(Exception("Respons body kosong"))
                }

                val data = body.data ?: return@withContext Result.failure(Exception("Data statistik tidak tersedia"))
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
        tingkatan: Int? = null,
        jurusan: String? = null,
        jk: String? = null,
        agama: String? = null
    ): Result<SiswaListPaginatedResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<SiswaListPaginatedResponse> =
                    apiService.getStudents(
                        "Bearer $token",
                        page,
                        pageSize,
                        search,
                        tingkatan,
                        jurusan,
                        jk,
                        agama
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
                val response = apiService.updateStudent("Bearer $token", Uri.encode(nis), request)

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP Error: ${response.code()} - ${response.message()}")
                    )
                }

                val body = response.body()
                if (body == null) {
                    throw Exception("Response body kosong untuk update siswa")
                }

                val updatedSiswa = body.data
                    ?: return@withContext Result.failure(Exception("Data update siswa kosong"))
                return@withContext Result.success(updatedSiswa)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateSiswaByNIS(
        token: String,
        nis: String,
        request: UpdateSiswaRequest
    ): Result<SiswaItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateStudent(
                    "Bearer $token",
                    Uri.encode(nis),
                    request
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

                val data = body.data ?: return@withContext Result.failure(Exception("Data siswa tidak tersedia"))
                return@withContext Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getStudentDetail(token: String, nis: String): Result<StudentDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentDetail("Bearer $token", Uri.encode(nis))

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
                val response = apiService.deleteStudent("Bearer $token", Uri.encode(nis))

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
                val response = apiService.submitAttendance("Bearer $token", Uri.encode(nis), request)
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

    suspend fun getDuhaToday(token: String): Result<List<DuhaJurusanData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDuhaToday("Bearer $token")

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

    suspend fun importStudents(token: String, filePart: okhttp3.MultipartBody.Part): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.importStudents("Bearer $token", filePart)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try {
                            org.json.JSONObject(errorBody).getString("message")
                        } catch (e: Exception) {
                            errorBody
                        }
                    } else response.message()
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Guru Management ---

    suspend fun getGuruList(
        token: String,
        page: Int? = null,
        limit: Int? = null,
        search: String? = null,
        hasWaliKelas: String? = null
    ): Result<GuruListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminGuruList("Bearer $token", page, limit, search, hasWaliKelas)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createGuru(token: String, request: CreateGuruRequest): Result<GuruItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createGuru("Bearer $token", request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                    } else response.message()
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                val guru = body.data ?: return@withContext Result.failure(Exception("Data guru kosong"))
                Result.success(guru)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getGuruDetail(token: String, id: Int): Result<GuruItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getGuruDetail("Bearer $token", id)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                val guru = body.data ?: return@withContext Result.failure(Exception("Data guru kosong"))
                Result.success(guru)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateGuru(token: String, id: Int, request: UpdateGuruRequest): Result<GuruItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateGuru("Bearer $token", id, request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                    } else response.message()
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                val guru = body.data ?: return@withContext Result.failure(Exception("Data guru kosong"))
                Result.success(guru)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteGuru(token: String, id: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteGuru("Bearer $token", id)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                    } else {
                        "HTTP Error: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(response.body()?.message ?: "Guru berhasil dihapus")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun assignGuruWaliKelas(token: String, guruId: Int, idKelas: Int): Result<GuruItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.assignGuruWaliKelas(
                    "Bearer $token", guruId, AssignWaliKelasGuruRequest(idKelas)
                )
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                    } else response.message()
                    return@withContext Result.failure(Exception(msg))
                }
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                val guru = body.data ?: return@withContext Result.failure(Exception("Data guru kosong"))
                Result.success(guru)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun removeGuruWaliKelas(token: String, guruId: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.removeGuruWaliKelas("Bearer $token", guruId)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                Result.success(response.body()?.message ?: "Berhasil dihapus")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Wali Kelas Management ---

    suspend fun getWaliKelasList(
        token: String,
        page: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<WaliKelasManagementListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminWaliKelasList("Bearer $token", page, limit, search)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getWaliKelasHistory(
        token: String,
        page: Int? = null,
        limit: Int? = null,
        search: String? = null,
        isActive: String? = null
    ): Result<WaliKelasManagementListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminWaliKelasHistory("Bearer $token", page, limit, search, isActive)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Annual Rollover ---

    suspend fun annualRollover(token: String, tahunAjaranBaru: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.annualRollover(
                    "Bearer $token",
                    com.xirpl2.SASMobile.model.AnnualRolloverRequest(tahun_ajaran_baru = tahunAjaranBaru)
                )
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = try {
                        org.json.JSONObject(errorBody ?: "").getString("message")
                    } catch (e: Exception) {
                        errorBody ?: "HTTP Error: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(response.body()?.message ?: "Roll over berhasil")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Bulk Student Control (matches desktop) ---

    suspend fun bulkStudentProgression(token: String, request: BulkProgressionRequest): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.bulkStudentProgression("Bearer $token", request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = try {
                        org.json.JSONObject(errorBody ?: "").optString("error")
                            .takeIf { it.isNotEmpty() }
                            ?: org.json.JSONObject(errorBody ?: "").optString("message")
                    } catch (e: Exception) {
                        errorBody ?: "HTTP Error: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(response.body() ?: MessageResponse(message = "OK"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateBulkStudentFields(token: String, request: BulkFieldsRequest): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateBulkStudentFields("Bearer $token", request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = try {
                        org.json.JSONObject(errorBody ?: "").optString("error")
                            .takeIf { it.isNotEmpty() }
                            ?: org.json.JSONObject(errorBody ?: "").optString("message")
                    } catch (e: Exception) {
                        errorBody ?: "HTTP Error: ${response.code()}"
                    }
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(response.body() ?: MessageResponse(message = "OK"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Lookups ---

    suspend fun getJurusanLookup(token: String): Result<List<JurusanItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getJurusanLookup("Bearer $token")
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                Result.success(response.body()?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getKelasLookup(token: String): Result<List<KelasItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getKelasLookup("Bearer $token")
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                Result.success(response.body()?.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createKelas(token: String, request: CreateKelasRequest): Result<KelasItem?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createKelas("Bearer $token", request)
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                Result.success(response.body()?.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Unregistered Students ---

    suspend fun getUnregisteredStudents(
        token: String,
        page: Int? = 1,
        pageSize: Int? = 100,
        search: String? = null,
        jurusan: Int? = null,
        waliKelas: Int? = null,
        idKelas: Int? = null
    ): Result<SiswaListPaginatedResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUnregisteredStudents(
                    "Bearer $token", page, pageSize, search, jurusan, waliKelas, idKelas
                )
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP Error: ${response.code()}"))
                val body = response.body() ?: return@withContext Result.failure(Exception("Response body kosong"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun notifyWaliKelas(token: String, request: NotifyWaliKelasRequest): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.notifyWaliKelas("Bearer $token", request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = if (!errorBody.isNullOrEmpty()) {
                        try { org.json.JSONObject(errorBody).getString("message") } catch (e: Exception) { errorBody }
                    } else response.message()
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(response.body()?.message ?: "Berhasil")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Duha Groups ---

    suspend fun getDuhaGroups(token: String): Result<List<DuhaGroup>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDhuhaGroups("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createDuhaGroup(token: String, request: DuhaGroupRequest): Result<DuhaGroup> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createDhuhaGroup("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateDuhaGroup(token: String, id: Int, request: DuhaGroupRequest): Result<DuhaGroup> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateDhuhaGroup("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createWeeklyDuhaGroups(token: String, request: WeeklyDuhaGroupRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createWeeklyDhuhaGroup("Bearer $token", request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteDuhaGroup(token: String, id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteDhuhaGroup("Bearer $token", id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = parseErrorMessage(response) ?: response.message()
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Prayer Types ---

    suspend fun getPrayerTypesList(token: String): Result<List<PrayerType>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerTypes("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updatePrayerType(token: String, id: Int, request: PrayerTypeRequest): Result<PrayerType> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updatePrayerType("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deletePrayerType(token: String, id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deletePrayerType("Bearer $token", id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "Tipe shalat tidak bisa dihapus karena masih memiliki data absensi terkait"
                        else -> parseErrorMessage(response) ?: response.message()
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseErrorMessage(response: retrofit2.Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val map = org.json.JSONObject(errorBody)
                    .optJSONObject("error")?.optString("message")
                    ?: org.json.JSONObject(errorBody).optString("error")
                map
            } else null
        } catch (_: Exception) { null }
    }

    // --- Prayer Times ---

    suspend fun getPrayerTimesList(token: String): Result<List<PrayerTime>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerTimes("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updatePrayerTime(token: String, id: Int, request: PrayerTimeRequest): Result<PrayerTime> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updatePrayerTime("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deletePrayerTime(token: String, id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deletePrayerTime("Bearer $token", id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = parseErrorMessage(response) ?: response.message()
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Student Status Update ---

    suspend fun updateStudentStatus(token: String, nis: String, request: UpdateStatusRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateStudentStatus("Bearer $token", Uri.encode(nis), request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Academic Years ---

    suspend fun getAcademicYearsList(token: String): Result<List<AcademicYear>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAcademicYears("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createAcademicYear(token: String, request: AcademicYearRequest): Result<AcademicYear> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createAcademicYear("Bearer $token", request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateAcademicYear(token: String, id: Int, request: AcademicYearRequest): Result<AcademicYear> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateAcademicYear("Bearer $token", id, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.data!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteAcademicYear(token: String, id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteAcademicYear("Bearer $token", id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Backup ---

    suspend fun getBackupStatus(token: String): Result<BackupStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBackupsStatus("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun confirmBackup(token: String, request: BackupConfirmRequest): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.confirmBackup("Bearer $token", request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteBackup(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteBackup("Bearer $token")
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
