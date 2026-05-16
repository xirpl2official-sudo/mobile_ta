package com.xirpl2.SASMobile.network

import com.xirpl2.SASMobile.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Authentication ---
    
    @POST("v2/auth/registrations")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("v2/auth/sessions")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @DELETE("v2/auth/sessions/current")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    @POST("v2/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: PasswordResetRequest
    ): Response<MessageResponse>

    @POST("v2/auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: OtpVerificationRequest
    ): Response<MessageResponse>

    @POST("v2/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<MessageResponse>

    @GET("v2/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<AuthResponse>

    @POST("v2/auth/email-change-requests")
    suspend fun changeEmail(
        @Header("Authorization") token: String,
        @Body request: ChangeEmailRequest
    ): Response<MessageResponse>

    @POST("v2/auth/email-change-requests/verify")
    suspend fun verifyChangeEmail(
        @Header("Authorization") token: String,
        @Body request: VerifyEmailOTPRequest
    ): Response<MessageResponse>

    @POST("v2/auth/tokens/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): Response<AuthResponse>

    // --- Prayer Schedules ---

    @GET("v2/prayer-schedules")
    suspend fun getPrayerSchedules(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<JadwalSholatListResponse>

    @POST("v2/prayer-schedules")
    suspend fun createPrayerSchedule(
        @Header("Authorization") token: String,
        @Body request: JadwalSholatCreateRequest
    ): Response<JadwalSholatDetailResponse>

    @GET("v2/prayer-schedules/{id}")
    suspend fun getPrayerSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<JadwalSholatDetailResponse>

    @PUT("v2/prayer-schedules/{id}")
    suspend fun updatePrayerSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalSholatUpdateRequest
    ): Response<JadwalSholatDetailResponse>

    @DELETE("v2/prayer-schedules/{id}")
    suspend fun deletePrayerSchedule(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @GET("v2/prayer-schedules/today")
    suspend fun getPrayerSchedulesToday(
        @Header("Authorization") token: String
    ): Response<JadwalSholatTodayResponse>

    @GET("v2/prayer-schedules/dhuha/today")
    suspend fun getDhuhaToday(
        @Header("Authorization") token: String
    ): Response<DhuhaTodayResponse>

    @GET("v2/prayer-schedules/closest")
    suspend fun getClosestPrayerSchedule(
        @Header("Authorization") token: String
    ): Response<ApiResponse<com.google.gson.JsonObject>>

    @GET("v2/prayer-schedules/dhuha/keahlian")
    suspend fun getJadwalDhuhaKeahlian(
        @Header("Authorization") token: String
    ): Response<JadwalDhuhaKeahlianResponse>

    @GET("v2/prayer-schedules/dhuha/detail")
    suspend fun getSholatDhuhaDetail(
        @Header("Authorization") token: String
    ): Response<SholatDhuhaDetailResponse>

    @GET("v2/prayer-schedules/dzuhur/detail")
    suspend fun getSholatDzuhurDetail(
        @Header("Authorization") token: String
    ): Response<SholatDzuhurDetailResponse>

    @POST("v2/prayer-schedules/dhuha/keahlian")
    suspend fun createJadwalDhuhaKeahlian(
        @Header("Authorization") token: String,
        @Body request: JadwalDhuhaKeahlian
    ): Response<MessageResponse>

    @PUT("v2/prayer-schedules/dhuha/keahlian/{id}")
    suspend fun updateJadwalDhuhaKeahlian(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalDhuhaKeahlian
    ): Response<MessageResponse>

    @PUT("v2/prayer-schedules/dhuha/{id}")
    suspend fun updateSholatDhuha(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SholatDhuhaDetail
    ): Response<MessageResponse>

    @PUT("v2/prayer-schedules/dzuhur/{id}")
    suspend fun updateSholatDzuhur(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: SholatDzuhurDetail
    ): Response<MessageResponse>

    // --- Attendance ---

    @GET("v2/attendance/history")
    suspend fun getAttendanceHistory(
        @Header("Authorization") token: String,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null,
        @Query("tanggal") tanggal: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<HistoryStaffResponse>

    @POST("v2/attendance/auto-mark")
    suspend fun triggerAutoMark(
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    @GET("v2/attendance/qr-codes/current")
    suspend fun getCurrentQRCode(
        @Header("Authorization") token: String
    ): Response<QRCodeGenerateResponse>

    @GET("v2/attendance/qr-codes/current/image")
    suspend fun getCurrentQRCodeImage(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @POST("v2/attendance/qr-codes/verify")
    suspend fun verifyQRCode(
        @Header("Authorization") token: String,
        @Body request: QRCodeVerifyRequest
    ): Response<MessageResponse>

    @GET("v2/attendance/code/generate")
    suspend fun generateAttendanceCode(
        @Header("Authorization") token: String
    ): Response<GenerateCodeResponse>

    @POST("v2/attendance/code/verify")
    suspend fun verifyAttendanceCode(
        @Header("Authorization") token: String,
        @Body request: VerifyCodeRequest
    ): Response<MessageResponse>

    @GET("v2/attendance/barcode/current")
    suspend fun getCurrentBarcode(
        @Header("Authorization") token: String
    ): Response<ApiResponse<BarcodeData>>

    @POST("v2/attendance/barcode/verify")
    suspend fun verifyBarcode(
        @Header("Authorization") token: String,
        @Body request: BarcodeVerifyRequest
    ): Response<MessageResponse>

    // --- Students ---

    @GET("v2/students")
    suspend fun getStudents(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null
    ): Response<SiswaListPaginatedResponse>

    @POST("v2/students")
    suspend fun createStudent(
        @Header("Authorization") token: String,
        @Body request: CreateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @GET("v2/students/{nis}")
    suspend fun getStudentDetail(
        @Header("Authorization") token: String,
        @Path("nis") nis: String
    ): Response<ApiResponse<com.google.gson.JsonObject>>

    @PUT("v2/students/{nis}")
    suspend fun updateStudent(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: UpdateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @DELETE("v2/students/{nis}")
    suspend fun deleteStudent(
        @Header("Authorization") token: String,
        @Path("nis") nis: String
    ): Response<MessageResponse>

    @GET("v2/students/me/attendance-history")
    suspend fun getMyAttendanceHistory(
        @Header("Authorization") token: String,
        @Query("week") week: Int? = null
    ): Response<HistorySiswaResponse>

    @POST("v2/students/{nis}/attendances")
    suspend fun submitAttendance(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: AttendanceCreateRequest
    ): Response<MessageResponse>

    @GET("v2/students/unregistered")
    suspend fun getUnregisteredStudents(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<SiswaItem>>>

    // --- Pengajuan Izin ---

    @GET("v2/pengajuan-izin")
    suspend fun getPengajuanIzinList(
        @Header("Authorization") token: String,
        @QueryMap filters: Map<String, String>
    ): Response<PengajuanIzinList>

    @POST("v2/pengajuan-izin")
    suspend fun createPengajuanIzin(
        @Header("Authorization") token: String,
        @Body request: CreatePengajuanIzinRequest
    ): Response<MessageResponse>

    @GET("v2/pengajuan-izin/{id}")
    suspend fun getPengajuanIzinDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ApiResponse<PengajuanIzin>>

    @PATCH("v2/pengajuan-izin/{id}/status")
    suspend fun updatePengajuanIzinStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateStatusPengajuanIzinRequest
    ): Response<MessageResponse>

    // --- Notifications ---

    @GET("v2/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<GetNotificationsResponse>

    @PATCH("v2/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @POST("v2/notifications/bulk-read")
    suspend fun bulkReadNotifications(
        @Header("Authorization") token: String,
        @Body request: BulkReadRequest
    ): Response<MessageResponse>

    @DELETE("v2/notifications/{id}")
    suspend fun deleteNotification(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    // --- Analytics ---

    @GET("v2/analytics/attendance")
    suspend fun getAttendanceAnalytics(): Response<StatisticsResponse>

    // --- Admin Control ---

    @GET("v2/admin/management/kelas")
    suspend fun getAdminManagementKelas(
        @Header("Authorization") token: String
    ): Response<KelasManagementListResponse>

    @GET("v2/admin/management/kelas/{id}")
    suspend fun getAdminManagementKelasDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<KelasManagementDetailResponse>

    @PUT("v2/admin/management/kelas/{id}/wali")
    suspend fun updateWaliKelas(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateWaliKelasRequest
    ): Response<MessageResponse>

    @GET("v2/admin/promotion/config")
    suspend fun getPromotionConfig(
        @Header("Authorization") token: String
    ): Response<TahunAjaranListResponse>

    @POST("v2/admin/promotion/config")
    suspend fun createPromotionConfig(
        @Header("Authorization") token: String,
        @Body request: TahunAjaranRequest
    ): Response<TahunAjaranResponse>

    @PUT("v2/admin/promotion/config/{id}")
    suspend fun updatePromotionConfig(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: TahunAjaranRequest
    ): Response<TahunAjaranResponse>

    @POST("v2/admin/promotion/execute")
    suspend fun executePromotion(
        @Header("Authorization") token: String,
        @Body request: PromotionRequest
    ): Response<PromotionResponse>

    @GET("v2/admin/student-control/overview")
    suspend fun getStudentControlOverview(
        @Header("Authorization") token: String
    ): Response<StudentControlOverviewResponse>

    @POST("v2/admin/device-management/change-device")
    suspend fun adminChangeDevice(
        @Header("Authorization") token: String,
        @Body request: ChangeDeviceRequest
    ): Response<AuthResponse>

    // --- Device Auth ---

    @POST("v2/device-auth/register")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: HardwareAuthRequest
    ): Response<DeviceInfoResponse>

    @POST("v2/device-auth/verify")
    suspend fun verifyDevice(
        @Header("Authorization") token: String,
        @Body request: HardwareAuthRequest
    ): Response<DeviceInfoResponse>

    @GET("v2/device-auth/info")
    suspend fun getDeviceAuthInfo(
        @Header("Authorization") token: String
    ): Response<DeviceInfoResponse>

    // --- Lookups ---

    @GET("v2/kelas")
    suspend fun getKelasLookup(
        @Header("Authorization") token: String
    ): Response<KelasListResponse>

    @GET("v2/jurusan")
    suspend fun getJurusanLookup(
        @Header("Authorization") token: String
    ): Response<JurusanListResponse>

    @GET("v2/lookup/staff-guru")
    suspend fun getStaffGuruLookup(
        @Header("Authorization") token: String
    ): Response<StaffLookupResponse>

    // --- Reports ---

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
