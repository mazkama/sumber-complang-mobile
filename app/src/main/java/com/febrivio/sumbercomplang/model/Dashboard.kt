package com.febrivio.sumbercomplang.model

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    val success: Boolean,
    val message: String,
    val data: DashboardData
)

data class DashboardData(
    val jenis_tiket: String,
    val reserved_today: Int,
    val reserved_this_month: Int,
    val tickets_sold: String,
    val data: List<RiwayatTransaksiItem>
)

data class RecentTransaction(
    val id_transaksi_tiket: Int,
    val order_id: String,
    val id_user: Int,
    val total_harga: String,
    val metode_pembayaran: String,
    val id_divalidasi_oleh: Int?,
    val waktu_validasi: String?,
    val status: String,
    val redirect_url: String?,
    val created_at: String,
    val updated_at: String,
    val jenis_transaksi: String
)