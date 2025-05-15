package com.febrivio.sumbercomplang.model

import com.google.gson.annotations.SerializedName

data class TiketResponse(
    val success: Boolean,
    val message: String,
    val data: List<Tiket>
)

data class TiketRequest(
    val nama_tiket: String,
    val harga: Int,
    val kategori: String,
    val jenis: String,
    val deskripsi: String
)

data class Tiket(
    @SerializedName("id_tiket")
    val id_tiket: Int,

    @SerializedName("nama_tiket")
    val nama_tiket: String,

    @SerializedName("kategori")
    val kategori: String,

    @SerializedName("jenis")
    val jenis: String,

    @SerializedName("deskripsi")
    val deskripsi: String,

    @SerializedName("harga")
    val harga: Int,

    var jumlah: Int = 0  // jumlah ini digunakan untuk transaksi lokal
)

//data class Tiket(
//    val id_tiket: Int,
//    val nama_tiket: String,
//    val harga: Int,
//    val kategori: String,
//    val jenis: String,
//    val deskripsi: String,
//    var jumlah: Int = 0
//)
