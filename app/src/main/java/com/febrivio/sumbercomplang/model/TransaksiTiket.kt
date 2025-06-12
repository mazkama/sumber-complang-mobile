package com.febrivio.sumbercomplang.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TransaksiTiketRequest(
    @SerializedName("metode_pembayaran") val metodePembayaran: String,
    @SerializedName("tiket_details") val tiketDetails: List<TiketDetail>
)

data class TiketDetail(
    @SerializedName("id_tiket") val idTiket: Int,
    @SerializedName("jumlah") val jumlah: Int,
    @SerializedName("no_kendaraan") val noKendaraan: String
)

data class TransaksiTiketResponse(
    val success: Boolean,
    val message: String,
    val data: TransaksiData
) : Serializable

data class TransaksiData(
    @SerializedName("order_id") val orderId: String,
    val status: String,
    @SerializedName("tanggal") val date: String,
    @SerializedName("gross_amount") val grossAmount: Int,
    @SerializedName("payment_type") val paymentType: String,
    @SerializedName("user") val customer: Customer,
    @SerializedName("detail_transaksi") val tiketDetails: List<TiketDetailResponse>,
    @SerializedName("redirect_url") val redirectUrl: String? = null
) : Serializable

data class Customer(
    @SerializedName("id_user") val idUser: Int,
    val name: String,
    val email: String
) : Serializable

data class TiketDetailResponse(
    @SerializedName("id_tiket") val idTiket: Int,
    @SerializedName("nama_tiket") val namaTiket: String,
    @SerializedName("harga_satuan") val harga: String,
    val jumlah: Int,
    val subtotal: Int
) : Serializable

data class TiketValidationResponse(
    val status: Boolean,
    val message: String
) : Serializable
