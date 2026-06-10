package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val identifier: String? = null,
    val password: String
)

data class RefreshResponse(
    val message: String? = null,
    val token: String? = null,
    @SerializedName("refresh_token")
    val refresh_token: String? = null
)

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)