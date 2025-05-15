package com.febrivio.sumbercomplang.model

import com.google.gson.annotations.SerializedName

data class TransaksiTiketRequest(
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("tiket_details") val tiketDetails: List<TiketDetail>
)

data class TiketDetail(
    @SerializedName("id_tiket") val idTiket: Int,
    @SerializedName("jumlah") val jumlah: Int
)

data class TransaksiTiketResponse(
    val success: Boolean,
    val message: String,
    val data: TransaksiData
)

data class TransaksiData(
    val order_id: String,
    val status: String,
    val gross_amount: Int,
    val payment_type: String,
    val redirect_url: String
)
