package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.ClosestPrayerResponse
import com.xirpl2.SASMobile.network.generated.model.DhuhaTodayResponse
import com.xirpl2.SASMobile.network.generated.model.JadwalDhuhaKeahlianResponse
import com.xirpl2.SASMobile.network.generated.model.JadwalSholatDetailResponse
import com.xirpl2.SASMobile.network.generated.model.JadwalSholatListResponse
import com.xirpl2.SASMobile.network.generated.model.JadwalSholatTodayResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.SholatDhuhaDetailResponse
import com.xirpl2.SASMobile.network.generated.model.SholatDzuhurDetailResponse

interface PrayerSchedulesApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [ClosestPrayerResponse]
     */
    @GET("v2/prayer-schedules/closest")
    suspend fun v2PrayerSchedulesClosestGet(): Response<ClosestPrayerResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [SholatDhuhaDetailResponse]
     */
    @GET("v2/prayer-schedules/dhuha/detail")
    suspend fun v2PrayerSchedulesDhuhaDetailGet(): Response<SholatDhuhaDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @PUT("v2/prayer-schedules/dhuha/detail")
    suspend fun v2PrayerSchedulesDhuhaDetailPut(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @PUT("v2/prayer-schedules/dhuha/{id}")
    suspend fun v2PrayerSchedulesDhuhaIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [JadwalDhuhaKeahlianResponse]
     */
    @GET("v2/prayer-schedules/dhuha/keahlian")
    suspend fun v2PrayerSchedulesDhuhaKeahlianGet(): Response<JadwalDhuhaKeahlianResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @PUT("v2/prayer-schedules/dhuha/keahlian/{id}")
    suspend fun v2PrayerSchedulesDhuhaKeahlianIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/prayer-schedules/dhuha/keahlian")
    suspend fun v2PrayerSchedulesDhuhaKeahlianPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [DhuhaTodayResponse]
     */
    @GET("v2/prayer-schedules/dhuha/today")
    suspend fun v2PrayerSchedulesDhuhaTodayGet(): Response<DhuhaTodayResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/prayer-schedules/dhuha/turns")
    suspend fun v2PrayerSchedulesDhuhaTurnsGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [SholatDzuhurDetailResponse]
     */
    @GET("v2/prayer-schedules/dzuhur/detail")
    suspend fun v2PrayerSchedulesDzuhurDetailGet(): Response<SholatDzuhurDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @PUT("v2/prayer-schedules/dzuhur/detail")
    suspend fun v2PrayerSchedulesDzuhurDetailPut(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param limit  (optional)
     * @return [JadwalSholatListResponse]
     */
    @GET("v2/prayer-schedules")
    suspend fun v2PrayerSchedulesGet(@Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null): Response<JadwalSholatListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/prayer-schedules/{id}")
    suspend fun v2PrayerSchedulesIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [JadwalSholatDetailResponse]
     */
    @GET("v2/prayer-schedules/{id}")
    suspend fun v2PrayerSchedulesIdGet(@Path("id") id: kotlin.Int): Response<JadwalSholatDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [JadwalSholatDetailResponse]
     */
    @PUT("v2/prayer-schedules/{id}")
    suspend fun v2PrayerSchedulesIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<JadwalSholatDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [JadwalSholatDetailResponse]
     */
    @POST("v2/prayer-schedules")
    suspend fun v2PrayerSchedulesPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<JadwalSholatDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [JadwalSholatTodayResponse]
     */
    @GET("v2/prayer-schedules/today")
    suspend fun v2PrayerSchedulesTodayGet(): Response<JadwalSholatTodayResponse>

}
