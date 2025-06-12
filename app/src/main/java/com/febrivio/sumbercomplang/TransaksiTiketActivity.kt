package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.adapter.TransaksiTiketAdapter
import com.febrivio.sumbercomplang.databinding.ActivityTransaksiTiketBinding
import com.febrivio.sumbercomplang.model.Tiket
import com.febrivio.sumbercomplang.model.TiketDetail
import com.febrivio.sumbercomplang.model.TiketResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketRequest
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import com.febrivio.sumbercomplang.utils.CurrencyHelper.formatCurrency
import com.google.gson.Gson
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class TransaksiTiketActivity : AppCompatActivity() {

    private lateinit var b: ActivityTransaksiTiketBinding
    private lateinit var transaksiTiketAdapter: TransaksiTiketAdapter
    private var selectedTiketList: List<Tiket> = emptyList()
    private var selectedPaymentMethod: String = "E-Wallet"
    private var jenisTiket : String =  ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTransaksiTiketBinding.inflate(layoutInflater)
        setContentView(b.root)

        jenisTiket = intent.getStringExtra("jenis_tiket").toString()

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
                            onQuantityChange = { updatedList ->
                                selectedTiketList = updatedList

                                var total = 0
                                for (tiket in selectedTiketList) {
                                    val subtotal = tiket.harga * tiket.jumlah
                                    total += subtotal
                                }
                            },
                            onPaymentMethodChanged = { newMethod ->
                                selectedPaymentMethod = newMethod
                            }
                        )

                        b.rvTiketKolam.adapter = transaksiTiketAdapter
                    } else {
                        Toast.makeText(this@TransaksiTiketActivity, "Data tiket kosong", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this@TransaksiTiketActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TiketResponse>, t: Throwable) {
                b.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@TransaksiTiketActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun showPaymentDialog() {
        if (selectedTiketList.isEmpty()) {
            Toast.makeText(this, "Belum ada tiket yang dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        var total = 0
        for (tiket in selectedTiketList) {
            val subtotal = tiket.harga * tiket.jumlah
            total += subtotal
            
            // Add base ticket information
            sb.append("${tiket.nama_tiket} x${tiket.jumlah} = Rp ${formatCurrency(subtotal)}\n")
            
            // Add license plate info if available
            if (tiket.no_kendaraan != null && tiket.no_kendaraan.isNotEmpty() && tiket.no_kendaraan != "-") {
                sb.append("   No. Kendaraan: ${tiket.no_kendaraan}\n")
            }
        }

        sb.append("\nMetode Pembayaran: $selectedPaymentMethod")
        sb.append("\nTotal: Rp ${formatCurrency(total)}")

        AlertDialog.Builder(this)
            .setTitle("Ringkasan Pembayaran")
            .setMessage(sb.toString())
            .setPositiveButton("Bayar") { dialog, _ ->
                dialog.dismiss()
                kirimTransaksiKeServer()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun kirimTransaksiKeServer() {
        val paymentMethodFormatted = when (selectedPaymentMethod) {
            "E-Wallet" -> "ewallet"
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
                        val redirectUrl = transaksiResponse.data?.redirectUrl
                        val orderId = transaksiResponse.data?.orderId
                        if (redirectUrl != null) {
//                            val intent = Intent(this@TransaksiTiketActivity, WebViewActivity::class.java)
//                            intent.putExtra("url", redirectUrl)
//                            intent.putExtra("order_id", orderId) // Order ID yang valid
//                            startActivity(intent)

                            val intent = Intent(this@TransaksiTiketActivity,
                                DetailTransaksiActivity::class.java)
                            intent.putExtra("transaksi", transaksiResponse.data)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@TransaksiTiketActivity, "URL redirect tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Jika `success` false atau response body tidak valid
                        Toast.makeText(this@TransaksiTiketActivity, "Transaksi gagal: ${transaksiResponse?.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Menangani response error (tidak berhasil)
                    val errorResponse = response.errorBody()?.string()
                    Toast.makeText(this@TransaksiTiketActivity, "Error dari server: $errorResponse", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                // Menangani error jika koneksi gagal
                Toast.makeText(this@TransaksiTiketActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

}