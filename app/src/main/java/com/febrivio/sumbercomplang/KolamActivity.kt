package com.febrivio.sumbercomplang

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.adapter.KolamAdapter
import com.febrivio.sumbercomplang.databinding.ActivityKolamBinding
import com.febrivio.sumbercomplang.model.Kolam
import com.febrivio.sumbercomplang.model.KolamListResponse
import com.febrivio.sumbercomplang.network.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KolamActivity : AppCompatActivity() {

    private lateinit var b: ActivityKolamBinding
    private lateinit var kolamAdapter: KolamAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityKolamBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Set RecyclerView Layout Manager
        b.rvKolam.layoutManager = LinearLayoutManager(this)

        // Set up SwipeRefreshLayout
        b.swipeRefreshLayout.setOnRefreshListener {
            // Call getKolamData to refresh data
            getKolamData()
        }

        // Button for navigating to FormKolamActivity
        b.btnAddKolam.setOnClickListener {
            val intent = Intent(this, FormKolamActivity::class.java)
            // Start FormKolamActivity and expect a result
            startActivity(intent)
        }

        b.btnBack.setOnClickListener {
            finish()
        }

        // Fetch data on first load
        getKolamData()
    }

    private fun getKolamData() {
        // Show the refresh indicator
        b.swipeRefreshLayout.isRefreshing = true

        ApiClient.instance.getKolam().enqueue(object : Callback<KolamListResponse> {
            override fun onResponse(call: Call<KolamListResponse>, response: Response<KolamListResponse>) {
                // Stop refreshing indicator
                b.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val kolamList = response.body()!!.data
                    kolamAdapter = KolamAdapter(kolamList)
                    b.rvKolam.adapter = kolamAdapter
                } else {
                    Toast.makeText(this@KolamActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<KolamListResponse>, t: Throwable) {
                // Stop refreshing indicator
                b.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@KolamActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Menyegarkan data saat activity kembali muncul
    override fun onResume() {
        super.onResume()
        getKolamData()  // Panggil ulang data saat activity kembali muncul
    }
}
