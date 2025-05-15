package com.febrivio.sumbercomplang.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)
data class RegisterResponse(
    val success: Boolean,
    val message: String
)