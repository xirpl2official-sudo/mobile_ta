package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.AuthResponse
import com.xirpl2.SASMobile.network.generated.model.DeviceChangeRequestListResponse
import com.xirpl2.SASMobile.network.generated.model.DeviceManagementListResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface DeviceManagementApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [AuthResponse]
     */
    @POST("v2/admin/device-management/change-device")
    suspend fun v2AdminDeviceManagementChangeDevicePost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<AuthResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [DeviceChangeRequestListResponse]
     */
    @GET("v2/admin/device-management/change-requests")
    suspend fun v2AdminDeviceManagementChangeRequestsGet(): Response<DeviceChangeRequestListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param action 
     * @return [MessageResponse]
     */
    @PUT("v2/admin/device-management/change-requests/{id}/{action}")
    suspend fun v2AdminDeviceManagementChangeRequestsIdActionPut(@Path("id") id: kotlin.Int, @Path("action") action: kotlin.String): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/admin/device-management")
    suspend fun v2AdminDeviceManagementDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [DeviceManagementListResponse]
     */
    @GET("v2/admin/device-management")
    suspend fun v2AdminDeviceManagementGet(): Response<DeviceManagementListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/admin/device-management/{id}")
    suspend fun v2AdminDeviceManagementIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/device/change-request")
    suspend fun v2DeviceChangeRequestPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

}
