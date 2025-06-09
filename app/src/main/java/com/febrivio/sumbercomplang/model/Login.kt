package com.febrivio.sumbercomplang.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val code: Int,
    val success: Boolean,
    val message: String,
    val token: String,
    val user: User
)

data class User(
    val id_user: Int,
    val username: String,
    val name: String,
    val no_hp: String,
    val role: String,
    val created_at: String,
    val updated_at: String
)
