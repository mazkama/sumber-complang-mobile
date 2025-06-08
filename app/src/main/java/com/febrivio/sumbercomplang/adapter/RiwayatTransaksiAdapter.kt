package com.febrivio.sumbercomplang.adapter

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.databinding.ItemRiwayatTransaksiBinding
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.utils.CurrencyHelper.formatCurrency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RiwayatTransaksiAdapter(
    private val list: MutableList<RiwayatTransaksiItem>, // Changed from RiwayatTransaksiData
    private val onItemClick: ((RiwayatTransaksiItem) -> Unit)? = null
) : RecyclerView.Adapter<RiwayatTransaksiAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: RiwayatTransaksiItem) {
            with(binding) {
                tvStatusTransaksi.text = item.status
                tvIdTransaksi.text = item.order_id
                Log.d("TanggalTransaksi", "created_at: ${item.created_at}")
                tvTanggalTransaksi.text = formatTanggal(item.created_at)
                tvJenisTiket.text = "Tiket ${item.jenis_transaksi}"
                tvHargaTiket.text = "Rp ${formatCurrency(item.total_harga)}"

                // Optionally: Styling status (e.g., background)
                val bgRes = when (item.status.lowercase()) {
                    "menunggu" -> com.febrivio.sumbercomplang.R.drawable.bg_status_pending
                    "dibayar" -> com.febrivio.sumbercomplang.R.drawable.bg_status_success
                    "divalidasi" -> com.febrivio.sumbercomplang.R.drawable.bg_status_success
                    "dibatalkan" -> com.febrivio.sumbercomplang.R.drawable.bg_status_failed
                    "gagal" -> com.febrivio.sumbercomplang.R.drawable.bg_status_failed
                    else -> com.febrivio.sumbercomplang.R.drawable.bg_status_pending
                }
                tvStatusTransaksi.setBackgroundResource(bgRes)

                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
            }
        }
    }

    fun formatTanggal(createdAt: String?, locale: Locale = Locale("id")): String {
        if (createdAt.isNullOrBlank()) return "-"

        return try {
            // Parsing dengan Instant (ISO-8601)
            val instant = Instant.parse(createdAt)

            // Konversi ke zona lokal perangkat
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())

            // Format output dengan bahasa Indonesia
            val outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", locale)

            zonedDateTime.format(outputFormatter)
        } catch (e: Exception) {
            e.printStackTrace()
            "-"
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatTransaksiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position]) // This now correctly matches the type
    }

    override fun getItemCount(): Int = list.size
}
