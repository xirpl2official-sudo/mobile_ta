package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName(value = "status", alternate = ["success"])
    val status: Boolean,
    val message: String,
    val data: T? = null
)