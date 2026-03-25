package com.xirpl2.SASMobile.network

import com.xirpl2.SASMobile.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== Auth Endpoints ==========
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

    // ========== Beranda Endpoints ==========
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

    /**
     * Get jadwal sholat by ID
     * GET /api/jadwal-sholat/{id}
     */
    @GET("jadwal-sholat/{id}")
    suspend fun getJadwalSholatById(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<JadwalSholatDetailResponse>

    /**
     * Update jadwal sholat
     * PUT /api/jadwal-sholat/{id}
     */
    @PUT("jadwal-sholat/{id}")
    suspend fun updateJadwalSholat(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: JadwalSholatUpdateRequest
    ): Response<JadwalSholatDetailResponse>

    /**
     * Delete jadwal sholat
     * DELETE /api/jadwal-sholat/{id}
     */
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

    /**
     * Get today's attendance statistics
     * No authentication required
     * Response format: {"message":"...", "data":{...}}
     */
    @GET("statistics")
    suspend fun getStatistics(): Response<StatisticsResponse>

    /**
     * Get Dhuha prayer schedules for today (all scheduled jurusan)
     */
    @GET("jadwal-sholat/dhuha-today")
    suspend fun getDhuhaToday(
        @Header("Authorization") token: String
    ): Response<DhuhaTodayResponse>

    @POST("absensi")
    suspend fun submitAbsensi(
        @Header("Authorization") token: String,
        @Body absensiData: AbsensiRequest
    ): Response<ApiResponse<AbsensiResponse>>

    // ========== QR Code Endpoints ==========
    
    /**
     * Generate QR code for attendance (Staff only)
     * GET /api/qrcode/generate
     * Authentication: Bearer token (admin/guru/wali_kelas role required)
     * Purpose: Staff displays QR code on screen/projector for students to scan
     * Returns: Base64 encoded QR code image with token and expiry info (5 min)
     */
    @GET("qrcode/generate")
    suspend fun generateQRCode(
        @Header("Authorization") token: String
    ): Response<QRCodeGenerateResponse>

    /**
     * Scan and verify QR code to record attendance (Student only)
     * POST /api/qrcode/verify
     * Authentication: Bearer token (siswa/student role required)
     * Purpose: Student scans QR code displayed by staff to record their attendance
     * Returns: Attendance confirmation with student info
     */
    @POST("qrcode/verify")
    suspend fun verifyQRCode(
        @Header("Authorization") token: String,
        @Body request: QRCodeVerifyRequest
    ): Response<QRCodeVerifyResponse>

    /**
     * Get attendance history for staff (admin/guru/wali_kelas)
     * GET /api/history/staff
     * Returns all students' attendance with summary statistics
     */
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
        @Query("tanggal") tanggal: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<HistoryStaffResponse>

    // ========== Siswa Endpoints ==========
    
    /**
     * Get list of all students with pagination
     * GET /api/siswa
     * Authentication: Bearer token required
     * Supports search, kelas, and jurusan filters
     */
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

    /**
     * Create new student
     * POST /api/siswa
     * Authentication: Bearer token required
     */
    @POST("siswa")
    suspend fun createSiswa(
        @Header("Authorization") token: String,
        @Body request: CreateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    /**
     * Update existing student
     * PUT /api/siswa/{nis}
     * Authentication: Bearer token required
     */
    @PUT("siswa/{nis}")
    suspend fun updateSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: UpdateSiswaRequest
    ): Response<ApiResponse<SiswaItem>>

    /**
     * Delete student
     * DELETE /api/siswa/{nis}
     * Authentication: Bearer token required
     */
    @DELETE("siswa/{nis}")
    suspend fun deleteSiswa(
        @Header("Authorization") token: String,
        @Path("nis") nis: String
    ): Response<ApiResponse<String>>

    /**
     * Create manual absensi (Izin/Sakit)
     * POST /api/siswa/{nis}/absensi
     */
    @POST("siswa/{nis}/absensi")
    suspend fun createAbsensi(
        @Header("Authorization") token: String,
        @Path("nis") nis: String,
        @Body request: CreateAbsensiRequest
    ): Response<ApiResponse<ManualAbsensiResponse>>

    // ========== Notification Endpoints ==========

    /**
     * Get pending attendance notifications
     * GET /api/notifications
     * Authentication: Bearer token required (admin/guru/wali_kelas)
     * Returns list of students who haven't marked attendance for active prayers
     */
    @GET("notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationResponse>
}