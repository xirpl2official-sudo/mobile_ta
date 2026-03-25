package com.xirpl2.SASMobile.model

/**
 * Login request body
 * identifier: Can be NIS (for students) or Username (for admin/staff)
 * password: User's password
 */
data class LoginRequest(
    val identifier: String,
    val password: String
)