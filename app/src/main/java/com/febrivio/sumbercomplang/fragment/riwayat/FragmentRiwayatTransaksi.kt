package com.febrivio.sumbercomplang.fragment.riwayat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.DetailTransaksiActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.adapter.RiwayatTransaksiAdapter
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTransaksiBinding
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.model.RiwayatTransaksiTiketResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentRiwayatTransaksi : Fragment() {

    lateinit var b: FragmentRiwayatTransaksiBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View

    private lateinit var riwayatTransaksiAdapter: RiwayatTransaksiAdapter
    private val riwayatTransaksiList = mutableListOf<RiwayatTransaksiItem>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var selectedJenis = ""

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        setupRecyclerView()
        setupListeners()

        // Initialize "Semua" as selected
        b.btnSemua.isSelected = true
        selectedJenis = ""
        fetchRiwayatTransaksiData(selectedJenis)

        return v
    }

    private fun setupRecyclerView() {
        riwayatTransaksiAdapter = RiwayatTransaksiAdapter(riwayatTransaksiList) { item ->
            // Handle item click by fetching details
            fetchTransactionDetail(item.order_id)
        }
        with(b.rvTransaksi) {
            layoutManager = LinearLayoutManager(thisParent)
            adapter = riwayatTransaksiAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    if (!isLoading && !isLastPage && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - 5) {
                        fetchRiwayatTransaksiData()
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        with(b) {
            val buttons = listOf(btnSemua, btnKolam, btnParkir)

            fun updateButtonStates(selectedButton: TextView) {
                buttons.forEach { button ->
                    button.isSelected = button == selectedButton
                }
            }

            // Set initial state for "Semua"
            updateButtonStates(btnSemua)

            btnSemua.setOnClickListener {
                updateButtonStates(btnSemua)
                selectedJenis = ""
                refreshData()
            }

            btnKolam.setOnClickListener {
                updateButtonStates(btnKolam)
                selectedJenis = "kolam"
                refreshData()
            }

            btnParkir.setOnClickListener {
                updateButtonStates(btnParkir)
                selectedJenis = "parkir"
                refreshData()
            }

            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isRefreshing = true
                refreshData()
            }
        }
    }

    private fun fetchRiwayatTransaksiData(jenis: String = "") {
        if (isLoading) return
        isLoading = true
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()

        ApiServiceAuth(thisParent,token).getRiwayatTransaksi(jenis, currentPage).enqueue(object : Callback<RiwayatTransaksiTiketResponse> {
            override fun onResponse(call: Call<RiwayatTransaksiTiketResponse>, response: Response<RiwayatTransaksiTiketResponse>) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false

                response.body()?.let { body ->
                    if (body.success) {
                        val listData = body.data.data // Get the List<RiwayatTransaksiItem>

                        if (currentPage == 1) {
                            riwayatTransaksiList.clear()
                        }
                        riwayatTransaksiList.addAll(listData)
                        riwayatTransaksiAdapter.notifyDataSetChanged()

                        isLastPage = currentPage >= body.data.last_page
                        if (!isLastPage) currentPage++

                        b.tvDataNon.visibility = if (riwayatTransaksiList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<RiwayatTransaksiTiketResponse>, t: Throwable) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false
                if (currentPage == 1) b.tvDataNon.visibility = View.VISIBLE
            }
        })
    }

    private fun fetchTransactionDetail(orderId: String) {
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()

        ApiServiceAuth(thisParent, token).getTransaksiDetail(orderId)
            .enqueue(object : Callback<TransaksiTiketResponse> {
                override fun onResponse(
                    call: Call<TransaksiTiketResponse>,
                    response: Response<TransaksiTiketResponse>
                ) {
                    b.swipeRefresh.isRefreshing = false
                    if (response.isSuccessful && response.body() != null) {
                        response.body()?.let { transaksiResponse ->
                            if (transaksiResponse.success) {
                                // Navigate to detail activity
                                val intent = Intent(requireContext(), DetailTransaksiActivity::class.java)
                                intent.putExtra("transaksi", transaksiResponse.data)
                                startActivity(intent)
                            } else {
                                Toast.makeText(requireContext(), "Gagal memuat detail transaksi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                    b.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun refreshData() {
        currentPage = 1
        isLastPage = false
        b.tvDataNon.visibility = View.GONE
        fetchRiwayatTransaksiData(selectedJenis) // Pass the selectedJenis parameter
    }
}