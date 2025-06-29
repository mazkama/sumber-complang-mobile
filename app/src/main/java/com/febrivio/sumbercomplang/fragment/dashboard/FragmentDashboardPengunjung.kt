package com.febrivio.sumbercomplang.fragment.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.ChangePasswordActivity
import com.febrivio.sumbercomplang.DetailKolamActivity
import com.febrivio.sumbercomplang.FormKolamActivity
import com.febrivio.sumbercomplang.KolamActivity
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.TransaksiTiketActivity
import com.febrivio.sumbercomplang.adapter.KolamAdapter
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPengunjungBinding
import com.febrivio.sumbercomplang.model.KolamListResponse
import com.febrivio.sumbercomplang.model.CountThisMonthResponse
import com.febrivio.sumbercomplang.model.Kolam
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentDashboardPengunjung : Fragment() {

    lateinit var b: FragmentBerandaPengunjungBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View
    private lateinit var kolamAdapter: KolamAdapter

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentBerandaPengunjungBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        // Set RecyclerView Layout Manager
        b.rvKolam.layoutManager = LinearLayoutManager(thisParent)

        // Set up SwipeRefreshLayout
        b.swipeRefreshLayout.setOnRefreshListener {
            // Call getKolamData to refresh data
            countThisMonth = null
            kolamListCache = null
            getKolamData()
        }


        b.tvUserName.setText(session.getUserName())
        b.tvUsername.setText("@${session.getUserUsername()}")

        b.cardTiketKolam.setOnClickListener {
            val intent = Intent(thisParent, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "kolam")
            startActivity(intent)
        }

        b.cardParkir.setOnClickListener {
            val intent = Intent(thisParent, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "parkir")
            startActivity(intent)
        }


        b.cardPengaturan.setOnClickListener {
            val intent = Intent(thisParent, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Fetch data on first load
        getKolamData()

        return v
    }

    private var countThisMonth: CountThisMonthResponse? = null
    private var kolamListCache: List<Kolam>? = null

    private fun getKolamData() {
        // Jika data sudah ada, langsung tampilkan tanpa request ulang
        if (countThisMonth != null && kolamListCache != null) {
            setKolamAdapter()
            return
        }

        b.swipeRefreshLayout.isRefreshing = true

        ApiClient.instance.getCountThisMonth().enqueue(object : Callback<CountThisMonthResponse> {
            override fun onResponse(
                call: Call<CountThisMonthResponse>,
                response: Response<CountThisMonthResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    countThisMonth = response.body()
                    ApiClient.instance.getKolam().enqueue(object : Callback<KolamListResponse> {
                        override fun onResponse(
                            call: Call<KolamListResponse>,
                            response: Response<KolamListResponse>
                        ) {
                            b.swipeRefreshLayout.isRefreshing = false
                            if (response.isSuccessful && response.body() != null) {
                                kolamListCache = response.body()!!.data
                                setKolamAdapter()
                            } else {
                                Toast.makeText(thisParent, "Gagal mengambil data kolam", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<KolamListResponse>, t: Throwable) {
                            b.swipeRefreshLayout.isRefreshing = false
                            Toast.makeText(thisParent, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    b.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(thisParent, "Gagal mengambil data pengunjung", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<CountThisMonthResponse>, t: Throwable) {
                b.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(thisParent, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setKolamAdapter() {
        val kolamList = kolamListCache ?: return
        val countData = countThisMonth
        kolamAdapter = KolamAdapter(kolamList) { kolam ->
            val intent = Intent(thisParent, DetailKolamActivity::class.java)
            intent.putExtra("kolam", kolam)
            when (kolam.nama?.lowercase()) {
                "kolam anak" -> {
                    intent.putExtra("bulan", countData?.bulan)
                    intent.putExtra("count", countData?.total_kolam_anak ?: 0)
                }
                "kolam dewasa" -> {
                    intent.putExtra("bulan", countData?.bulan)
                    intent.putExtra("count", countData?.total_kolam_dewasa ?: 0)
                }
                "kolam alam complang" -> {
                    intent.putExtra("bulan", countData?.bulan)
                    intent.putExtra("count", countData?.total_parkir ?: 0)
                }
                else -> {
                    intent.putExtra("bulan", countData?.bulan)
                    intent.putExtra("count", 0)
                }
            }
            startActivity(intent)
        }
        b.rvKolam.adapter = kolamAdapter
    }

}