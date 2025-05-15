package com.febrivio.sumbercomplang.model

import java.io.Serializable

data class KolamResponse(
    val success: Boolean,
    val message: String,
    val data: Kolam?
)

data class KolamListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Kolam>
)


data class Kolam(
    val id_kolam: Int,
    val nama: String,
    val deskripsi: String?,
    val kedalaman: Double?,
    val luas: Double?,
    val url_foto: String?
) : Serializable
