package com.xirpl2.SASMobile.network

import com.xirpl2.SASMobile.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    
    @POST("v2/auth/registrations")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<String>>

    @POST("v2/auth/sessions")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AkunLoginResponse>>

    @POST("v2/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: PasswordResetRequest
    ): Response<ApiResponse<String>>

    @POST("v2/auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: OtpVerificationRequest
    ): Response<ApiResponse<String>>

    @POST("v2/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<String>>

    @GET("v2/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserProfile>>

    @POST("v2/auth/email-change-requests")
    suspend fun changeEmail(
        @Header("Authorization") token: String,
        @Body request: ChangeEmailRequest
    ): Response<ApiResponse<String>>

    @POST("v2/auth/email-change-requests/verify")
    suspend fun verifyChangeEmail(
        @Header("Authorization") token: String,
        @Body request: VerifyEmailOTPRequest
    ): Response<ApiResponse<String>>

    
    @GET("v2/prayer-schedules")
    suspend fun getJadwalSholat(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("sort_by") sortBy: String = "id_jadwal"
    ): Response<JadwalSholatListResponse>

    @GET("v2/prayer-schedules/dhuha/today")
    suspend fun getDhuhaToday(
        @Header("Authorization") token: String
    ): Response<DhuhaTodayResponse>

    @POST("v2/prayer-schedules")
    suspend fun createJadwalSholat(
        @Header("Authorization") token: String,
        @Body request: JadwalSholatCreateRequest
    ): Response<JadwalSholatDetailResponse>

    @GET("v2/prayer-schedules/{id}")
    suspend fun getJadwalSholatById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<JadwalSholatDetailResponse>

    @PUT("v2/prayer-schedules/{id}")
    suspend fun updateJadwalSholat(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalSholatUpdateRequest
    ): Response<JadwalSholatDetailResponse>

    @DELETE("v2/prayer-schedules/{id}")
    suspend fun deleteJadwalSholat(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ApiResponse<String>>


    @GET("v2/students/me/attendance-history")
    suspend fun getHistorySiswa(
        @Header("Authorization") token: String,
        @Query("week") week: Int = 0
    ): Response<HistorySiswaResponse>

    @GET("v2/auth/profile") 
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserData>>

    @GET("v2/analytics/attendance")
    suspend fun getStatistikAbsensi(
        @Header("Authorization") token: String,
        @Query("bulan") bulan: Int? = null,
        @Query("tahun") tahun: Int? = null
    ): Response<ApiResponse<StatistikData>>

    @GET("v2/students/count") 
    suspend fun getTotalSiswa(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Int>>

    @GET("v2/analytics/attendance")
    suspend fun getStatistics(): Response<StatisticsResponse>

    @POST("v2/attendance")
    suspend fun submitAbsensi(
        @Header("Authorization") token: String,
        @Body absensiData: AbsensiRequest
    ): Response<ApiResponse<AbsensiResponse>>

    
    
    @GET("v2/attendance/qr-codes/current")
    suspend fun generateQRCode(
        @Header("Authorization") token: String
    ): Response<QRCodeGenerateResponse>

    @POST("v2/attendance/qr-codes/verify")
    suspend fun verifyQRCode(
        @Header("Authorization") token: String,
        @Body request: QRCodeVerifyRequest
    ): Response<QRCodeVerifyResponse>

    @GET("v2/attendance/history")
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

    
    
    @GET("v2/students")
    suspend fun getSiswa(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("search") search: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("jk") jk: String? = null
    ): Response<SiswaListResponse>

    @POST("v2/students")
    suspend fun createSiswa(
        @Header("Authorization") token: String,
        @Body request: CreateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @PUT("v2/students/{nis}")
    suspend fun updateSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: UpdateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @DELETE("v2/students/{nis}")
    suspend fun deleteSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String
    ): Response<ApiResponse<String>>

    @POST("v2/students/{nis}/attendances")
    suspend fun createAbsensi(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: CreateAbsensiRequest
    ): Response<ApiResponse<ManualAbsensiResponse>>

    

    @GET("v2/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationResponse>

    
    @POST("v2/device-auth/register")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceAuthRequest
    ): Response<DeviceInfoResponse>

    @POST("v2/device-auth/verify")
    suspend fun verifyDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceAuthRequest
    ): Response<DeviceInfoResponse>

    @GET("v2/device-auth/me")
    suspend fun getDeviceAuthInfo(
        @Header("Authorization") token: String
    ): Response<DeviceInfoResponse>

    
    @GET("v2/attendance/barcode/current")
    suspend fun generateBarcode(
        @Header("Authorization") token: String
    ): Response<ApiResponse<BarcodeData>>

    @POST("v2/attendance/barcode/verify")
    suspend fun verifyBarcode(
        @Header("Authorization") token: String,
        @Body request: BarcodeVerifyRequest
    ): Response<QRCodeVerifyResponse>

    

    @Streaming
    @GET("v2/reports/attendance/excel")
    suspend fun exportAttendanceReport(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("jurusan") jurusan: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("v2/reports/full/excel")
    suspend fun exportFullReport(
        @Header("Authorization") token: String,
        @Query("month") month: String,
        @Query("jurusan") jurusan: String
    ): Response<ResponseBody>
}
}