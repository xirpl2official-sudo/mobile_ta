package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.StatisticsResponse

interface AnalyticsApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @return [StatisticsResponse]
     */
    @GET("v2/analytics/attendance")
    suspend fun v2AnalyticsAttendanceGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null): Response<StatisticsResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param startDate  (optional)
     * @param endDate  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/analytics/charts")
    suspend fun v2AnalyticsChartsGet(@Query("start_date") startDate: kotlin.String? = null, @Query("end_date") endDate: kotlin.String? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/analytics/pending-attendance")
    suspend fun v2AnalyticsPendingAttendanceGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

}
