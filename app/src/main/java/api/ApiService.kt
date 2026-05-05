package com.xirpl2.SASMobile.network

import com.xirpl2.SASMobile.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<String>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AkunLoginResponse>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: PasswordResetRequest
    ): Response<ApiResponse<String>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: OtpVerificationRequest
    ): Response<ApiResponse<String>>

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<String>>

    @GET("auth/me")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfile>>

    @POST("auth/change-email")
    suspend fun changeEmail(
        @Header("Authorization") token: String,
        @Body request: ChangeEmailRequest
    ): Response<ApiResponse<String>>

    
    @GET("jadwal-sholat")
    suspend fun getJadwalSholat(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("sort_by") sortBy: String = "id_jadwal"
    ): Response<JadwalSholatListResponse>

    @GET("jadwal-sholat/{tanggal}")
    suspend fun getJadwalSholatByDate(
        @Header("Authorization") token: String,
        @Path("tanggal") tanggal: String
    ): Response<ApiResponse<List<JadwalSholatData>>>

    @POST("jadwal-sholat")
    suspend fun createJadwalSholat(
        @Header("Authorization") token: String,
        @Body request: JadwalSholatCreateRequest
    ): Response<JadwalSholatDetailResponse>

    @GET("jadwal-sholat/{id}")
    suspend fun getJadwalSholatById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<JadwalSholatDetailResponse>

    @PUT("jadwal-sholat/{id}")
    suspend fun updateJadwalSholat(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalSholatUpdateRequest
    ): Response<JadwalSholatDetailResponse>

    @DELETE("jadwal-sholat/{id}")
    suspend fun deleteJadwalSholat(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ApiResponse<String>>


    @GET("history/siswa")
    suspend fun getHistorySiswa(
        @Header("Authorization") token: String,
        @Query("week") week: Int = 0
    ): Response<HistorySiswaResponse>

    @GET("user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserData>>

    @GET("statistik-absensi")
    suspend fun getStatistikAbsensi(
        @Header("Authorization") token: String,
        @Query("bulan") bulan: Int? = null,
        @Query("tahun") tahun: Int? = null
    ): Response<ApiResponse<StatistikData>>

    @GET("statistik/total-siswa")
    suspend fun getTotalSiswa(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Int>>

    @GET("statistics")
    suspend fun getStatistics(): Response<StatisticsResponse>

    @GET("jadwal-sholat/dhuha-today")
    suspend fun getDhuhaToday(
        @Header("Authorization") token: String
    ): Response<DhuhaTodayResponse>

    @POST("absensi")
    suspend fun submitAbsensi(
        @Header("Authorization") token: String,
        @Body absensiData: AbsensiRequest
    ): Response<ApiResponse<AbsensiResponse>>

    
    
    @GET("qrcode/generate")
    suspend fun generateQRCode(
        @Header("Authorization") token: String
    ): Response<QRCodeGenerateResponse>

    @POST("qrcode/verify")
    suspend fun verifyQRCode(
        @Header("Authorization") token: String,
        @Body request: QRCodeVerifyRequest
    ): Response<QRCodeVerifyResponse>

    @GET("history/staff")
    suspend fun getHistoryStaff(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("status") status: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null,
        @Query("nis") nis: String? = null,
        @Query("tanggal") tanggal: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<HistoryStaffResponse>

    
    
    @GET("siswa")
    suspend fun getSiswa(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("search") search: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("jk") jk: String? = null
    ): Response<SiswaListResponse>

    @POST("siswa")
    suspend fun createSiswa(
        @Header("Authorization") token: String,
        @Body request: CreateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @PUT("siswa/{nis}")
    suspend fun updateSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: UpdateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @DELETE("siswa/{nis}")
    suspend fun deleteSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String
    ): Response<ApiResponse<String>>

    @POST("siswa/{nis}/absensi")
    suspend fun createAbsensi(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: CreateAbsensiRequest
    ): Response<ApiResponse<ManualAbsensiResponse>>

    

    @GET("notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationResponse>

    

    @Streaming
    @GET("export/attendance-report")
    suspend fun exportAttendanceReport(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("jurusan") jurusan: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("export/full-report")
    suspend fun exportFullReport(
        @Header("Authorization") token: String,
        @Query("month") month: String,
        @Query("jurusan") jurusan: String
    ): Response<ResponseBody>
}