package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import okhttp3.ResponseBody
import com.google.gson.annotations.SerializedName


interface ReportsApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @param kelas  (optional)
     * @param jenisSholat  (optional)
     * @param search  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/attendance/excel")
    suspend fun v2ReportsAttendanceExcelGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("kelas") kelas: kotlin.String? = null, @Query("jenis_sholat") jenisSholat: kotlin.String? = null, @Query("search") search: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @param kelas  (optional)
     * @param jenisSholat  (optional)
     * @param search  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/attendance/pdf")
    suspend fun v2ReportsAttendancePdfGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("kelas") kelas: kotlin.String? = null, @Query("jenis_sholat") jenisSholat: kotlin.String? = null, @Query("search") search: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @param kelas  (optional)
     * @param jenisSholat  (optional)
     * @param search  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/attendances/csv")
    suspend fun v2ReportsAttendancesCsvGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("kelas") kelas: kotlin.String? = null, @Query("jenis_sholat") jenisSholat: kotlin.String? = null, @Query("search") search: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/attendances/excel")
    suspend fun v2ReportsAttendancesExcelGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param tanggal  (optional)
     * @param kelas  (optional)
     * @param jurusan  (optional)
     * @param jenisSholat  (optional)
     * @param search  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/attendances/pdf")
    suspend fun v2ReportsAttendancesPdfGet(@Query("tanggal") tanggal: kotlin.String? = null, @Query("kelas") kelas: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("jenis_sholat") jenisSholat: kotlin.String? = null, @Query("search") search: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param month  (optional)
     * @param jurusan  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/full/excel")
    suspend fun v2ReportsFullExcelGet(@Query("month") month: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/summary/csv")
    suspend fun v2ReportsSummaryCsvGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param jurusan  (optional)
     * @return [ResponseBody]
     */
    @GET("v2/reports/summary/excel")
    suspend fun v2ReportsSummaryExcelGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null): Response<ResponseBody>

}
