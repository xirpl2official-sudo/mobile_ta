package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class DeviceInfoResponse(
    val message: String? = null,
    val data: DeviceInfo? = null
)

data class DeviceInfo(
    val hardware_id: String,
    val device_name: String?,
    val device_model: String?,
    val os_version: String?,
    val is_verified: Boolean,
    @SerializedName("verified_at")
    val verifiedAt: String?,
    @SerializedName("last_auth_at")
    val lastAuthAt: String?
)

data class HardwareAuthRequest(
    @SerializedName("hardware_id")
    val hardwareId: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("device_model")
    val deviceModel: String,
    @SerializedName("os_version")
    val osVersion: String
)

data class ChangeDeviceRequest(
    @SerializedName("nis")
    val nis: String,
    @SerializedName("hardware_id_baru")
    val hardwareIdBaru: String,
    @SerializedName("reason")
    val reason: String
)
