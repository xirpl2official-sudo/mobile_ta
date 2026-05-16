package com.xirpl2.SASMobile.model

data class Notifikasi(
    val id: Int = 0,
    val title: String,
    val message: String,
    val time: String,
    val type: String = "info",
    val isRead: Boolean = false
)