package com.febrivio.sumbercomplang.model

// Data classes for request and response
data class ProfileUpdateRequest(
    val name: String,
    val username: String,
    val no_hp: String
)

data class ProfileUpdateResponse(
    val status: Boolean,
    val code: Int,
    val message: String,
    val errors: Map<String, List<String>>? = null,
)