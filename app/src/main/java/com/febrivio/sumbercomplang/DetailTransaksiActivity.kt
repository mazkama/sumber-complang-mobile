package com.febrivio.sumbercomplang

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.ActivityDetailTransaksiBinding
import org.json.JSONArray
import org.json.JSONObject

class DetailTransaksiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailTransaksiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailTransaksiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val jsonData = intent.getStringExtra("transaksi_data") ?: return
        val jsonObject = JSONObject(jsonData)

        // Set data utama
        binding.tvOrderId.text = "Order ID: ${jsonObject.getString("order_id")}"
        binding.tvTanggal.text = "Tanggal: ${jsonObject.getString("tanggal")}"
        binding.tvMetodePembayaran.text = "Metode: ${jsonObject.getString("metode_pembayaran")}"
        binding.tvStatus.text = "Status: ${jsonObject.getString("status")}"
        binding.tvTotalHarga.text = "Total: Rp${jsonObject.getString("total_harga")}"

        // Data user
        val user = jsonObject.getJSONObject("user")
        binding.tvUserNama.text = "Nama: ${user.getString("nama")}"
        binding.tvUserEmail.text = "Email: ${user.getString("email")}"

        // Detail tiket
        val detailTiketArray = jsonObject.getJSONArray("detail_tiket")
        renderDetailTiket(detailTiketArray)
    }

    private fun renderDetailTiket(array: JSONArray) {
        for (i in 0 until array.length()) {
            val tiketObj = array.getJSONObject(i)
            val tiketView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null)

            val tv1 = tiketView.findViewById<TextView>(android.R.id.text1)
            val tv2 = tiketView.findViewById<TextView>(android.R.id.text2)

            tv1.text = "${tiketObj.getString("nama_tiket")} (x${tiketObj.getInt("jumlah")})"
            tv2.text = "Subtotal: Rp${tiketObj.getString("subtotal")}"

            binding.tiketContainer.addView(tiketView)
        }
    }
}
