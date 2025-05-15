package com.febrivio.sumbercomplang.adapter

import com.febrivio.sumbercomplang.R
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.databinding.ItemCardTotalBayarBinding
import com.febrivio.sumbercomplang.databinding.ItemTiketTransaksiBinding
import com.febrivio.sumbercomplang.model.Tiket
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

class TransaksiTiketAdapter(
    private val listTiket: List<Tiket>,
    private val onPaymentMethodChanged: (String) -> Unit,
    private val onQuantityChange: (List<Tiket>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedTickets = mutableMapOf<Int, Int>()
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
            val jumlah = selectedTickets[idTiket] ?: 0
            val formattedHarga = NumberFormat.getNumberInstance(Locale("in", "ID")).format(tiket.harga)

            holder.binding.tvKategoriTiket.text = tiket.kategori
            holder.binding.tvNamaTiket.text = tiket.nama_tiket
            holder.binding.tvDesTiket.text = tiket.deskripsi
            holder.binding.tvHargaTiket.text = "Rp $formattedHarga"
            holder.binding.tvJumlah.text = jumlah.toString()

            holder.binding.btnTambah.setOnClickListener {
                updateQuantityInBackground(idTiket, 1, position)
            }

            holder.binding.btnKurang.setOnClickListener {
                updateQuantityInBackground(idTiket, -1, position)
            }
        } else if (holder is TotalBayarViewHolder) {
            // Hitung total
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
                        // UPDATE STATE
                        selectedPaymentMethod = options[which]
                        // CALLBACK ke Activity
                        onPaymentMethodChanged(selectedPaymentMethod)
                        // REFRESH item total saja
                        notifyItemChanged(itemCount - 1)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

    }

    private fun getSelectedTiket(): List<Tiket> {
        return listTiket.map { tiket ->
            tiket.copy(jumlah = selectedTickets[tiket.id_tiket] ?: 0)
        }.filter { it.jumlah > 0 }
    }

    // Fungsi untuk memperbarui jumlah tiket di background
    private fun updateQuantityInBackground(idTiket: Int, delta: Int, position: Int) {
        GlobalScope.launch {
            val currentQty = selectedTickets[idTiket] ?: 0
            val newQty = currentQty + delta
            if (newQty >= 0) {
                selectedTickets[idTiket] = newQty
                // Panggil notifyItemChanged di UI thread
                withContext(Dispatchers.Main) {
                    notifyItemChanged(position)
                    notifyItemChanged(itemCount - 1) // Refresh total bayar
                    onQuantityChange(getSelectedTiket())
                }
            }
        }
    }
}
