package com.febrivio.sumbercomplang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.databinding.ItemValidasiTiketBinding
import com.febrivio.sumbercomplang.model.TiketDetailResponse
import java.text.NumberFormat
import java.util.Locale

class ValidasiTiketAdapter(
    private val listTiket: List<TiketDetailResponse>
) : RecyclerView.Adapter<ValidasiTiketAdapter.ValidasiViewHolder>() {

    inner class ValidasiViewHolder(val binding: ItemValidasiTiketBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValidasiViewHolder {
        val binding = ItemValidasiTiketBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ValidasiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ValidasiViewHolder, position: Int) {
        val tiket = listTiket[position]

        holder.binding.tvNamaTiket.text = tiket.namaTiket
        holder.binding.tvJenisTiket.text = tiket.jenisTiket.capitalize(Locale.getDefault())
        holder.binding.tvHarga.text = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(tiket.harga.toDouble() ?: 0.0)

        // Plat nomor untuk parkir
        if (tiket.jenisTiket.lowercase() == "parkir" && !tiket.noKendaraan.isNullOrEmpty() && tiket.noKendaraan != "-") {
            holder.binding.layoutPlatNomor.visibility = View.VISIBLE
            holder.binding.tvPlatNomor.text = tiket.noKendaraan
        } else {
            holder.binding.layoutPlatNomor.visibility = View.GONE
        }

        // Indikasi tervalidasi (optional)
        if (tiket.waktu_validasi != null) {
            holder.binding.tvNamaTiket.alpha = 0.5f
            holder.binding.tvJenisTiket.alpha = 0.5f
            holder.binding.tvHarga.alpha = 0.5f
        } else {
            holder.binding.tvNamaTiket.alpha = 1.0f
            holder.binding.tvJenisTiket.alpha = 1.0f
            holder.binding.tvHarga.alpha = 1.0f
        }
    }

    override fun getItemCount() = listTiket.size
}