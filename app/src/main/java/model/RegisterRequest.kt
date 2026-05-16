package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val nis: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String,
    val nis: String? = null,
    val email: String? = null,
    @SerializedName("is_google_acct")
    val isGoogleAcct: Boolean? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)