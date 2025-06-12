package com.febrivio.sumbercomplang

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.adapter.DetailTransaksiTiketAdapter
import com.febrivio.sumbercomplang.databinding.ActivityDetailTransaksiBinding
import com.febrivio.sumbercomplang.model.TransaksiData
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetailTransaksiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailTransaksiBinding
    private lateinit var orderId: String
    private var transaksiData: TransaksiData? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailTransaksiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { onBackPressed() }

        transaksiData = intent.getSerializableExtra("transaksi") as? TransaksiData
        if (transaksiData == null) {
            finish()
            return
        }

        orderId = transaksiData!!.orderId
        setupSwipeToRefresh()
        setupTransactionDetails(transaksiData!!)
        setupPaymentButtons()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchTransactionDetail(orderId)
        }
    }

    private fun fetchTransactionDetail(orderId: String) {
        binding.swipeRefreshLayout.isRefreshing = true

        ApiClient.instance.getTransaksiDetail(orderId)
            .enqueue(object : Callback<TransaksiTiketResponse> {
                override fun onResponse(
                    call: Call<TransaksiTiketResponse>,
                    response: Response<TransaksiTiketResponse>
                ) {
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        if (data != null) {
                            setupTransactionDetails(data)

                            // Cek status transaksi, hentikan polling jika status final
                            val status = data.status.lowercase()
                            if (status == "divalidasi" || status == "gagal" || status == "dibatalkan") {
                                handler.removeCallbacks(refreshRunnable)
                            }
                        }
                    }

                    if (response.isSuccessful && response.body() != null) {
                        transaksiData = response.body()!!.data
                        transaksiData?.let {
                            setupTransactionDetails(it)
                        }
                    } else {
                        Toast.makeText(this@DetailTransaksiActivity, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@DetailTransaksiActivity, "Kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupTransactionDetails(transaksiData: TransaksiData) {
        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        formatRupiah.maximumFractionDigits = 0

        // Atur visibilitas button setelah transaksiData siap
        if (transaksiData.status.equals("menunggu", ignoreCase = true)) {
            binding.btnBayar.visibility = View.VISIBLE
            binding.btnBatalkan.visibility = View.VISIBLE
        } else {
            binding.btnBayar.visibility = View.GONE
            binding.btnBatalkan.visibility = View.GONE
        }

        binding.tvTransactionId.text = "ID#${transaksiData.orderId}"
        binding.tvStatus.text = transaksiData.status

        when (transaksiData.status.lowercase()) {
            "menunggu" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            "dibayar" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_success)
            "divalidasi" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_validate)
            "gagal", "dibatalkan" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_failed)
            "selesai" -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_finish)
            else -> binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
        }

        binding.tvDate.text = transaksiData.date

        val detailList = transaksiData.tiketDetails
        val adapter = DetailTransaksiTiketAdapter(detailList) // Sesuaikan constructor-nya

        // Atur layout manager dan adapter ke RecyclerView
        binding.listItems.layoutManager = LinearLayoutManager(this)
        binding.listItems.adapter = adapter

        // Matikan scroll agar tidak bisa discroll
        binding.listItems.isNestedScrollingEnabled = false


        binding.tvTotalAmount.text = formatRupiah
            .format(transaksiData.grossAmount.toLong())
            .replace("Rp", "")
            .trim()

        binding.btnTunai.text = transaksiData.paymentType

        // Tampilkan QR code hanya jika status == "dibayar"
        val status = transaksiData.status.lowercase()
        val inflater = LayoutInflater.from(this)

        when (status) {
            "dibayar" -> {
                val qrBitmap = generateQRCode(transaksiData.orderId)
                binding.qrImageView.setImageBitmap(qrBitmap)
                binding.qrImageView.visibility = View.VISIBLE

                binding.stempelContainer.removeAllViews()
                binding.stempelContainer.visibility = View.GONE
            }

            "divalidasi" -> {
                val qrBitmap = generateQRCode(transaksiData.orderId)
                binding.qrImageView.setImageBitmap(qrBitmap)
                binding.qrImageView.visibility = View.VISIBLE

                binding.stempelContainer.removeAllViews()
                binding.stempelContainer.visibility = View.GONE
            }

            "selesai" -> {
                binding.qrImageView.visibility = View.GONE

                // Inflate layout stempel dan tambahkan ke container
                val stempelView = inflater.inflate(R.layout.stempel_sudah_digunakan, binding.stempelContainer, false)
                binding.stempelContainer.removeAllViews()
                binding.stempelContainer.addView(stempelView)
                binding.stempelContainer.visibility = View.VISIBLE
            }

            "dibatalkan", "gagal" -> {
                binding.qrImageView.visibility = View.GONE

                val stempelView = inflater.inflate(R.layout.stempel_telah_dibatalkan, binding.stempelContainer, false)
                binding.stempelContainer.removeAllViews()
                binding.stempelContainer.addView(stempelView)
                binding.stempelContainer.visibility = View.VISIBLE
            }

            else -> {
                binding.qrImageView.visibility = View.GONE
                binding.stempelContainer.removeAllViews()
                binding.stempelContainer.visibility = View.GONE
            }
        }
    }

    private fun setupPaymentButtons() {
        binding.btnBayar.setOnClickListener {
            transaksiData?.redirectUrl?.let { url ->
                if (url.isNotEmpty()) {
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    startActivity(intent)
                }
            }
        }

        binding.btnBatalkan.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun showCancelConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Batalkan Transaksi")
            .setMessage("Apakah Anda yakin ingin membatalkan transaksi ini?")
            .setPositiveButton("Ya") { _, _ ->
                cancelTransaction()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun cancelTransaction() {
        binding.btnBatalkan.isEnabled = false

        ApiClient.instance.cancelTransaction(orderId)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    binding.btnBatalkan.isEnabled = true

                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(response.body()?.string() ?: "")
                            val success = jsonResponse.optBoolean("success", false)
                            val message = jsonResponse.optString("message", "")

                            if (success) {
                                Toast.makeText(
                                    this@DetailTransaksiActivity,
                                    "Transaksi berhasil dibatalkan",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Refresh data to show updated status
                                fetchTransactionDetail(orderId)
                            } else {
                                Toast.makeText(
                                    this@DetailTransaksiActivity,
                                    message.ifEmpty { "Gagal membatalkan transaksi" },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@DetailTransaksiActivity,
                                "Gagal memproses response",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@DetailTransaksiActivity,
                            "Gagal membatalkan transaksi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    binding.btnBatalkan.isEnabled = true
                    Toast.makeText(
                        this@DetailTransaksiActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun generateQRCode(text: String): Bitmap {
        val size = 512 // Ukuran QR
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchTransactionDetail(orderId) // panggil API
            handler.postDelayed(this, 10_000) // ulangi setiap 10 detik
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable) // mulai polling saat halaman tampil
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable) // berhenti polling saat pindah halaman
    }

}
