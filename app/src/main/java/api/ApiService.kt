package com.xirpl2.SASMobile.network

import com.xirpl2.SASMobile.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @POST("v2/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<MessageResponse>

    @POST("v2/auth/verify-account")
    suspend fun verifyAccount(
        @Header("Authorization") token: String,
        @Body request: VerifyAccountRequest
    ): Response<MessageResponse>

    @POST("v2/auth/tokens/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): Response<AuthResponse>

    /** Synchronous variant for use in OkHttp Authenticator (no coroutine context). */
    @POST("v2/auth/tokens/refresh")
    fun refreshTokenSync(
        @Body request: RefreshRequest
    ): retrofit2.Call<AuthResponse>

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

    @GET("v2/prayer-schedules/Duha/today")
    suspend fun getDuhaToday(
        @Header("Authorization") token: String
    ): Response<DuhaTodayResponse>

    @GET("v2/prayer-schedules/closest")
    suspend fun getClosestPrayerSchedule(
        @Header("Authorization") token: String
    ): Response<ClosestPrayerResponse>

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
    ): Response<QRCodeVerifyResponse>

    @GET("v2/attendance/code/generate")
    suspend fun generateAttendanceCode(
        @Header("Authorization") token: String
    ): Response<GenerateCodeResponse>

    @POST("v2/attendance/code/verify")
    suspend fun verifyAttendanceCode(
        @Header("Authorization") token: String,
        @Body request: VerifyCodeRequest
    ): Response<QRCodeVerifyResponse>

    // --- Students ---

    @GET("v2/students")
    suspend fun getStudents(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("page_size") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("tingkatan") tingkatan: Int? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("jk") jk: String? = null,
        @Query("agama") agama: String? = null
    ): Response<SiswaListPaginatedResponse>

    @POST("v2/students")
    suspend fun createStudent(
        @Header("Authorization") token: String,
        @Body request: CreateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @GET("v2/students/{nis}")
    suspend fun getStudentDetail(
        @Header("Authorization") token: String,
        @Path(value = "nis", encoded = true) nis: String
    ): Response<ApiResponse<StudentDetailResponse>>

    @PUT("v2/students/{nis}")
    suspend fun updateStudent(
        @Header("Authorization") token: String,
        @Path(value = "nis", encoded = true) nis: String,
        @Body request: UpdateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    @DELETE("v2/students/{nis}")
    suspend fun deleteStudent(
        @Header("Authorization") token: String,
        @Path(value = "nis", encoded = true) nis: String
    ): Response<MessageResponse>

    @GET("v2/students/me/attendance-history")
    suspend fun getMyAttendanceHistory(
        @Header("Authorization") token: String,
        @Query("week") week: Int? = null
    ): Response<HistorySiswaResponse>

    @POST("v2/students/{nis}/attendances")
    suspend fun submitAttendance(
        @Header("Authorization") token: String,
        @Path(value = "nis", encoded = true) nis: String,
        @Body request: CreateAbsensiRequest
    ): Response<MessageResponse>

    @GET("v2/students/unregistered")
    suspend fun getUnregisteredStudents(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("search") search: String? = null,
        @Query("jurusan") jurusan: Int? = null,
        @Query("wali_kelas") waliKelas: Int? = null,
        @Query("id_kelas") idKelas: Int? = null
    ): Response<SiswaListPaginatedResponse>

    // --- Synchronized SMKN 2 Singosari ---

    @GET("v2/prayer-schedules/Duha/keahlian")
    suspend fun getJadwalDuhaKeahlian(
        @Header("Authorization") token: String
    ): Response<JadwalDuhaKeahlianResponse>

    @GET("v2/prayer-schedules/Duha/detail")
    suspend fun getSholatDuhaDetail(
        @Header("Authorization") token: String
    ): Response<SholatDuhaDetailResponse>

    @GET("v2/prayer-schedules/Zuhur/detail")
    suspend fun getSholatZuhurDetail(
        @Header("Authorization") token: String
    ): Response<SholatZuhurDetailResponse>

    @POST("v2/prayer-schedules/Duha/keahlian")
    suspend fun createJadwalDuhaKeahlian(
        @Header("Authorization") token: String,
        @Body request: JadwalDuhaKeahlian
    ): Response<MessageResponse>

    @PUT("v2/prayer-schedules/Duha/detail")
    suspend fun updateSholatDuha(
        @Header("Authorization") token: String,
        @Body request: SholatDuhaDetail
    ): Response<MessageResponse>

    @PUT("v2/prayer-schedules/Zuhur/detail")
    suspend fun updateSholatZuhur(
        @Header("Authorization") token: String,
        @Body request: SholatZuhurDetail
    ): Response<MessageResponse>

    // --- Pengajuan Izin ---

    @GET("v2/pengajuan-izin")
    suspend fun getPengajuanIzinList(
        @Header("Authorization") token: String,
        @QueryMap filters: Map<String, String>
    ): Response<PengajuanIzinList>

    @Multipart
    @POST("v2/pengajuan-izin")
    suspend fun createPengajuanIzin(
        @Header("Authorization") token: String,
        @Part("jenis_izin") jenisIzin: RequestBody,
        @Part("tanggal_awal") tanggalAwal: RequestBody,
        @Part("tanggal_akhir") tanggalAkhir: RequestBody,
        @Part("keterangan") keterangan: RequestBody,
        @Part buktiFoto: MultipartBody.Part? = null
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

    @DELETE("v2/pengajuan-izin/{id}")
    suspend fun deletePengajuanIzin(
        @Header("Authorization") token: String,
        @Path("id") id: Int
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
    suspend fun getAttendanceAnalytics(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<StatisticsResponse>

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

    @POST("v2/admin/management/kelas")
    suspend fun createKelas(
        @Header("Authorization") token: String,
        @Body request: CreateKelasRequest
    ): Response<CreateKelasResponse>

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
        @Query("jurusan") jurusan: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("v2/reports/full/excel")
    suspend fun exportFullReport(
        @Header("Authorization") token: String,
        @Query("month") month: String,
        @Query("jurusan") jurusan: String
    ): Response<ResponseBody>

    // --- Analytics (FASE 3.1) ---

    @GET("v2/analytics/charts")
    suspend fun getChartData(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ApiResponse<com.google.gson.JsonObject>>

    @GET("v2/analytics/pending-attendance")
    suspend fun getPendingAttendance(
        @Header("Authorization") token: String
    ): Response<PendingAttendanceResponse>

    // --- Reports Tambahan (FASE 3.2) ---

    @Streaming
    @GET("v2/reports/attendances/csv")
    suspend fun exportAttendanceCSV(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("v2/reports/attendances/excel")
    suspend fun exportAttendanceExcel(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("jurusan") jurusan: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("v2/reports/summary/csv")
    suspend fun exportSummaryCSV(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("jurusan") jurusan: String? = null
    ): Response<ResponseBody>

    @Streaming
    @GET("v2/reports/summary/excel")
    suspend fun exportSummaryExcel(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("jurusan") jurusan: String? = null
    ): Response<ResponseBody>

    // --- Students Tambahan (FASE 3.3) ---

    @GET("v2/students/filters")
    suspend fun getStudentFilters(@Header("Authorization") token: String): Response<ApiResponse<com.google.gson.JsonObject>>

    @PATCH("v2/students/{nis}/status")
    suspend fun updateStudentStatus(@Header("Authorization") token: String, @Path(value = "nis", encoded = true) nis: String, @Body request: UpdateStatusRequest): Response<MessageResponse>

    @POST("v2/students/notify-wali-kelas")
    suspend fun notifyWaliKelas(@Header("Authorization") token: String, @Body request: NotifyWaliKelasRequest): Response<MessageResponse>

    @Multipart
    @POST("v2/students/import")
    suspend fun importStudents(@Header("Authorization") token: String, @Part file: MultipartBody.Part): Response<MessageResponse>

    @POST("v2/students/import/json")
    suspend fun importStudentsJSON(@Header("Authorization") token: String, @Body request: ImportStudentsRequest): Response<MessageResponse>

    // --- Admin Student Control (FASE 3.4) ---

    @GET("v2/admin/student-control/transitions")
    suspend fun getStudentTransitions(@Header("Authorization") token: String): Response<StudentTransitionsResponse>

    @POST("v2/admin/student-control/bulk-progression")
    suspend fun bulkStudentProgression(@Header("Authorization") token: String, @Body request: BulkProgressionRequest): Response<MessageResponse>

    @POST("v2/admin/student-control/bulk-fields")
    suspend fun updateBulkStudentFields(@Header("Authorization") token: String, @Body request: BulkFieldsRequest): Response<MessageResponse>

    @POST("v2/admin/student-control/annual-rollover")
    suspend fun annualRollover(@Header("Authorization") token: String, @Body request: AnnualRolloverRequest): Response<MessageResponse>

    @POST("v2/admin/student-control/sequential-progression")
    suspend fun sequentialProgression(@Header("Authorization") token: String, @Body request: SequentialProgressionRequest): Response<MessageResponse>

    // --- Admin Promotion - Simulate (FASE 3.5) ---

    @POST("v2/admin/promotion/simulate")
    suspend fun simulatePromotion(@Header("Authorization") token: String, @Body request: PromotionRequest): Response<PromotionResponse>

    // --- Academic Years CRUD (FASE 3.6) ---

    @GET("v2/academic-years")
    suspend fun getAcademicYears(@Header("Authorization") token: String): Response<AcademicYearListResponse>

    @POST("v2/academic-years")
    suspend fun createAcademicYear(@Header("Authorization") token: String, @Body request: AcademicYearRequest): Response<AcademicYearResponse>

    @GET("v2/academic-years/{id}")
    suspend fun getAcademicYear(@Header("Authorization") token: String, @Path("id") id: Int): Response<AcademicYearResponse>

    @PUT("v2/academic-years/{id}")
    suspend fun updateAcademicYear(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: AcademicYearRequest): Response<AcademicYearResponse>

    @DELETE("v2/academic-years/{id}")
    suspend fun deleteAcademicYear(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    // --- Profile Devices (FASE 3.7) ---

    @GET("v2/profile/devices")
    suspend fun getProfileDevices(@Header("Authorization") token: String): Response<DeviceInfoResponse>

    @DELETE("v2/profile/devices")
    suspend fun unbindDevice(@Header("Authorization") token: String): Response<MessageResponse>

    // --- Admin Device Management (FASE 3.8) ---

    @GET("v2/admin/device-management")
    suspend fun getAdminDeviceManagement(@Header("Authorization") token: String): Response<DeviceManagementListResponse>

    @DELETE("v2/admin/device-management/{id}")
    suspend fun deleteAdminDevice(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    @GET("v2/admin/device-management/change-requests")
    suspend fun getDeviceChangeRequests(@Header("Authorization") token: String): Response<DeviceChangeRequestListResponse>

    @Headers("Content-Type: application/json")
    @PUT("v2/admin/device-management/change-requests/{id}/{action}")
    suspend fun processDeviceChangeRequest(@Header("Authorization") token: String, @Path("id") id: Int, @Path("action") action: String): Response<MessageResponse>

    // --- Device Change Request (FASE 3.9) ---

    @POST("v2/device/change-request")
    suspend fun createDeviceChangeRequest(@Header("Authorization") token: String, @Body request: DeviceChangeRequestBody): Response<MessageResponse>

    // --- Class Teachers CRUD (FASE 3.10) ---

    @GET("v2/class-teachers")
    suspend fun getClassTeachers(@Header("Authorization") token: String): Response<ClassTeacherListResponse>

    @POST("v2/class-teachers")
    suspend fun createClassTeacher(@Header("Authorization") token: String, @Body request: ClassTeacherRequest): Response<ClassTeacherResponse>

    @GET("v2/class-teachers/{id}")
    suspend fun getClassTeacher(@Header("Authorization") token: String, @Path("id") id: Int): Response<ClassTeacherResponse>

    @PUT("v2/class-teachers/{id}")
    suspend fun updateClassTeacher(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: ClassTeacherRequest): Response<ClassTeacherResponse>

    @DELETE("v2/class-teachers/{id}")
    suspend fun deleteClassTeacher(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    // --- Prayer Types CRUD (FASE 3.11) ---

    @GET("v2/prayer-types")
    suspend fun getPrayerTypes(@Header("Authorization") token: String): Response<PrayerTypeListResponse>

    @POST("v2/prayer-types")
    suspend fun createPrayerType(@Header("Authorization") token: String, @Body request: PrayerTypeRequest): Response<PrayerTypeResponse>

    @GET("v2/prayer-types/{id}")
    suspend fun getPrayerType(@Header("Authorization") token: String, @Path("id") id: Int): Response<PrayerTypeResponse>

    @PUT("v2/prayer-types/{id}")
    suspend fun updatePrayerType(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: PrayerTypeRequest): Response<PrayerTypeResponse>

    @DELETE("v2/prayer-types/{id}")
    suspend fun deletePrayerType(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    // --- Prayer Times CRUD (FASE 3.12) ---

    @GET("v2/prayer-times")
    suspend fun getPrayerTimes(@Header("Authorization") token: String): Response<PrayerTimeListResponse>

    @POST("v2/prayer-times")
    suspend fun createPrayerTime(@Header("Authorization") token: String, @Body request: PrayerTimeRequest): Response<PrayerTimeResponse>

    @GET("v2/prayer-times/{id}")
    suspend fun getPrayerTime(@Header("Authorization") token: String, @Path("id") id: Int): Response<PrayerTimeResponse>

    @PUT("v2/prayer-times/{id}")
    suspend fun updatePrayerTime(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: PrayerTimeRequest): Response<PrayerTimeResponse>

    @DELETE("v2/prayer-times/{id}")
    suspend fun deletePrayerTime(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    // --- Duha & Jurusan (FASE 3.13) ---

    @GET("v2/jurusan/Duha-schedules")
    suspend fun getJurusanDuhaSchedules(@Header("Authorization") token: String): Response<JurusanDuhaSchedulesResponse>

    @PUT("v2/jurusan/{id}/Duha-day")
    suspend fun updateJurusanDuhaDay(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: DuhaDayRequest): Response<MessageResponse>

    @GET("v2/Duha-groups")
    suspend fun getDuhaGroups(@Header("Authorization") token: String): Response<DuhaGroupListResponse>

    @POST("v2/Duha-groups")
    suspend fun createDuhaGroup(@Header("Authorization") token: String, @Body request: DuhaGroupRequest): Response<DuhaGroupResponse>

    @PUT("v2/Duha-groups/{id}")
    suspend fun updateDuhaGroup(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: DuhaGroupRequest): Response<DuhaGroupResponse>

    @POST("v2/Duha-groups/weekly")
    suspend fun createWeeklyDuhaGroup(@Header("Authorization") token: String, @Body request: WeeklyDuhaGroupRequest): Response<MessageResponse>

    @DELETE("v2/Duha-groups/{id}")
    suspend fun deleteDuhaGroup(@Header("Authorization") token: String, @Path("id") id: Int): Response<MessageResponse>

    @PUT("v2/prayer-schedules/Duha/keahlian/{id}")
    suspend fun updateDuhaKeahlian(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalDuhaKeahlianUpdateRequest
    ): Response<MessageResponse>

    // --- Pengajuan Izin Tambahan (FASE 3.14) ---

    @GET("v2/pengajuan-izin/{id}/bukti")
    suspend fun getBuktiFoto(@Header("Authorization") token: String, @Path("id") id: Int): Response<ApiResponse<com.google.gson.JsonObject>>

    // --- Prayer Schedules (FASE 3.15) ---

    @PUT("v2/prayer-schedules/Duha/{id}")
    suspend fun updateJadwalDuhaTime(@Header("Authorization") token: String, @Path("id") idJurusan: Int, @Body request: com.xirpl2.SASMobile.model.JadwalDuhaTimeUpdateRequest): Response<MessageResponse>

    @GET("v2/prayer-schedules/Duha/turns")
    suspend fun getDuhaTurnsToday(@Header("Authorization") token: String): Response<DuhaTurnsResponse>

    // --- Data Retention (FASE 3.16) ---

    @GET("v2/data-retention/backups/status")
    suspend fun getBackupsStatus(@Header("Authorization") token: String): Response<BackupStatusResponse>

    @POST("v2/data-retention/backups/confirm")
    suspend fun confirmBackup(@Header("Authorization") token: String, @Body request: BackupConfirmRequest): Response<MessageResponse>

    @DELETE("v2/data-retention/backups")
    suspend fun deleteBackup(@Header("Authorization") token: String): Response<MessageResponse>

    // --- Perizinan Halangan ---

    @POST("v2/perizinan/halangan/request")
    suspend fun requestHalangan(
        @Header("Authorization") token: String,
        @Body request: com.xirpl2.SASMobile.model.RequestHalanganBody
    ): Response<com.xirpl2.SASMobile.model.ApiResponse<com.xirpl2.SASMobile.model.RequestHalanganData>>

    @POST("v2/perizinan/halangan/validate")
    suspend fun validateHalangan(
        @Header("Authorization") token: String,
        @Body request: com.xirpl2.SASMobile.model.ValidateHalanganBody
    ): Response<com.xirpl2.SASMobile.model.MessageResponse>

    @GET("v2/perizinan/halangan/status/{siswa_id}")
    suspend fun getHalanganStatus(
        @Header("Authorization") token: String,
        @Path("siswa_id") siswaId: Int
    ): Response<com.xirpl2.SASMobile.model.ApiResponse<com.xirpl2.SASMobile.model.HalanganStatusData>>

    // --- Reports: PDF Export ---

    @GET("v2/reports/attendances/pdf")
    @Streaming
    suspend fun exportAttendancePdf(
        @Header("Authorization") token: String,
        @Query("tanggal") tanggal: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null
    ): Response<ResponseBody>

    @GET("v2/reports/attendance/pdf")
    @Streaming
    suspend fun exportAttendanceReportPdf(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("jurusan") jurusan: String? = null,
        @Query("kelas") kelas: String? = null,
        @Query("jenis_sholat") jenisSholat: String? = null,
        @Query("search") search: String? = null
    ): Response<ResponseBody>

    // --- Admin Guru Management ---

    @GET("v2/admin/management/guru")
    suspend fun getAdminGuruList(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("has_wali_kelas") hasWaliKelas: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("sort_order") sortOrder: String? = null
    ): Response<GuruListResponse>

    @POST("v2/admin/management/guru")
    suspend fun createGuru(
        @Header("Authorization") token: String,
        @Body request: CreateGuruRequest
    ): Response<GuruDetailResponse>

    @GET("v2/admin/management/guru/{id}")
    suspend fun getGuruDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<GuruDetailResponse>

    @PUT("v2/admin/management/guru/{id}")
    suspend fun updateGuru(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateGuruRequest
    ): Response<GuruDetailResponse>

    @DELETE("v2/admin/management/guru/{id}")
    suspend fun deleteGuru(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    @PUT("v2/admin/management/guru/{id}/wali-kelas")
    suspend fun assignGuruWaliKelas(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: AssignWaliKelasGuruRequest
    ): Response<GuruDetailResponse>

    @DELETE("v2/admin/management/guru/{id}/wali-kelas")
    suspend fun removeGuruWaliKelas(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    // --- Admin Wali Kelas Management ---

    @GET("v2/admin/management/wali-kelas")
    suspend fun getAdminWaliKelasList(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("kelas_id") kelasId: Int? = null,
        @Query("staff_id") staffId: Int? = null
    ): Response<WaliKelasManagementListResponse>

    @GET("v2/admin/management/wali-kelas/history")
    suspend fun getAdminWaliKelasHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("is_active") isActive: String? = null
    ): Response<WaliKelasManagementListResponse>
}
