package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest(
    @SerializedName("nis")
    val nis: String,
    @SerializedName("otp")
    val otp: String,
    @SerializedName("new_password")
    val newPassword: String
)