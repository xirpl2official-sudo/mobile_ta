package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.AcademicYearListResponse
import com.xirpl2.SASMobile.network.generated.model.AcademicYearResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface AcademicYearsApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [AcademicYearListResponse]
     */
    @GET("v2/academic-years")
    suspend fun v2AcademicYearsGet(): Response<AcademicYearListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/academic-years/{id}")
    suspend fun v2AcademicYearsIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [AcademicYearResponse]
     */
    @GET("v2/academic-years/{id}")
    suspend fun v2AcademicYearsIdGet(@Path("id") id: kotlin.Int): Response<AcademicYearResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [AcademicYearResponse]
     */
    @PUT("v2/academic-years/{id}")
    suspend fun v2AcademicYearsIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<AcademicYearResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [AcademicYearResponse]
     */
    @POST("v2/academic-years")
    suspend fun v2AcademicYearsPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<AcademicYearResponse>

}
