package com.febrivio.sumbercomplang.model

data class RekapPembayaranResponse(
    val success: Boolean,
    val message: String,
    val total_transaksi: Int,
    val total_pembayaran: String,
    val data: RiwayatTransaksiData
)