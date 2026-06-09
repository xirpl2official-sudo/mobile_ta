package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.JurusanListResponse

interface JurusanApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [JurusanListResponse]
     */
    @GET("v2/jurusan")
    suspend fun v2JurusanGet(): Response<JurusanListResponse>

}
