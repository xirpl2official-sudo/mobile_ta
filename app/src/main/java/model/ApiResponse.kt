package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName(value = "status", alternate = ["success"])
    val status: Boolean? = null,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccessful: Boolean
        get() = status == true
}