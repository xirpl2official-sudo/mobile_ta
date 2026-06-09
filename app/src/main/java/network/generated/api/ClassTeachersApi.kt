package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.MessageResponse

interface ClassTeachersApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/class-teachers")
    suspend fun v2ClassTeachersGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/class-teachers/{id}")
    suspend fun v2ClassTeachersIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/class-teachers/{id}")
    suspend fun v2ClassTeachersIdGet(@Path("id") id: kotlin.Int): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @PUT("v2/class-teachers/{id}")
    suspend fun v2ClassTeachersIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @POST("v2/class-teachers")
    suspend fun v2ClassTeachersPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

}
