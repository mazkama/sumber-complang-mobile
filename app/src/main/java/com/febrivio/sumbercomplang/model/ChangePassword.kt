package com.febrivio.sumbercomplang.model

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val confirm_password: String
)

data class ChangePasswordResponse(
    val code: Int,
    val message: String,
    val errors: Map<String, List<String>>? = null
)
