package com.febrivio.sumbercomplang.fragment.riwayat

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.DetailTransaksiActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.R
import com.febrivio.sumbercomplang.adapter.RiwayatTransaksiAdapter
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTransaksiBinding
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.model.RiwayatTransaksiTiketResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class FragmentRiwayatTransaksi : Fragment() {

    private lateinit var b: FragmentRiwayatTransaksiBinding
    private lateinit var riwayatTransaksiAdapter: RiwayatTransaksiAdapter
    private val riwayatTransaksiList = mutableListOf<RiwayatTransaksiItem>()
    private lateinit var session: SessionManager

    // Filter state
    private var selectedJenis: String = ""
    private var selectedStatus: String = ""
    private var startDate: String = ""
    private var endDate: String = ""
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        b = FragmentRiwayatTransaksiBinding.inflate(inflater, container, false)
        session = SessionManager(requireContext())

        setupRecyclerView()
        setupFilterUI()
        setupSwipeRefresh()

        // Set default filter (hari ini)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        startDate = today
        endDate = today
        b.tvStartDate.text = today
        b.tvEndDate.text = today

        // Load data pertama kali
        fetchRiwayatTransaksiData()

        return b.root
    }

    private fun setupRecyclerView() {
        riwayatTransaksiAdapter = RiwayatTransaksiAdapter(riwayatTransaksiList) { item ->
            fetchTransactionDetail(item.order_id)
        }
        b.rvTransaksi.layoutManager = LinearLayoutManager(requireContext())
        b.rvTransaksi.adapter = riwayatTransaksiAdapter
    }

    private fun setupFilterUI() {
        // Jenis Spinner
        val jenisList = listOf("Semua", "Kolam", "Parkir")
        val jenisValue = listOf("", "kolam", "parkir")
        b.spinnerJenis.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, jenisList)
        b.spinnerJenis.setSelection(0)
        b.spinnerJenis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedJenis = jenisValue[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Status Spinner
        val statusList = listOf("Semua", "Menunggu", "Dibayar", "Cekin", "Selesai", "Gagal", "Dibatalkan")
        val statusValue = listOf("", "menunggu", "dibayar", "cekin", "selesai", "gagal", "dibatalkan")
        b.spinnerStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)
        b.spinnerStatus.setSelection(0)
        b.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedStatus = statusValue[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Tanggal
        b.tvStartDate.setOnClickListener {
            showDatePicker(startDate) { date ->
                startDate = date
                b.tvStartDate.text = date
            }
        }
        b.tvEndDate.setOnClickListener {
            showDatePicker(endDate) { date ->
                endDate = date
                b.tvEndDate.text = date
            }
        }

        // Tombol Terapkan Filter
        b.btnTerapkanFilter.setOnClickListener {
            currentPage = 1
            isLastPage = false
            fetchRiwayatTransaksiData()
        }

        // Tombol Reset Filter
        b.btnResetFilter.setOnClickListener {
            b.spinnerJenis.setSelection(0)
            b.spinnerStatus.setSelection(0)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            startDate = today
            endDate = today
            b.tvStartDate.text = today
            b.tvEndDate.text = today
            currentPage = 1
            isLastPage = false
            fetchRiwayatTransaksiData()
            fetchRiwayatTransaksiData()
        }

        b.tvToggleFilter.setOnClickListener {
            if (b.filterSection.visibility == View.VISIBLE) {
                b.filterSection.visibility = View.GONE
                b.tvToggleFilter.text = "Tampilkan Filter"
                b.tvToggleFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            } else {
                b.filterSection.visibility = View.VISIBLE
                b.tvToggleFilter.text = "Sembunyikan Filter"
                b.tvToggleFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_up, 0)
            }
        }
    }

    private fun setupSwipeRefresh() {
        b.swipeRefresh.setOnRefreshListener {
            currentPage = 1
            isLastPage = false
            fetchRiwayatTransaksiData()
        }
    }

    private fun showDatePicker(current: String, onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            cal.time = sdf.parse(current) ?: Date()
        } catch (_: Exception) {}
        DatePickerDialog(requireContext(),
            { _, year, month, dayOfMonth ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(date)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchRiwayatTransaksiData() {
        if (isLoading) return
        isLoading = true
        b.swipeRefresh.isRefreshing = true
        if (currentPage == 1) riwayatTransaksiList.clear()

        val token = session.getToken()
        ApiServiceAuth(requireContext(), token).getRiwayatTransaksi(
            jenis = selectedJenis,
            status = selectedStatus,
            startDate = startDate,
            endDate = endDate,
            page = currentPage
        ).enqueue(object : Callback<RiwayatTransaksiTiketResponse> {
            override fun onResponse(
                call: Call<RiwayatTransaksiTiketResponse>,
                response: Response<RiwayatTransaksiTiketResponse>
            ) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data?.data ?: emptyList()
                    riwayatTransaksiList.addAll(data)
                    riwayatTransaksiAdapter.notifyDataSetChanged()
                    isLastPage = response.body()?.data?.last_page == currentPage
                    b.tvDataNon.visibility = if (riwayatTransaksiList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    b.tvDataNon.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<RiwayatTransaksiTiketResponse>, t: Throwable) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false
                b.tvDataNon.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchTransactionDetail(orderId: String) {
        val token = session.getToken()
        ApiServiceAuth(requireContext(), token).getTransaksiDetail(orderId)
            .enqueue(object : Callback<TransaksiTiketResponse> {
                override fun onResponse(
                    call: Call<TransaksiTiketResponse>,
                    response: Response<TransaksiTiketResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val intent = Intent(requireContext(), DetailTransaksiActivity::class.java)
                        intent.putExtra("transaksi", response.body()?.data)
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat detail transaksi", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Gagal memuat detail transaksi", Toast.LENGTH_SHORT).show()
                }
            })
    }
}