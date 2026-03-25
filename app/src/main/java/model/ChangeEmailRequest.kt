package com.xirpl2.SASMobile.model

import com.google.gson.annotations.SerializedName

data class ChangeEmailRequest(
    @SerializedName("new_email")
    val email: String
)
