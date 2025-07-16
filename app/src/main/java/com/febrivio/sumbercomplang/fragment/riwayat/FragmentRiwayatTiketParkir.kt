package com.febrivio.sumbercomplang.fragment.riwayat

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.febrivio.sumbercomplang.DetailTransaksiActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.R
import com.febrivio.sumbercomplang.adapter.RiwayatTransaksiAdapter
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTiketKolamBinding
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTiketParkirBinding
import com.febrivio.sumbercomplang.model.RekapPembayaranResponse
import com.febrivio.sumbercomplang.model.RiwayatTransaksiItem
import com.febrivio.sumbercomplang.model.RiwayatTransaksiTiketResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.PdfDownloadHelper
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FragmentRiwayatTiketParkir : Fragment() {

    lateinit var b: FragmentRiwayatTiketParkirBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View

    private lateinit var riwayatTransaksiAdapter: RiwayatTransaksiAdapter
    private val riwayatTransaksiList = mutableListOf<RiwayatTransaksiItem>()
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var selectedStatus: String = ""
    private var startDate: String = ""
    private var endDate: String = ""
    private var isRekapMode = false

    private var lastRefreshTime: Long = 0

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentRiwayatTiketParkirBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        setupRecyclerView()
        setupListeners()

        // Inisialisasi tanggal hari ini
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        startDate = today
        endDate = today
        b.tvStartDate.text = today
        b.tvEndDate.text = today

        setupFilterUI()
        fetchRiwayatTransaksiData(selectedStatus)

        // Set text cetak laporan sesuai rentang tanggal filter
        b.btnCetakLaporan.text = "Cetak Laporan - $startDate s/d $endDate"

        // Cetak laporan
        b.btnCetakLaporan.setOnClickListener {
            if (riwayatTransaksiList.isEmpty()) {
                Toast.makeText(thisParent, "Tidak ada data untuk dicetak.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val token = session.getToken()
            val pdfHelper = PdfDownloadHelper(thisParent)
            pdfHelper.downloadReportByDate(
                startDate = startDate,
                endDate = endDate,
                jenis = "parkir",
                token = token.toString(),
                onStartDownload = { b.swipeRefresh.isRefreshing = true },
                onFinishDownload = { activity?.runOnUiThread { b.swipeRefresh.isRefreshing = false } },
                onError = { errorMessage -> activity?.runOnUiThread {
                    b.swipeRefresh.isRefreshing = false
                    Toast.makeText(thisParent, errorMessage, Toast.LENGTH_SHORT).show()
                } }
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
                        if (isRekapMode) {
                            fetchRekapPembayaran()
                        } else {
                            fetchRiwayatTransaksiData(selectedStatus)
                        }
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        with(b) {
            val buttons = listOf(btnSemua, btnRekap)

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
                isRekapMode = false
                cardRekapOmset.visibility = View.GONE
                layoutStatusFilter.visibility = View.VISIBLE
                refreshData()
            }

            btnRekap.setOnClickListener {
                updateButtonStates(btnRekap)
                selectedStatus = "dibayar,selesai,divalidasi"
                isRekapMode = true
                cardRekapOmset.visibility = View.VISIBLE
                layoutStatusFilter.visibility = View.GONE
                fetchRekapPembayaran()
            }

            swipeRefresh.setOnRefreshListener {
                swipeRefresh.isRefreshing = true
                if (isRekapMode) fetchRekapPembayaran() else refreshData()
            }
        }
    }

    private fun setupFilterUI() {
        // Status Spinner
        val statusList = listOf("Semua", "Menunggu", "Dibayar", "Divalidasi", "Selesai", "Gagal", "Dibatalkan")
        val statusValue = listOf("", "menunggu", "dibayar", "divalidasi", "selesai", "gagal", "dibatalkan")
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
                b.btnCetakLaporan.text = "Cetak Laporan - $startDate s/d $endDate"
            }
        }
        b.tvEndDate.setOnClickListener {
            showDatePicker(endDate) { date ->
                endDate = date
                b.tvEndDate.text = date
                b.btnCetakLaporan.text = "Cetak Laporan - $startDate s/d $endDate"
            }
        }
        // Tombol Terapkan Filter
        b.btnTerapkanFilter.setOnClickListener {
            currentPage = 1
            isLastPage = false
            if (isRekapMode) {
                fetchRekapPembayaran()
            } else {
                fetchRiwayatTransaksiData(selectedStatus)
            }
        }
        // Tombol Reset Filter
        b.btnResetFilter.setOnClickListener {
            b.spinnerStatus.setSelection(0)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            startDate = today
            endDate = today
            b.tvStartDate.text = today
            b.tvEndDate.text = today
            b.btnCetakLaporan.text = "Cetak Laporan - $startDate s/d $endDate"
            currentPage = 1
            isLastPage = false
            if (isRekapMode) {
                fetchRekapPembayaran()
            } else {
                fetchRiwayatTransaksiData("")
            }
        }
        // Toggle filter
        b.tvToggleFilter.setOnClickListener {
            if (b.filterSection.visibility == View.VISIBLE) {
                b.filterSection.visibility = View.GONE
                b.tvToggleFilter.text = "Filter"
                b.tvToggleFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            } else {
                b.filterSection.visibility = View.VISIBLE
                b.tvToggleFilter.text = "Filter"
                b.tvToggleFilter.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_up, 0)
            }
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

    private fun fetchRiwayatTransaksiData(status: String = "") {
        if (isLoading) return
        isLoading = true
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()
        ApiServiceAuth(thisParent, token).getRiwayatTransaksi(
            jenis = "parkir",
            status = status,
            startDate = startDate,
            endDate = endDate,
            page = currentPage
        ).enqueue(object : Callback<RiwayatTransaksiTiketResponse> {
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

    private fun fetchRekapPembayaran() {
        if (isLoading) return
        isLoading = true
        b.swipeRefresh.isRefreshing = true
        val token = session.getToken()
        ApiServiceAuth(thisParent, token).getRekapPembayaran(
            status = "dibayar,selesai,divalidasi",
            startDate = startDate,
            endDate = endDate,
            jenis = "parkir",
            page = currentPage
        ).enqueue(object : Callback<RekapPembayaranResponse> {
            override fun onResponse(call: Call<RekapPembayaranResponse>, response: Response<RekapPembayaranResponse>) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    b.cardRekapOmset.visibility = View.VISIBLE
                    b.tvTotalTransaksi.text = "${body.total_transaksi}"
                    b.tvTotalPembayaran.text = "${body.total_pembayaran}"
                    val listData = body.data.data
                    if (currentPage == 1) {
                        riwayatTransaksiList.clear()
                    }
                    riwayatTransaksiList.addAll(listData)
                    riwayatTransaksiAdapter.notifyDataSetChanged()
                    isLastPage = currentPage >= body.data.last_page
                    if (!isLastPage) currentPage++
                    b.tvDataNon.visibility = if (riwayatTransaksiList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    b.cardRekapOmset.visibility = View.GONE
                    Toast.makeText(thisParent, "Gagal memuat rekap pembayaran", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<RekapPembayaranResponse>, t: Throwable) {
                isLoading = false
                b.swipeRefresh.isRefreshing = false
                b.cardRekapOmset.visibility = View.GONE
                Toast.makeText(thisParent, "Gagal memuat rekap pembayaran", Toast.LENGTH_SHORT).show()
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