package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.PrayerTypeListResponse
import com.xirpl2.SASMobile.network.generated.model.PrayerTypeResponse

interface PrayerTypesApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [PrayerTypeListResponse]
     */
    @GET("v2/prayer-types")
    suspend fun v2PrayerTypesGet(): Response<PrayerTypeListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/prayer-types/{id}")
    suspend fun v2PrayerTypesIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [PrayerTypeResponse]
     */
    @GET("v2/prayer-types/{id}")
    suspend fun v2PrayerTypesIdGet(@Path("id") id: kotlin.Int): Response<PrayerTypeResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [PrayerTypeResponse]
     */
    @PUT("v2/prayer-types/{id}")
    suspend fun v2PrayerTypesIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<PrayerTypeResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [PrayerTypeResponse]
     */
    @POST("v2/prayer-types")
    suspend fun v2PrayerTypesPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<PrayerTypeResponse>

}
