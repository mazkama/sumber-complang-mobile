package com.febrivio.sumbercomplang.model

data class RegisterRequest(
    val username: String,
    val name: String,
    val no_hp: String,
    val password: String
)
data class RegisterResponse(
    val code: Int,
    val message: String,
    val errors: Map<String, List<String>>? = null,
)