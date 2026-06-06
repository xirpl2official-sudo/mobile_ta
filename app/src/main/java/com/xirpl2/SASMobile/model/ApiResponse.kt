package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName(value = "status", alternate = ["success"])
    val status: String? = null,
    val message: String? = null,
    val data: T? = null,
    val errors: List<ApiError>? = null
) {
    val isSuccessful: Boolean
        get() = status == "success" || status == "true" || status == "ok"
}

data class ApiError(
    val field: String? = null,
    val message: String? = null
)