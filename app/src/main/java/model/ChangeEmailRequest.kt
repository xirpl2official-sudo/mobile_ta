package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ChangeEmailRequest(
    @SerializedName("new_email")
    val newEmail: String
)

data class VerifyEmailOTPRequest(
    @SerializedName("new_email")
    val newEmail: String,
    @SerializedName("otp")
    val otp: String
)
