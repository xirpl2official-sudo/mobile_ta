package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.HistorySiswaResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse

import okhttp3.MultipartBody

interface StudentsApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/students/filters")
    suspend fun v2StudentsFiltersGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param pageSize  (optional)
     * @param search  (optional)
     * @param tingkatan  (optional)
     * @param jurusan  (optional)
     * @param jk  (optional)
     * @param agama  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/students")
    suspend fun v2StudentsGet(@Query("page") page: kotlin.Int? = null, @Query("page_size") pageSize: kotlin.Int? = null, @Query("search") search: kotlin.String? = null, @Query("tingkatan") tingkatan: kotlin.Int? = null, @Query("jurusan") jurusan: kotlin.String? = null, @Query("jk") jk: kotlin.String? = null, @Query("agama") agama: kotlin.String? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/students/import/json")
    suspend fun v2StudentsImportJsonPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param file  (optional)
     * @return [MessageResponse]
     */
    @Multipart
    @POST("v2/students/import")
    suspend fun v2StudentsImportPost(@Part file: MultipartBody.Part? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param week  (optional)
     * @return [HistorySiswaResponse]
     */
    @GET("v2/students/me/attendance-history")
    suspend fun v2StudentsMeAttendanceHistoryGet(@Query("week") week: kotlin.Int? = null): Response<HistorySiswaResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param nis 
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/students/{nis}/attendances")
    suspend fun v2StudentsNisAttendancesPost(@Path("nis") nis: kotlin.String, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param nis 
     * @return [MessageResponse]
     */
    @DELETE("v2/students/{nis}")
    suspend fun v2StudentsNisDelete(@Path("nis") nis: kotlin.String): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param nis 
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/students/{nis}")
    suspend fun v2StudentsNisGet(@Path("nis") nis: kotlin.String): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param nis 
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @PUT("v2/students/{nis}")
    suspend fun v2StudentsNisPut(@Path("nis") nis: kotlin.String, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param nis 
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @PATCH("v2/students/{nis}/status")
    suspend fun v2StudentsNisStatusPatch(@Path("nis") nis: kotlin.String, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/students/notify-wali-kelas")
    suspend fun v2StudentsNotifyWaliKelasPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @POST("v2/students")
    suspend fun v2StudentsPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param pageSize  (optional)
     * @param search  (optional)
     * @param jurusan  (optional)
     * @param waliKelas  (optional)
     * @param idKelas  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/students/unregistered")
    suspend fun v2StudentsUnregisteredGet(@Query("page") page: kotlin.Int? = null, @Query("page_size") pageSize: kotlin.Int? = null, @Query("search") search: kotlin.String? = null, @Query("jurusan") jurusan: kotlin.Int? = null, @Query("wali_kelas") waliKelas: kotlin.Int? = null, @Query("id_kelas") idKelas: kotlin.Int? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

}
