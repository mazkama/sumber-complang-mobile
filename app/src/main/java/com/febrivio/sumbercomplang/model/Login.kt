package com.febrivio.sumbercomplang.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String,
    val user: User
)

data class User(
    val id_user: Int,
    val name: String,
    val email: String,
    val role: String,
    val created_at: String,
    val updated_at: String
)
