package com.febrivio.sumbercomplang.model

// Data classes for request and response
data class ProfileUpdateRequest(
    val name: String,
    val email: String
)

data class ProfileUpdateResponse(
    val status: Boolean,
    val code: Int,
    val message: String,
    val error: String? = null
)