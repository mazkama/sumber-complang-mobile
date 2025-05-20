package com.febrivio.sumbercomplang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.R
import com.febrivio.sumbercomplang.model.TiketDetailResponse
import java.text.NumberFormat
import java.util.*

class DetailTransaksiTiketAdapter(
    private val tiketList: List<TiketDetailResponse>
) : RecyclerView.Adapter<DetailTransaksiTiketAdapter.ViewHolder>() {

    private val localeID = Locale("in", "ID")
    private val formatRupiah = NumberFormat.getCurrencyInstance(localeID).apply {
        maximumFractionDigits = 0
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvItemTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvItemPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = tiketList[position]
        holder.tvTitle.text = "${position + 1}. ${item.namaTiket} x ${item.jumlah}"
        holder.tvPrice.text = "Rp ${formatRupiah.format(item.subtotal.toLong()).replace("Rp", "").trim()}"
    }

    override fun getItemCount(): Int = tiketList.size
}
