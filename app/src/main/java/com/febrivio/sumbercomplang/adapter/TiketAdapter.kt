package com.febrivio.sumbercomplang.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.databinding.ItemTiketBinding
import com.febrivio.sumbercomplang.model.Tiket
import com.febrivio.sumbercomplang.utils.CurrencyHelper.formatCurrency

class TiketAdapter(private val tiketKolamList: List<Tiket>, private val onItemClick: (Tiket) -> Unit,) : RecyclerView.Adapter<TiketAdapter.TiketKolamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiketKolamViewHolder {
        val binding = ItemTiketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TiketKolamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TiketKolamViewHolder, position: Int) {
        val kolam = tiketKolamList[position]
        holder.binding.tvKategoriTiket.text = kolam.kategori
        holder.binding.tvNamaTiket.text = kolam.nama_tiket
        holder.binding.tvDesTiket.text = kolam.deskripsi
        holder.binding.tvHargaTiket.text = "Rp ${formatCurrency(kolam.harga)}"

        holder.itemView.setOnClickListener {
            onItemClick(kolam)
        }
//
//        holder.itemView.setOnClickListener {
//            val context = holder.itemView.context
//            val intent = Intent(context, FormTiketActivity::class.java)
//            context.startActivity(intent)
//        }
    }


    override fun getItemCount(): Int {
        return tiketKolamList.size
    }

    inner class TiketKolamViewHolder(val binding: ItemTiketBinding) : RecyclerView.ViewHolder(binding.root)
}
