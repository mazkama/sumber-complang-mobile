package com.febrivio.sumbercomplang.model


data class RiwayatTransaksiTiketResponse(
    val success: Boolean,
    val message: String,
    val data: RiwayatTransaksiData
)

data class RiwayatTransaksiData(
    val current_page: Int,
    val data: List<RiwayatTransaksiItem>,
    val last_page: Int,
    val next_page_url: String?,
    val prev_page_url: String?,
    val total: Int
)

data class RiwayatTransaksiItem(
    val id_transaksi_tiket: Int,
    val order_id: String,
    val id_user: Int,
    val total_harga: Int,
    val metode_pembayaran: String,
    val tgl_booking: String?,
    val id_divalidasi_oleh: Int?,
    val waktu_validasi: String?,
    val status: String,
    val redirect_url: String,
    val created_at: String,
    val updated_at: String,
    val jenis_transaksi: String,
)
