package com.xirpl2.SASMobile.network.generated.api

import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.google.gson.annotations.SerializedName

import com.xirpl2.SASMobile.network.generated.model.AuthResponse
import com.xirpl2.SASMobile.network.generated.model.ChangeEmailRequest
import com.xirpl2.SASMobile.network.generated.model.ChangePasswordRequest
import com.xirpl2.SASMobile.network.generated.model.LoginRequest
import com.xirpl2.SASMobile.network.generated.model.MessageResponse
import com.xirpl2.SASMobile.network.generated.model.OtpVerificationRequest
import com.xirpl2.SASMobile.network.generated.model.PasswordResetRequest
import com.xirpl2.SASMobile.network.generated.model.RefreshRequest
import com.xirpl2.SASMobile.network.generated.model.RegisterRequest
import com.xirpl2.SASMobile.network.generated.model.RegisterResponse
import com.xirpl2.SASMobile.network.generated.model.ResetPasswordRequest
import com.xirpl2.SASMobile.network.generated.model.VerifyAccountRequest
import com.xirpl2.SASMobile.network.generated.model.VerifyEmailOTPRequest

interface AuthApi {
    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param changePasswordRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/change-password")
    suspend fun v2AuthChangePasswordPost(@Body changePasswordRequest: ChangePasswordRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param changeEmailRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/email-change-requests")
    suspend fun v2AuthEmailChangeRequestsPost(@Body changeEmailRequest: ChangeEmailRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param verifyEmailOTPRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/email-change-requests/verify")
    suspend fun v2AuthEmailChangeRequestsVerifyPost(@Body verifyEmailOTPRequest: VerifyEmailOTPRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param passwordResetRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/forgot-password")
    suspend fun v2AuthForgotPasswordPost(@Body passwordResetRequest: PasswordResetRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [AuthResponse]
     */
    @GET("v2/auth/profile")
    suspend fun v2AuthProfileGet(): Response<AuthResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param registerRequest  (optional)
     * @return [RegisterResponse]
     */
    @POST("v2/auth/registrations")
    suspend fun v2AuthRegistrationsPost(@Body registerRequest: RegisterRequest? = null): Response<RegisterResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param resetPasswordRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/reset-password")
    suspend fun v2AuthResetPasswordPost(@Body resetPasswordRequest: ResetPasswordRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @return [MessageResponse]
     */
    @DELETE("v2/auth/sessions/current")
    suspend fun v2AuthSessionsCurrentDelete(): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param loginRequest  (optional)
     * @return [AuthResponse]
     */
    @POST("v2/auth/sessions")
    suspend fun v2AuthSessionsPost(@Body loginRequest: LoginRequest? = null): Response<AuthResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param refreshRequest  (optional)
     * @return [AuthResponse]
     */
    @POST("v2/auth/tokens/refresh")
    suspend fun v2AuthTokensRefreshPost(@Body refreshRequest: RefreshRequest? = null): Response<AuthResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param verifyAccountRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/verify-account")
    suspend fun v2AuthVerifyAccountPost(@Body verifyAccountRequest: VerifyAccountRequest? = null): Response<MessageResponse>

    /**
     * 
     * 
     * Responses:
     *  - 200: OK
     *
     * @param otpVerificationRequest  (optional)
     * @return [MessageResponse]
     */
    @POST("v2/auth/verify-otp")
    suspend fun v2AuthVerifyOtpPost(@Body otpVerificationRequest: OtpVerificationRequest? = null): Response<MessageResponse>

}
