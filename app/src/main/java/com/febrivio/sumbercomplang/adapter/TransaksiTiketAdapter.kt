package com.febrivio.sumbercomplang.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.databinding.ItemCardTotalBayarBinding
import com.febrivio.sumbercomplang.databinding.ItemTiketTransaksiBinding
import com.febrivio.sumbercomplang.databinding.BottomSheetNopolBinding
import com.febrivio.sumbercomplang.model.Tiket
import java.text.NumberFormat
import java.util.Locale
import com.google.android.material.bottomsheet.BottomSheetDialog

class TransaksiTiketAdapter(
    private val listTiket: List<Tiket>,
    private val onPaymentMethodChanged: (String) -> Unit,
    private val onQuantityChange: (List<Tiket>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Modified TicketInfo class to store a list of plate numbers
    data class TicketInfo(
        var quantity: Int = 0,
        val plateNumbers: MutableList<String> = mutableListOf()
    )

    private val selectedTickets = mutableMapOf<Int, TicketInfo>()
    private var selectedPaymentMethod: String = "E-Wallet"

    companion object {
        private const val ITEM_TIKET = 0
        private const val ITEM_TOTAL = 1
    }

    inner class TiketViewHolder(val binding: ItemTiketTransaksiBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class TotalBayarViewHolder(val binding: ItemCardTotalBayarBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = listTiket.size + 1 // +1 untuk total bayar

    override fun getItemViewType(position: Int): Int {
        return if (position < listTiket.size) ITEM_TIKET else ITEM_TOTAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TIKET) {
            val binding = ItemTiketTransaksiBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            TiketViewHolder(binding)
        } else {
            val binding = ItemCardTotalBayarBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            TotalBayarViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TiketViewHolder && position < listTiket.size) {
            val tiket = listTiket[position]
            val idTiket = tiket.id_tiket
            val ticketInfo = selectedTickets[idTiket] ?: TicketInfo()
            val jumlah = ticketInfo.quantity
            val formattedHarga = NumberFormat.getNumberInstance(Locale("in", "ID")).format(tiket.harga)
            
            holder.binding.tvKategoriTiket.text = tiket.kategori
            holder.binding.tvNamaTiket.text = tiket.nama_tiket
            holder.binding.tvDesTiket.text = tiket.deskripsi
            holder.binding.tvHargaTiket.text = "Rp $formattedHarga"
            holder.binding.tvJumlah.text = jumlah.toString()
            
            // Display plate numbers if available
            if (ticketInfo.plateNumbers.isNotEmpty() && jumlah > 0) {
                val plateList = ticketInfo.plateNumbers.joinToString("\n")
                holder.binding.tvDesTiket.text = "${tiket.deskripsi}\nNo. Kendaraan:\n$plateList"
            }

            holder.binding.btnTambah.setOnClickListener {
                // Check if this is a vehicle ticket
                if (tiket.kategori.lowercase() == "mobil" || tiket.kategori.lowercase() == "motor") {
                    showBottomSheetNopol(holder, tiket)
                } else {
                    // For non-vehicle tickets, just update quantity directly
                    updateQuantityInBackground(idTiket, 1, position)
                }
            }

            holder.binding.btnKurang.setOnClickListener {
                // For vehicle tickets with plate numbers, show dialog to select which to remove
                if ((tiket.kategori.lowercase() == "mobil" || tiket.kategori.lowercase() == "motor") 
                    && ticketInfo.plateNumbers.isNotEmpty()) {
                    showRemovePlateDialog(holder, tiket, position)
                } else {
                    // For non-vehicle tickets, just update quantity directly
                    updateQuantityInBackground(idTiket, -1, position)
                }
            }
        } else if (holder is TotalBayarViewHolder) {
            // Calculate total
            val total = getSelectedTiket().sumOf { it.harga * it.jumlah }
            val formattedTotal = NumberFormat.getNumberInstance(Locale("in", "ID")).format(total)
            holder.binding.tvTotalNominal.text = "Rp $formattedTotal"

            // SET TEKS BUTTON BERDASARKAN STATE
            holder.binding.btnMetodePembayaran.text = selectedPaymentMethod

            holder.binding.btnMetodePembayaran.setOnClickListener { view ->
                val context = view.context
                val options = arrayOf("Tunai", "E-Wallet")
                AlertDialog.Builder(context)
                    .setTitle("Pilih Metode Pembayaran")
                    .setItems(options) { _, which ->
                        selectedPaymentMethod = options[which]
                        onPaymentMethodChanged(selectedPaymentMethod)
                        notifyItemChanged(itemCount - 1)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    // Show dialog to select which plate number to remove
    private fun showRemovePlateDialog(holder: TiketViewHolder, tiket: Tiket, position: Int) {
        val context = holder.itemView.context
        val ticketInfo = selectedTickets[tiket.id_tiket] ?: return
        
        if (ticketInfo.plateNumbers.isEmpty()) {
            updateQuantityInBackground(tiket.id_tiket, -1, position)
            return
        }
        
        val plateNumbers = ticketInfo.plateNumbers.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("Hapus Tiket Kendaraan")
            .setItems(plateNumbers) { _, which ->
                removePlateNumber(tiket.id_tiket, which, position)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun removePlateNumber(idTiket: Int, plateIndex: Int, position: Int) {
        val ticketInfo = selectedTickets[idTiket] ?: return
        
        if (plateIndex >= 0 && plateIndex < ticketInfo.plateNumbers.size) {
            ticketInfo.plateNumbers.removeAt(plateIndex)
            ticketInfo.quantity = ticketInfo.plateNumbers.size
            
            if (ticketInfo.quantity <= 0) {
                selectedTickets.remove(idTiket)
            } else {
                selectedTickets[idTiket] = ticketInfo
            }
            
            notifyItemChanged(position)
            notifyItemChanged(itemCount - 1) // Refresh total
            onQuantityChange(getSelectedTiket())
        }
    }

    private fun getSelectedTiket(): List<Tiket> {
        val result = mutableListOf<Tiket>()
        
        listTiket.forEach { tiket ->
            val ticketInfo = selectedTickets[tiket.id_tiket]
            if (ticketInfo != null && ticketInfo.quantity > 0) {
                // For vehicle tickets with plate numbers
                if ((tiket.kategori.lowercase() == "mobil" || tiket.kategori.lowercase() == "motor") 
                    && ticketInfo.plateNumbers.isNotEmpty()) {
                    
                    // Create individual ticket for each plate number
                    ticketInfo.plateNumbers.forEach { plateNumber ->
                        result.add(
                            tiket.copy(
                                jumlah = 1,  // Each vehicle is one ticket
                                no_kendaraan = plateNumber
                            )
                        )
                    }
                } else {
                    // For regular tickets (non-vehicles)
                    result.add(
                        tiket.copy(
                            jumlah = ticketInfo.quantity,
                            no_kendaraan = ""
                        )
                    )
                }
            }
        }
        
        return result
    }

    // Updated method to store license plate number
    private fun showBottomSheetNopol(holder: TiketViewHolder, tiket: Tiket) {
        val context = holder.itemView.context
        val bottomSheetDialog = BottomSheetDialog(context)
        val binding = BottomSheetNopolBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        
        binding.btnSimpan.setOnClickListener {
            val huruf1 = binding.etPlatHuruf1.text.toString().uppercase()
            val angka = binding.etPlatAngka.text.toString()
            val huruf2 = binding.etPlatHuruf2.text.toString().uppercase()
            
            if (huruf1.isNotEmpty() && angka.isNotEmpty() && huruf2.isNotEmpty()) {
                val platNomor = "$huruf1 $angka $huruf2"
                
                // Add the new plate number
                addPlateNumber(tiket.id_tiket, platNomor, holder.adapterPosition, context)
                
                Toast.makeText(context, "Plat nomor: $platNomor disimpan", Toast.LENGTH_SHORT).show()
                
                // Clear fields for next entry
                binding.etPlatHuruf1.text?.clear()
                binding.etPlatAngka.text?.clear()
                binding.etPlatHuruf2.text?.clear()
                binding.etPlatHuruf1.requestFocus()
                
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(context, "Harap lengkapi semua field", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnBanyakKendaraan.setOnClickListener {
            Toast.makeText(context, "Fitur multiple kendaraan diklik", Toast.LENGTH_SHORT).show()
            // Could implement batch entry here in the future
        }
        
        bottomSheetDialog.show()
    }

    // New method to add a plate number
    private fun addPlateNumber(idTiket: Int, plateNumber: String, position: Int, context: Context) {
        val ticketInfo = selectedTickets[idTiket] ?: TicketInfo()
        
        // Check if plate number already exists
        if (!ticketInfo.plateNumbers.contains(plateNumber)) {
            ticketInfo.plateNumbers.add(plateNumber)
            ticketInfo.quantity = ticketInfo.plateNumbers.size
            
            selectedTickets[idTiket] = ticketInfo
            notifyItemChanged(position)
            notifyItemChanged(itemCount - 1) // Refresh total bayar
            onQuantityChange(getSelectedTiket())
        } else {
            // Use the context that was passed as a parameter
            Toast.makeText(
                context,
                "Nomor kendaraan sudah terdaftar", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Update the existing method to handle the new TicketInfo structure
    private fun updateQuantityInBackground(idTiket: Int, delta: Int, position: Int) {
        val ticketInfo = selectedTickets[idTiket] ?: TicketInfo()
        val newQuantity = ticketInfo.quantity + delta
        
        if (newQuantity >= 0) {
            ticketInfo.quantity = newQuantity
            
            if (newQuantity == 0) {
                selectedTickets.remove(idTiket)
            } else {
                selectedTickets[idTiket] = ticketInfo
            }
            
            notifyItemChanged(position)
            notifyItemChanged(itemCount - 1) // Refresh total bayar
            onQuantityChange(getSelectedTiket())
        }
    }
}
