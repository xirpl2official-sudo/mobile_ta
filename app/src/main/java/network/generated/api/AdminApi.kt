package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.AnnualRolloverRequest
import com.xirpl2.SASMobile.network.generated.model.AssignWaliKelasGuruRequest
import com.xirpl2.SASMobile.network.generated.model.CreateKelasResponse
import com.xirpl2.SASMobile.network.generated.model.GuruDetailResponse
import com.xirpl2.SASMobile.network.generated.model.KelasManagementDetailResponse
import com.xirpl2.SASMobile.network.generated.model.KelasManagementListResponse
import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.UpdateWaliKelasRequest

interface AdminApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param limit  (optional)
     * @param search  (optional)
     * @param hasWaliKelas  (optional)
     * @param sortBy  (optional)
     * @param sortOrder  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/management/guru")
    suspend fun v2AdminManagementGuruGet(@Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null, @Query("search") search: kotlin.String? = null, @Query("has_wali_kelas") hasWaliKelas: kotlin.String? = null, @Query("sort_by") sortBy: kotlin.String? = null, @Query("sort_order") sortOrder: kotlin.String? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/admin/management/guru/{id}")
    suspend fun v2AdminManagementGuruIdDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [GuruDetailResponse]
     */
    @GET("v2/admin/management/guru/{id}")
    suspend fun v2AdminManagementGuruIdGet(@Path("id") id: kotlin.Int): Response<GuruDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param requestBody  (optional)
     * @return [GuruDetailResponse]
     */
    @PUT("v2/admin/management/guru/{id}")
    suspend fun v2AdminManagementGuruIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<GuruDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [MessageResponse]
     */
    @DELETE("v2/admin/management/guru/{id}/wali-kelas")
    suspend fun v2AdminManagementGuruIdWaliKelasDelete(@Path("id") id: kotlin.Int): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param assignWaliKelasGuruRequest  (optional)
     * @return [GuruDetailResponse]
     */
    @PUT("v2/admin/management/guru/{id}/wali-kelas")
    suspend fun v2AdminManagementGuruIdWaliKelasPut(@Path("id") id: kotlin.Int, @Body assignWaliKelasGuruRequest: AssignWaliKelasGuruRequest? = null): Response<GuruDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [GuruDetailResponse]
     */
    @POST("v2/admin/management/guru")
    suspend fun v2AdminManagementGuruPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<GuruDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [KelasManagementListResponse]
     */
    @GET("v2/admin/management/kelas")
    suspend fun v2AdminManagementKelasGet(): Response<KelasManagementListResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @return [KelasManagementDetailResponse]
     */
    @GET("v2/admin/management/kelas/{id}")
    suspend fun v2AdminManagementKelasIdGet(@Path("id") id: kotlin.Int): Response<KelasManagementDetailResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param id 
     * @param updateWaliKelasRequest  (optional)
     * @return [MessageResponse]
     */
    @PUT("v2/admin/management/kelas/{id}/wali")
    suspend fun v2AdminManagementKelasIdWaliPut(@Path("id") id: kotlin.Int, @Body updateWaliKelasRequest: UpdateWaliKelasRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [CreateKelasResponse]
     */
    @POST("v2/admin/management/kelas")
    suspend fun v2AdminManagementKelasPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<CreateKelasResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param limit  (optional)
     * @param search  (optional)
     * @param kelasId  (optional)
     * @param staffId  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/management/wali-kelas")
    suspend fun v2AdminManagementWaliKelasGet(@Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null, @Query("search") search: kotlin.String? = null, @Query("kelas_id") kelasId: kotlin.Int? = null, @Query("staff_id") staffId: kotlin.Int? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param page  (optional)
     * @param limit  (optional)
     * @param search  (optional)
     * @param isActive  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/management/wali-kelas/history")
    suspend fun v2AdminManagementWaliKelasHistoryGet(@Query("page") page: kotlin.Int? = null, @Query("limit") limit: kotlin.Int? = null, @Query("search") search: kotlin.String? = null, @Query("is_active") isActive: kotlin.String? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/promotion/config")
    suspend fun v2AdminPromotionConfigGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

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
    @PUT("v2/admin/promotion/config/{id}")
    suspend fun v2AdminPromotionConfigIdPut(@Path("id") id: kotlin.Int, @Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @POST("v2/admin/promotion/config")
    suspend fun v2AdminPromotionConfigPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @POST("v2/admin/promotion/execute")
    suspend fun v2AdminPromotionExecutePost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @POST("v2/admin/promotion/simulate")
    suspend fun v2AdminPromotionSimulatePost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param annualRolloverRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/admin/student-control/annual-rollover")
    suspend fun v2AdminStudentControlAnnualRolloverPost(@Body annualRolloverRequest: AnnualRolloverRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/admin/student-control/bulk-fields")
    suspend fun v2AdminStudentControlBulkFieldsPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/admin/student-control/bulk-progression")
    suspend fun v2AdminStudentControlBulkProgressionPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/student-control/overview")
    suspend fun v2AdminStudentControlOverviewGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param requestBody  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/admin/student-control/sequential-progression")
    suspend fun v2AdminStudentControlSequentialProgressionPost(@Body requestBody: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [kotlin.collections.Map<kotlin.String, kotlin.Any>]
     */
    @GET("v2/admin/student-control/transitions")
    suspend fun v2AdminStudentControlTransitionsGet(): Response<kotlin.collections.Map<kotlin.String, kotlin.Any>>

}
