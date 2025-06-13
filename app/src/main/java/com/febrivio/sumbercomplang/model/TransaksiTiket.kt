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
    @SerializedName("user") val customer: Customer, // Updated field name
    @SerializedName("detail_transaksi") val tiketDetails: List<TiketDetailResponse>,
    @SerializedName("redirect_url") val redirectUrl: String? = null
) : Serializable

// Updated customer model per the new response format
data class Customer(
    val name: String,
    val username: String
) : Serializable

// Updated ticket detail response to include license plate and ticket type
data class TiketDetailResponse(
    @SerializedName("id_dt_transaksi") val idDtTransaksi: Int,
    @SerializedName("id_tiket") val idTiket: Int,
    @SerializedName("nama_tiket") val namaTiket: String,
    @SerializedName("jenis_tiket") val jenisTiket: String, // Added field
    @SerializedName("harga") val harga: Int,
    val jumlah: Int,
    val subtotal: Int,
    @SerializedName("no_kendaraan") val noKendaraan: String? = null, // Added field
    @SerializedName("waktu_validasi") val waktu_validasi: String? = null // Added field
) : Serializable

data class TiketValidationResponse(
    val status: Boolean,
    val message: String
) : Serializable

// Add this to your TransaksiTiket.kt file
data class TiketValidationRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("ticket_ids") val ticketIds: List<Int>
)