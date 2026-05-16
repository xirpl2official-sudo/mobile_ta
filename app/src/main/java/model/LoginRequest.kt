package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val identifier: String? = null,
    val username: String? = null,
    val password: String
)

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)