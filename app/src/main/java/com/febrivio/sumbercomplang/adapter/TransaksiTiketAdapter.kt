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
    private val paymentMethod: String, // Changed: direct payment method parameter
    private val onQuantityChange: (List<Tiket>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Modified TicketInfo class to store a list of plate numbers
    data class TicketInfo(
        var quantity: Int = 0,
        val plateNumbers: MutableList<String> = mutableListOf()
    )

    private val selectedTickets = mutableMapOf<Int, TicketInfo>()
    private var selectedPaymentMethod: String = paymentMethod // Initialize with provided method

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
                val jenisLower = tiket.jenis.lowercase()
                val kategoriLower = tiket.kategori.lowercase()

                // Untuk jenis kolam, boleh lebih dari satu jenis/jumlah
                if (jenisLower == "kolam") {
                    updateQuantityInBackground(idTiket, 1, position)
                    return@setOnClickListener
                }

                // Untuk jenis parkir
                if (jenisLower == "parkir") {
                    val alreadySelectedId = selectedTickets.filter { it.value.quantity > 0 }.keys
                    val isCurrentSelected = selectedTickets[idTiket]?.quantity ?: 0 > 0

                    // Tidak boleh lebih dari satu jenis/jumlah
                    if (alreadySelectedId.isNotEmpty() && !isCurrentSelected) {
                        Toast.makeText(
                            holder.itemView.context,
                            "Hanya boleh memilih satu tiket parkir saja.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    if (selectedTickets[idTiket]?.quantity ?: 0 >= 1) {
                        Toast.makeText(
                            holder.itemView.context,
                            "Jumlah tiket parkir hanya boleh satu.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Jika kategori sepeda, tidak perlu bottom sheet nopol
                    if (kategoriLower == "sepeda") {
                        updateQuantityInBackground(idTiket, 1, position)
                    } else {
                        showBottomSheetNopol(holder, tiket)
                    }
                    return@setOnClickListener
                }

                // Untuk kategori lain, hanya boleh satu tiket dan tidak boleh beda tiket
                val alreadySelectedId = selectedTickets.filter { it.value.quantity > 0 }.keys
                val isCurrentSelected = selectedTickets[idTiket]?.quantity ?: 0 > 0

                if (alreadySelectedId.isNotEmpty() && !isCurrentSelected) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Hanya boleh memilih satu tiket saja untuk kategori ini.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (selectedTickets[idTiket]?.quantity ?: 0 >= 1) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Jumlah tiket kategori ini hanya boleh satu.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                updateQuantityInBackground(idTiket, 1, position)
            }

            holder.binding.btnKurang.setOnClickListener {
                // For vehicle tickets with plate numbers, show dialog to select which to remove
//                if ((tiket.kategori.lowercase() == "mobil" || tiket.kategori.lowercase() == "motor")
//                    && ticketInfo.plateNumbers.isNotEmpty()) {
//                    showRemovePlateDialog(holder, tiket, position)
//                } else {
                    // For non-vehicle tickets, just update quantity directly
                    updateQuantityInBackground(idTiket, -1, position)
//                }
            }
        } else if (holder is TotalBayarViewHolder) {
            // Calculate total
            val total = getSelectedTiket().sumOf { it.harga * it.jumlah }
            val formattedTotal = NumberFormat.getNumberInstance(Locale("in", "ID")).format(total)
            holder.binding.tvTotalNominal.text = "Rp $formattedTotal"

            // SET TEKS BUTTON BERDASARKAN STATE (no click listener)
            holder.binding.btnMetodePembayaran.text = selectedPaymentMethod
            
            // Remove the click listener for payment method selection
            // The button will just display the payment method
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
                val kategoriLower = tiket.kategori.lowercase()
                val jenisLower = tiket.jenis.lowercase()

                // Untuk semua tiket parkir kecuali sepeda, kirim plat nomor jika ada
                if (jenisLower == "parkir" && kategoriLower != "sepeda" && ticketInfo.plateNumbers.isNotEmpty()) {
                    ticketInfo.plateNumbers.forEach { plateNumber ->
                        result.add(
                            tiket.copy(
                                jumlah = 1,
                                no_kendaraan = plateNumber
                            )
                        )
                    }
                } else {
                    // Untuk tiket lain, tetap kirim plat nomor jika ada
                    result.add(
                        tiket.copy(
                            jumlah = ticketInfo.quantity,
                            no_kendaraan = if (ticketInfo.plateNumbers.isNotEmpty()) ticketInfo.plateNumbers.first() else ""
                        )
                    )
                }
            }
        }

        return result
    }    // Updated method to store license plate number
    private fun showBottomSheetNopol(holder: TiketViewHolder, tiket: Tiket) {
        val context = holder.itemView.context
        val bottomSheetDialog = BottomSheetDialog(context)
        val binding = BottomSheetNopolBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        
        // Auto focus on first field and show keyboard
        binding.etPlatHuruf1.requestFocus()
        bottomSheetDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
          // Auto navigation between fields
        binding.etPlatHuruf1.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 2) {
                    binding.etPlatAngka.requestFocus()
                }
            }
        })
        
        binding.etPlatAngka.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 4) {
                    binding.etPlatHuruf2.requestFocus()
                } else if (s?.isEmpty() == true) {
                    binding.etPlatHuruf1.requestFocus()
                    binding.etPlatHuruf1.setSelection(binding.etPlatHuruf1.text?.length ?: 0)
                }
            }
        })
        
        binding.etPlatHuruf2.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isEmpty() == true) {
                    binding.etPlatAngka.requestFocus()
                    binding.etPlatAngka.setSelection(binding.etPlatAngka.text?.length ?: 0)
                }
            }
        })
        
        binding.btnSimpan.setOnClickListener {
            val huruf1 = binding.etPlatHuruf1.text.toString().uppercase()
            val angka = binding.etPlatAngka.text.toString()
            val huruf2 = binding.etPlatHuruf2.text.toString().uppercase()
            
            if (huruf1.isNotEmpty() && angka.isNotEmpty() && huruf2.isNotEmpty()) {
                val platNomor = "$huruf1 $angka $huruf2"                // Check for duplicate license plate
                val allPlateNumbers = ArrayList<String>()
                selectedTickets.values.forEach { ticketInfo ->
                    allPlateNumbers.addAll(ticketInfo.plateNumbers)
                }
                
                if (allPlateNumbers.contains(platNomor)) {
                    Toast.makeText(context, "Nomor plat $platNomor sudah digunakan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
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
