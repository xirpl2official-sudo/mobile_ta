package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.PrayerTimeListResponse
import com.xirpl2.SASMobile.network.generated.model.PrayerTimeResponse
import com.xirpl2.SASMobile.network.generated.model.PrayerTypeResponse

interface PrayerTimesApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [PrayerTimeListResponse]
     */
    @GET("v2/prayer-times")
    suspend fun v2PrayerTimesGet(): Response<PrayerTimeListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/prayer-times/{id}")
    suspend fun v2PrayerTimesIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [PrayerTimeResponse]
     */
    @GET("v2/prayer-times/{id}")
    suspend fun v2PrayerTimesIdGet(@Path("id") id: kotlin.Int): Response<PrayerTimeResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [PrayerTimeResponse]
     */
    @PUT("v2/prayer-times/{id}")
    suspend fun v2PrayerTimesIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<PrayerTimeResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [PrayerTypeResponse]
     */
    @POST("v2/prayer-times")
    suspend fun v2PrayerTimesPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<PrayerTypeResponse>

}
