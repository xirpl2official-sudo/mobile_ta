package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import okhttp3.ResponseBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.GenerateCodeResponse
import com.xirpl2.SASMobile.network.generated.model.HistoryStaffResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.QRCodeGenerateResponse
import com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyRequest
import com.xirpl2.SASMobile.network.generated.model.QRCodeVerifyResponse
import com.xirpl2.SASMobile.network.generated.model.VerifyCodeRequest

interface AttendanceApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [MessageResponse]
     */
    @POST("v2/attendance/auto-mark")
    suspend fun v2AttendanceAutoMarkPost(): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [GenerateCodeResponse]
     */
    @GET("v2/attendance/code/generate")
    suspend fun v2AttendanceCodeGenerateGet(): Response<GenerateCodeResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param verifyCodeRequest  (optional)
     * @return [QRCodeVerifyResponse]
     */
    @POST("v2/attendance/code/verify")
    suspend fun v2AttendanceCodeVerifyPost(@Body verifyCodeRequest: VerifyCodeRequest? = null): Response<QRCodeVerifyResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param kelas  (optional)
     * @param jurusan  (optional)
     * @param jenisSholat  (optional)
     * @param search  (optional)
     * @param tanggal  (optional)
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @param page  (optional)
     * @param limit  (optional)
     * @return [HistoryStaffResponse]
     */
    @GET("v2/attendance/history")
    suspend fun v2AttendanceHistoryGet(@Query("kelas") kelas: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("jenis_sholat") jenisSholat: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("tanggal") tanggal: kotlin.String? = null, @Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null, @Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null): Response<HistoryStaffResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [QRCodeGenerateResponse]
     */
    @GET("v2/attendance/qr-codes/current")
    suspend fun v2AttendanceQrCodesCurrentGet(): Response<QRCodeGenerateResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [ResponseBody]
     */
    @GET("v2/attendance/qr-codes/current/image")
    suspend fun v2AttendanceQrCodesCurrentImageGet(): Response<ResponseBody>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param qrCodeVerifyRequest  (optional)
     * @return [QRCodeVerifyResponse]
     */
    @POST("v2/attendance/qr-codes/verify")
    suspend fun v2AttendanceQrCodesVerifyPost(@Body qrCodeVerifyRequest: QRCodeVerifyRequest? = null): Response<QRCodeVerifyResponse>

}
