package com.febrivio.sumbercomplang.model

data class CountThisMonthResponse(
    val bulan: String,
    val total_parkir: Int,
    val total_kolam_anak: Int,
    val total_kolam_dewasa: Int
)