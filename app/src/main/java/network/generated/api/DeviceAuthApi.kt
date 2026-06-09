package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.DeviceInfoResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface DeviceAuthApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [DeviceInfoResponse]
     */
    @GET("v2/device-auth/info")
    suspend fun v2DeviceAuthInfoGet(): Response<DeviceInfoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [DeviceInfoResponse]
     */
    @POST("v2/device-auth/register")
    suspend fun v2DeviceAuthRegisterPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<DeviceInfoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [DeviceInfoResponse]
     */
    @POST("v2/device-auth/verify")
    suspend fun v2DeviceAuthVerifyPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<DeviceInfoResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [MessageResponse]
     */
    @DELETE("v2/profile/devices")
    suspend fun v2ProfileDevicesDelete(): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [DeviceInfoResponse]
     */
    @GET("v2/profile/devices")
    suspend fun v2ProfileDevicesGet(): Response<DeviceInfoResponse>

}
