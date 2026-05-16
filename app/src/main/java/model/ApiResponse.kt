package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName(value = "status", alternate = ["success"])
    val status: Any? = null, // Can be Boolean or String ("error")
    val message: String? = null,
    val data: T? = null,
    val errors: List<ApiError>? = null
) {
    val isSuccessful: Boolean
        get() = status == true || status == "success"
}

data class ApiError(
    val field: String? = null,
    val message: String? = null
)