package com.febrivio.sumbercomplang.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.FormKolamActivity
import com.febrivio.sumbercomplang.databinding.ItemPoolBinding
import com.febrivio.sumbercomplang.model.Kolam
import com.squareup.picasso.Picasso

class KolamAdapter(private val kolamList: List<Kolam>) : RecyclerView.Adapter<KolamAdapter.KolamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KolamViewHolder {
        val binding = ItemPoolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KolamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KolamViewHolder, position: Int) {
        val kolam = kolamList[position]
        holder.binding.tvPoolName.text = kolam.nama
        holder.binding.tvPoolDeskripsi.text = kolam.deskripsi
        Picasso.get().load(kolam.url_foto).into(holder.binding.ivPoolImage)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, FormKolamActivity::class.java)
            intent.putExtra("kolam", kolam)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return kolamList.size
    }

    inner class KolamViewHolder(val binding: ItemPoolBinding) : RecyclerView.ViewHolder(binding.root)
}

