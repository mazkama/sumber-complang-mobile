package com.febrivio.sumbercomplang.fragment.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.DetailTransaksiActivity
import com.febrivio.sumbercomplang.KolamActivity
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.ScanTiketActivity
import com.febrivio.sumbercomplang.TiketActivity
import com.febrivio.sumbercomplang.TransaksiTiketActivity
import com.febrivio.sumbercomplang.adapter.RiwayatTransaksiAdapter
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPetugasParkirBinding
import com.febrivio.sumbercomplang.model.DashboardResponse
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentDashboardPetugasParkir :  Fragment() {

    lateinit var b: FragmentBerandaPetugasParkirBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View

    private lateinit var session: SessionManager
    private lateinit var riwayatAdapter: RiwayatTransaksiAdapter
    private val riwayatList = mutableListOf<RiwayatTransaksiItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentBerandaPetugasParkirBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        b.tvUserName.setText(session.getUserName())
        b.tvUsername.setText("@${session.getUserUsername()}")

        // Setup RecyclerView
        setupRecyclerView()

        // Setup refresh listener
        b.swipeRefresh.setOnRefreshListener {
            fetchDashboardData()
        }

        // Setup click listeners for service cards
        setupClickListeners()

        // Fetch initial data
        fetchDashboardData()

        return v
    }

    private fun setupRecyclerView() {
        // Initialize adapter with click handler to show transaction details
        riwayatAdapter = RiwayatTransaksiAdapter(riwayatList) { item ->
            fetchTransactionDetail(item.order_id)
        }

        // Set up RecyclerView
        b.rvHistory.apply {
            layoutManager = LinearLayoutManager(thisParent)
            adapter = riwayatAdapter
        }
    }

    private fun setupClickListeners() {
        b.cardTiket.setOnClickListener {
            val intent = Intent(thisParent, TiketActivity::class.java)
            intent.putExtra("jenis_tiket", "Parkir")
            startActivity(intent)
        }

        b.cardScan.setOnClickListener {
            val intent = Intent(thisParent, ScanTiketActivity::class.java)
            startActivity(intent)
        }

        b.cardPengaturan.setOnClickListener {
            session.logout()
            val intent = Intent(thisParent, LoginActivity::class.java)
            startActivity(intent)
            thisParent.finish()
        }
    }

    private fun fetchDashboardData() {
        // Show loading
        b.swipeRefresh.isRefreshing = true

        // Get token
        val token = session.getToken()

        // Make API call
        ApiClient.ApiServiceAuth(thisParent, token).getDashboardStatistics("parkir")
            .enqueue(object : Callback<DashboardResponse> {
                override fun onResponse(
                    call: Call<DashboardResponse>,
                    response: Response<DashboardResponse>
                ) {
                    // Hide loading
                    b.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        response.body()?.let { dashboardResponse ->
                            if (dashboardResponse.success) {
                                updateDashboardUI(dashboardResponse)
                            } else {
                                // API returned success=false
                                Toast.makeText(thisParent, dashboardResponse.message, Toast.LENGTH_SHORT).show()
                                showEmptyState()
                            }
                        }
                    } else {
                        // Handle HTTP error
                        Toast.makeText(
                            thisParent,
                            "Error: ${response.code()} - ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showEmptyState()
                    }
                }

                override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                    // Hide loading
                    b.swipeRefresh.isRefreshing = false

                    // Show error message
                    Toast.makeText(
                        thisParent,
                        "Gagal terhubung ke server: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.e("DashboardKolam", "API call failed", t)
                    showEmptyState()
                }
            })
    }

    private fun updateDashboardUI(response: DashboardResponse) {
        val data = response.data

        // Update statistics counts
        b.tvReservasiBulanCount.text = data.reserved_this_month.toString()
        b.tvReservasiHariCount.text = data.reserved_today.toString()

        // Convert recent transactions to RiwayatTransaksiItem
        val transactionItems = data.data

        // Update the RecyclerView
        riwayatList.clear()
        riwayatList.addAll(transactionItems)
        riwayatAdapter.notifyDataSetChanged()

        // Show/hide empty state
        if (riwayatList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        b.layoutEmptyState.visibility = View.VISIBLE
        b.rvHistory.visibility = View.GONE
    }

    private fun hideEmptyState() {
        b.layoutEmptyState.visibility = View.GONE
        b.rvHistory.visibility = View.VISIBLE
    }

    /**
     * Fetches transaction detail when a transaction item is clicked
     */
    private fun fetchTransactionDetail(orderId: String) {
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()

        ApiClient.ApiServiceAuth(thisParent, token).getTransaksiDetail(orderId)
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
}