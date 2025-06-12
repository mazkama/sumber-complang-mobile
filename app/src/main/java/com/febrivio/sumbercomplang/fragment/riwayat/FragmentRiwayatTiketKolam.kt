package com.febrivio.sumbercomplang.fragment.riwayat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.DetailTransaksiActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.adapter.RiwayatTransaksiAdapter
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTiketKolamBinding
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.model.RiwayatTransaksiTiketResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.PdfDownloadHelper
import com.febrivio.sumbercomplang.services.SessionManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class FragmentRiwayatTiketKolam : Fragment() {

    lateinit var b: FragmentRiwayatTiketKolamBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View

    private lateinit var riwayatTransaksiAdapter: RiwayatTransaksiAdapter
    private val riwayatTransaksiList = mutableListOf<RiwayatTransaksiItem>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var selectedStatus = ""

    private var lastRefreshTime: Long = 0

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentRiwayatTiketKolamBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        setupRecyclerView()
        setupListeners()

        // Initialize "Semua" as selected
        b.btnSemua.isSelected = true
        selectedStatus = ""
        fetchRiwayatTransaksiData(selectedStatus)

        // Get current month and year
        val calendar = Calendar.getInstance()
        val monthName = getIndonesianMonth(calendar.get(Calendar.MONTH))
        val year = calendar.get(Calendar.YEAR)

        // Set button text with current month and year
        b.btnCetakLaporan.text = "Cetak Laporan - $monthName $year"

        b.btnCetakLaporan.setOnClickListener {
            // Get current month and year
            val calendar = Calendar.getInstance()
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val year = calendar.get(Calendar.YEAR).toString()

            // Get token
            val token = session.getToken()

            // Use the helper class
            val pdfHelper = PdfDownloadHelper(thisParent)
            pdfHelper.downloadMonthlyReport(
                month = month,
                year = year,
                reportType = "kolam",
                token = token.toString(),
                onStartDownload = {
                    // Show loading indicator
                    b.swipeRefresh.isRefreshing = true
                    Toast.makeText(thisParent, "Memulai download laporan...", Toast.LENGTH_SHORT).show()
                },
                onFinishDownload = {
                    // Hide loading indicator
                    activity?.runOnUiThread {
                        b.swipeRefresh.isRefreshing = false
                    }
                },
                onError = { errorMessage ->
                    // Handle error and hide loading
                    activity?.runOnUiThread {
                        b.swipeRefresh.isRefreshing = false
                        Toast.makeText(thisParent, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        return v
    }

    // Function to convert month number to Indonesian month name
    private fun getIndonesianMonth(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Januari"
            Calendar.FEBRUARY -> "Februari"
            Calendar.MARCH -> "Maret"
            Calendar.APRIL -> "April"
            Calendar.MAY -> "Mei"
            Calendar.JUNE -> "Juni"
            Calendar.JULY -> "Juli"
            Calendar.AUGUST -> "Agustus"
            Calendar.SEPTEMBER -> "September"
            Calendar.OCTOBER -> "Oktober"
            Calendar.NOVEMBER -> "November"
            Calendar.DECEMBER -> "Desember"
            else -> "Unknown"
        }
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
            val buttons = listOf(btnSemua, btnReserved)

            fun updateButtonStates(selectedButton: TextView) {
                buttons.forEach { button ->
                    button.isSelected = button == selectedButton
                }
            }

            // Set initial state for "Semua"
            updateButtonStates(btnSemua)

            btnSemua.setOnClickListener {
                updateButtonStates(btnSemua)
                selectedStatus = ""
                refreshData()
            }

            btnReserved.setOnClickListener {
                updateButtonStates(btnReserved)
                selectedStatus = "reserved"
                refreshData()
            }

            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isRefreshing = true
                refreshData()
            }
        }
    }

    private fun fetchRiwayatTransaksiData(status: String = "") {
        if (isLoading) return
        isLoading = true
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()

        ApiServiceAuth(thisParent,token).getRiwayatTransaksi("kolam", status,currentPage).enqueue(object : Callback<RiwayatTransaksiTiketResponse> {
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
        fetchRiwayatTransaksiData(selectedStatus) // Pass the selecteStatus parameter
    }

    override fun onResume() {
        super.onResume()
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime > 10_000) { // 10 detik
            fetchRiwayatTransaksiData()
            lastRefreshTime = currentTime
        }
    }
}