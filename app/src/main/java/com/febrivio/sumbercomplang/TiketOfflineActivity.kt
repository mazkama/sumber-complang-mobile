package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.febrivio.sumbercomplang.adapter.TiketAdapter
import com.febrivio.sumbercomplang.databinding.ActivityTiketBinding
import com.febrivio.sumbercomplang.model.Tiket
import com.febrivio.sumbercomplang.model.TiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.adapter.TransaksiTiketAdapter
import com.febrivio.sumbercomplang.databinding.ActivityTiketofflineBinding
import com.febrivio.sumbercomplang.databinding.ActivityTransaksiTiketBinding
import com.febrivio.sumbercomplang.model.TiketDetail
import com.febrivio.sumbercomplang.model.TransaksiTiketRequest
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import com.febrivio.sumbercomplang.utils.CurrencyHelper.formatCurrency
import com.google.gson.Gson

class TiketOfflineActivity : AppCompatActivity(){
    private lateinit var b: ActivityTiketofflineBinding
    private lateinit var transaksiTiketAdapter: TransaksiTiketAdapter
    private var selectedTiketList: List<Tiket> = emptyList()
    private var selectedPaymentMethod: String = "Tunai" // Changed: Set default directly
    private var jenisTiket : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTiketofflineBinding.inflate(layoutInflater)
        setContentView(b.root)

        jenisTiket = intent.getStringExtra("jenis_tiket").toString()

        // Set payment method based on ticket type if needed
        selectedPaymentMethod = "Tunai"

        if (jenisTiket == "kolam") b.tvPembelianTiket.setText("Tiket Kolam Renang")
        else b.tvPembelianTiket.setText("Tiket Parkir")

        // Setup SwipeRefresh
        b.swipeRefreshLayout.setOnRefreshListener {
            getTiketData(jenisTiket)
        }

        // Setup RecyclerView
        b.rvTiketKolam.layoutManager = LinearLayoutManager(this)

        // Load data
        getTiketData(jenisTiket)

        b.btnBayar.setOnClickListener {
            showPaymentDialog()
        }

