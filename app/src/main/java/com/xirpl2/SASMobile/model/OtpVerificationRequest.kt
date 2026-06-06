package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class OtpVerificationRequest(
    @SerializedName("nis")
    val nis: String,
    @SerializedName("otp")
    val otp: String
)