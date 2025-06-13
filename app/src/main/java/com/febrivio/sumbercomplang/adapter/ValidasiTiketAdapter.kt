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
    private val listTiket: List<TiketDetailResponse>,
    private val onItemChecked: (TiketDetailResponse, Boolean) -> Unit
) : RecyclerView.Adapter<ValidasiTiketAdapter.ValidasiViewHolder>() {

    // Map to track checked state of items - use a composite key of idDtTransaksi and index
    private val checkedItems = mutableMapOf<String, Boolean>()

    // Expanded list of tickets with individual entries for each quantity
    private val expandedTickets = mutableListOf<ExpandedTicket>()

    // Data class for expanded tickets
    data class ExpandedTicket(
        val originalTicket: TiketDetailResponse,
        val index: Int  // Index within the quantity group
    )

    init {
        // Expand tickets based on quantity
        expandTickets()
    }

    private fun expandTickets() {
        expandedTickets.clear()
        listTiket.forEach { ticket ->
            // Create individual entries for each ticket based on quantity
            for (i in 0 until ticket.jumlah) {
                expandedTickets.add(ExpandedTicket(ticket, i))
            }
        }
    }

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
        val expandedTicket = expandedTickets[position]
        val tiket = expandedTicket.originalTicket
        val index = expandedTicket.index

        // Create a unique identifier for this specific ticket instance
        val ticketKey = "${tiket.idDtTransaksi}-$index"

        // Set up basic ticket info
        holder.binding.tvNamaTiket.text = tiket.namaTiket
        holder.binding.tvJenisTiket.text = tiket.jenisTiket.capitalize(Locale.getDefault())
        holder.binding.tvHarga.text = formatCurrency(tiket.harga.toString())

        // Check if ticket is already validated
        val isAlreadyValidated = tiket.waktu_validasi != null
        
        // Set checkbox state and enabled status
        holder.binding.checkboxValidasi.isChecked = checkedItems[ticketKey] ?: false
        holder.binding.checkboxValidasi.isEnabled = !isAlreadyValidated
        
        // Visual indication for already validated tickets
        if (isAlreadyValidated) {
            // Gray out the text and add validation indicator
            holder.binding.tvNamaTiket.alpha = 0.5f
            holder.binding.tvNamaTiket.text = "${tiket.namaTiket} (Tervalidasi)"
            holder.binding.tvJenisTiket.alpha = 0.5f
            holder.binding.tvHarga.alpha = 0.5f
            holder.binding.checkboxValidasi.alpha = 0.5f
        } else {
            // Reset alpha for non-validated tickets
            holder.binding.tvNamaTiket.alpha = 1.0f
            holder.binding.tvJenisTiket.alpha = 1.0f
            holder.binding.tvHarga.alpha = 1.0f
            holder.binding.checkboxValidasi.alpha = 1.0f
        }

        // Conditionally show license plate for parking tickets
        if (tiket.jenisTiket.lowercase() == "parkir" && !tiket.noKendaraan.isNullOrEmpty() && tiket.noKendaraan != "-") {
            holder.binding.layoutPlatNomor.visibility = View.VISIBLE
            holder.binding.tvPlatNomor.text = tiket.noKendaraan
            
            // Apply the same alpha to license plate section
            if (isAlreadyValidated) {
                holder.binding.layoutPlatNomor.alpha = 0.5f
            } else {
                holder.binding.layoutPlatNomor.alpha = 1.0f
            }
        } else {
            holder.binding.layoutPlatNomor.visibility = View.GONE
        }

        // Handle checkbox changes - only for non-validated tickets
        holder.binding.checkboxValidasi.setOnCheckedChangeListener { _, isChecked ->
            if (!isAlreadyValidated) {
                checkedItems[ticketKey] = isChecked
                onItemChecked(tiket, isChecked)
            }
        }

        // Make the whole item clickable to toggle checkbox - only for non-validated tickets
        holder.binding.root.setOnClickListener {
            if (!isAlreadyValidated) {
                val newState = !(checkedItems[ticketKey] ?: false)
                holder.binding.checkboxValidasi.isChecked = newState
                checkedItems[ticketKey] = newState
                onItemChecked(tiket, newState)
            } else {
                // Inform user that this ticket is already validated
                val validationTime = tiket.waktu_validasi ?: "unknown time"
                val message = "Tiket sudah tervalidasi pada $validationTime"
                android.widget.Toast.makeText(holder.itemView.context, message, 
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = expandedTickets.size

    private fun formatCurrency(amount: String): String {
        try {
            val value = amount.toDouble()
            return NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                .format(value)
                .replace("Rp", "Rp ")
                .trim()
        } catch (e: Exception) {
            return "Rp $amount"
        }
    }

    // Toggle all items selection
    fun toggleSelectAll(select: Boolean) {
        expandedTickets.forEach { expandedTicket ->
            val ticket = expandedTicket.originalTicket
            val ticketKey = "${ticket.idDtTransaksi}-${expandedTicket.  index}"
            
            // Only toggle selection for non-validated tickets
            if (ticket.waktu_validasi == null) {
                checkedItems[ticketKey] = select
            }
        }
        notifyDataSetChanged()
    }

    // Function to get all checked items
    fun getCheckedItems(): List<TiketDetailResponse> {
        val result = mutableListOf<TiketDetailResponse>()
        checkedItems.forEach { (key, isChecked) ->
            if (isChecked) {
                val parts = key.split("-")
                val idDtTransaksi = parts[0].toInt()
                val ticket = listTiket.find { it.idDtTransaksi == idDtTransaksi }
                if (ticket != null && ticket.waktu_validasi == null) {
                    result.add(ticket)
                }
            }
        }
        return result.distinctBy { it.idDtTransaksi }
    }
}