        b.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset values when returning from DetailTransaksiActivity
        resetTiketSelection()
    }

    private fun resetTiketSelection() {
        selectedTiketList = emptyList()
        // Refresh the adapter to reset all quantities to 0
        if (::transaksiTiketAdapter.isInitialized) {
            getTiketData(jenisTiket)
        }
    }

    private fun getTiketData(jenis : String) {
        b.swipeRefreshLayout.isRefreshing = true

        ApiClient.instance.getTiket(jenis).enqueue(object : Callback<TiketResponse> {
            override fun onResponse(call: Call<TiketResponse>, response: Response<TiketResponse>) {
                b.swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    val tiketList = response.body()?.data ?: emptyList()
                    if (tiketList.isNotEmpty()) {
                        transaksiTiketAdapter = TransaksiTiketAdapter(
                            listTiket = tiketList,
                            paymentMethod = selectedPaymentMethod, // Changed: Pass the preset method
                            onQuantityChange = { updatedList ->
                                selectedTiketList = updatedList

                                var total = 0
                                for (tiket in selectedTiketList) {
                                    val subtotal = tiket.harga * tiket.jumlah
                                    total += subtotal
                                }
                            }
                        )

                        b.rvTiketKolam.adapter = transaksiTiketAdapter
                    } else {
                        Toast.makeText(this@TiketOfflineActivity, "Data tiket kosong", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@TiketOfflineActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TiketResponse>, t: Throwable) {
                b.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@TiketOfflineActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun showPaymentDialog() {
        if (selectedTiketList.isEmpty()) {
            Toast.makeText(this, "Belum ada tiket yang dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        var total = 0
        for (tiket in selectedTiketList) {
            val subtotal = tiket.harga * tiket.jumlah
            total += subtotal
        }

        // Create custom dialog
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_payment)
        dialog.setCancelable(false)
        
        // Make dialog background transparent and full width
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Get views from custom layout
        val summaryContainer = dialog.findViewById<android.widget.LinearLayout>(R.id.ll_summary_container)
        val paymentMethodText = dialog.findViewById<android.widget.TextView>(R.id.tv_payment_method)
        val totalAmountText = dialog.findViewById<android.widget.TextView>(R.id.tv_total_amount)
        val editText = dialog.findViewById<android.widget.EditText>(R.id.et_payment_amount)
        val changeText = dialog.findViewById<android.widget.TextView>(R.id.tv_change)
        val changeSectionCard = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.cv_change_section)
        val btnCancel = dialog.findViewById<android.widget.TextView>(R.id.btn_cancel)
        val btnPay = dialog.findViewById<android.widget.TextView>(R.id.btn_pay)
        
        // Populate summary using item_detail_transaksi layout
        populateSummaryItems(summaryContainer)
        
        // Set payment method and total
        paymentMethodText.text = selectedPaymentMethod
        totalAmountText.text = "Rp ${formatCurrency(total)}"

        // Update change calculation when user types
        editText.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                
                // Remove all non-digit characters
                val cleanString = s.toString().replace("[^\\d]".toRegex(), "")
                
                // Format with thousand separators
                val formatted = formatWithThousandSeparator(cleanString)
                
                // Set the formatted text
                editText.setText(formatted)
                editText.setSelection(formatted.length)
                
                // Calculate change
                val inputAmount = cleanString.toIntOrNull() ?: 0
                val change = inputAmount - total
                if (change >= 0) {
                    changeText.text = "Rp ${formatCurrency(change)}"
                    changeText.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                    changeSectionCard.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E8"))
                } else {
                    changeText.text = "Rp ${formatCurrency(-change)}"
                    changeText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                    changeSectionCard.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                }
                
                isFormatting = false
            }
        })

        // Set button click listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnPay.setOnClickListener {
            // Get clean number without formatting
            val cleanInput = editText.text.toString().replace("[^\\d]".toRegex(), "")
            val inputAmount = cleanInput.toIntOrNull() ?: 0
            if (inputAmount >= total) {
                dialog.dismiss()
                kirimTransaksiKeServer()
            } else {
                Toast.makeText(this, "Jumlah uang tidak mencukupi!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        
        // Auto focus on payment input and show keyboard
        editText.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun populateSummaryItems(container: android.widget.LinearLayout) {
        container.removeAllViews()
        
        selectedTiketList.forEachIndexed { index, tiket ->
            val itemView = layoutInflater.inflate(R.layout.item_detail_transaksi, container, false)
            
            val tvItemTitle = itemView.findViewById<android.widget.TextView>(R.id.tvItemTitle)
            val tvItemPrice = itemView.findViewById<android.widget.TextView>(R.id.tvItemPrice)
            
            val subtotal = tiket.harga * tiket.jumlah
            
            tvItemTitle.text = "${index + 1}. ${tiket.nama_tiket} x ${tiket.jumlah}"
            tvItemPrice.text = "Rp ${formatCurrency(subtotal)}"
            
            container.addView(itemView)
        }
    }

    private fun kirimTransaksiKeServer() {
        val paymentMethodFormatted = when (selectedPaymentMethod) {
            "Tunai" -> "tunai"
            else -> selectedPaymentMethod  // Default jika tidak ada kecocokan
        }

        val tiketDetails = selectedTiketList.map {
            // Use the actual license plate number if available, otherwise use empty string
            val nopol = if (it.no_kendaraan != null && it.no_kendaraan.isNotEmpty()) {
                it.no_kendaraan
            } else {
                ""  // Default value if no license plate
            }

            TiketDetail(idTiket = it.id_tiket, jumlah = it.jumlah, nopol)
        }

        val request = TransaksiTiketRequest(
            metodePembayaran = paymentMethodFormatted,
            tiketDetails = tiketDetails
        )

        // Mendapatkan token dari SessionManager
        val token = SessionManager(this).getToken()
        val apiService = ApiServiceAuth(this, token)

        // Membuat permintaan ke server
        val call = apiService.createTransaksiTiket(request)

        call.enqueue(object : Callback<TransaksiTiketResponse> {
            override fun onResponse(
                call: Call<TransaksiTiketResponse>,
                response: Response<TransaksiTiketResponse>
            ) {
                Log.d("Request", Gson().toJson(request))
                Log.d("Response", Gson().toJson(response.body()))
                if (response.isSuccessful) {
                    val transaksiResponse = response.body()


                    if (transaksiResponse != null && transaksiResponse.success) {
                        val intent = Intent(this@TiketOfflineActivity,
                            DetailTransaksiActivity::class.java)
                        intent.putExtra("transaksi", transaksiResponse.data)
                        startActivity(intent)
                    } else {
                        // Jika `success` false atau response body tidak valid
                        Toast.makeText(this@TiketOfflineActivity, "Transaksi gagal: ${transaksiResponse?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Menangani response error (tidak berhasil)
                    val errorResponse = response.errorBody()?.string()
                    Toast.makeText(this@TiketOfflineActivity, "Error dari server: $errorResponse", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                // Menangani error jika koneksi gagal
                Toast.makeText(this@TiketOfflineActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun formatWithThousandSeparator(input: String): String {
        if (input.isEmpty()) return ""
        
        val number = input.toIntOrNull() ?: return ""
        
        // Format with thousand separators using CurrencyHelper
        return formatCurrency(number)
    }

}