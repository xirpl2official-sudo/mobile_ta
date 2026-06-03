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

    suspend fun getJadwalDhuhaKeahlian(token: String): Result<List<com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getJurusanDhuhaSchedules("Bearer $token")
                if (response.isSuccessful) {
                    val jurusans = response.body()?.data ?: emptyList()
                    val days = listOf("Senin", "Selasa", "Rabu", "Kamis")
                    val grouped = jurusans
                        .filter { !it.hari_dhuha.isNullOrBlank() }
                        .groupBy { it.hari_dhuha ?: "" }

                    val resultList = days.map { day ->
                        val onDay = grouped[day] ?: emptyList()
                        com.xirpl2.SASMobile.model.JadwalDhuhaKeahlian(
                            hari = day,
                            jurusan1 = onDay.getOrNull(0),
                            jurusan2 = onDay.getOrNull(1)
                        )
                    }
                    Result.success(resultList)
                } else {
                    Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateJurusanDhuhaDay(token: String, id: Int, hari: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateJurusanDhuhaDay("Bearer $token", id, com.xirpl2.SASMobile.model.DhuhaDayRequest(hari_dhuha = hari))
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

    suspend fun updateJadwalDhuhaTime(token: String, idJurusan: Int, request: com.xirpl2.SASMobile.model.JadwalDhuhaTimeUpdateRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateJadwalDhuhaTime("Bearer $token", idJurusan, request)
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

    suspend fun getStudentDetail(token: String, nis: String): Result<StudentDetailResponse> {
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
}
