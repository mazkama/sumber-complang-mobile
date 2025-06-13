package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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

class TiketOfflineActivity : AppCompatActivity(){
    private lateinit var b: ActivityTiketBinding
    private lateinit var tiketAdapter: TiketAdapter
    private var jenisTiket : String =  ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTiketBinding.inflate(layoutInflater)
        setContentView(b.root)

        jenisTiket = intent.getStringExtra("jenis_tiket").toString()

        // Set RecyclerView Layout Manager
        b.rvTiketKolam.layoutManager = LinearLayoutManager(this)

        // Set up SwipeRefreshLayout
        b.swipeRefreshLayout.setOnRefreshListener {
            // Call getKolamData to refresh data
            getTiketData(jenisTiket)
        }

        b.btnBack.setOnClickListener {
            finish()
        }

        // Fetch data on first load
        getTiketData(jenisTiket)
    }

    private fun getTiketData(jenis : String) {
        // Tampilkan indikator refresh
        b.swipeRefreshLayout.isRefreshing = true

        ApiClient.instance.getTiket(jenis).enqueue(object : Callback<TiketResponse> {
            override fun onResponse(call: Call<TiketResponse>, response: Response<TiketResponse>) {
                // Hentikan indikator refresh
                b.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val tiketResponse = response.body()
                    val kolamList = tiketResponse?.data

                    if (!kolamList.isNullOrEmpty()) {
                        // Inisialisasi adapter dengan onItemClick
                        tiketAdapter = TiketAdapter(kolamList) { tiket: Tiket ->
                            val intent = Intent(this@TiketOfflineActivity, FormTiketActivity::class.java)
                            // Kirim data jika perlu
                            intent.putExtra("id_tiket", tiket.id_tiket)
                            intent.putExtra("nama_tiket", tiket.nama_tiket)
                            intent.putExtra("harga", tiket.harga)
                            intent.putExtra("kategori", tiket.kategori)
                            intent.putExtra("jenis", tiket.jenis)
                            intent.putExtra("deskripsi", tiket.deskripsi)
                            startActivity(intent)

                        }

                        b.rvTiketKolam.adapter = tiketAdapter
                    } else {
                        Toast.makeText(
                            this@TiketOfflineActivity,
                            "Data tiket kolam kosong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@TiketOfflineActivity,
                        "Gagal mengambil data (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<TiketResponse>, t: Throwable) {
                // Hentikan indikator refresh
                b.swipeRefreshLayout.isRefreshing = false

                Toast.makeText(
                    this@TiketOfflineActivity,
                    "Terjadi kesalahan: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Menyegarkan data saat activity kembali muncul
    override fun onResume() {
        super.onResume()
        getTiketData(jenisTiket)  // Panggil ulang data saat activity kembali muncul
    }
}
