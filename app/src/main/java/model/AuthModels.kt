package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)

data class VerifyAccountRequest(
    @SerializedName("new_password")
    val newPassword: String? = null,
    @SerializedName("confirm_password")
    val confirmPassword: String? = null
)

data class DeviceChangeRequestBody(
    @SerializedName("new_hardware_id")
    val newHardwareId: String,
    @SerializedName("alasan")
    val alasan: String
)
