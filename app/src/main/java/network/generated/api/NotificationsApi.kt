package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.BulkReadRequest
import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface NotificationsApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param bulkReadRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/notifications/bulk-read")
    suspend fun v2NotificationsBulkReadPost(@Body bulkReadRequest: BulkReadRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param limit  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/notifications")
    suspend fun v2NotificationsGet(@Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/notifications/{id}")
    suspend fun v2NotificationsIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @PATCH("v2/notifications/{id}/read")
    suspend fun v2NotificationsIdReadPatch(@Path("id") id: kotlin.Int): Response<MessageResponse>

}
