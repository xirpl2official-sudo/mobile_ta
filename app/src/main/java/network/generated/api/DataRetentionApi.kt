package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface DataRetentionApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/data-retention/backups/confirm")
    suspend fun v2DataRetentionBackupsConfirmPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [MessageResponse]
     */
    @DELETE("v2/data-retention/backups")
    suspend fun v2DataRetentionBackupsDelete(): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/data-retention/backups/status")
    suspend fun v2DataRetentionBackupsStatusGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

}
