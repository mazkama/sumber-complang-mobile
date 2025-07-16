package com.febrivio.sumbercomplang

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.febrivio.sumbercomplang.adapter.ValidasiTiketAdapter
import com.febrivio.sumbercomplang.databinding.ActivityValidasiTiketBinding
import com.febrivio.sumbercomplang.model.TiketDetailResponse
import com.febrivio.sumbercomplang.model.TransaksiData
import com.febrivio.sumbercomplang.model.TiketValidationRequest
import com.febrivio.sumbercomplang.model.TiketValidationResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ValidasiTiketActivity : AppCompatActivity() {

    private lateinit var b: ActivityValidasiTiketBinding
    private lateinit var validasiTiketAdapter: ValidasiTiketAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var transaksiData: TransaksiData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityValidasiTiketBinding.inflate(layoutInflater)
        setContentView(b.root)

        sessionManager = SessionManager(this)
        b.btnBack.setOnClickListener { onBackPressed() }

        transaksiData = intent.getSerializableExtra("transaksi") as? TransaksiData
            ?: run {
                Toast.makeText(this, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        b.tvOrderId.text = "ID#${transaksiData.orderId}"
        b.rvItemValidasi.layoutManager = LinearLayoutManager(this)
        validasiTiketAdapter = ValidasiTiketAdapter(transaksiData.tiketDetails)
        b.rvItemValidasi.adapter = validasiTiketAdapter

        setupValidateButton()
    }

    private fun setupValidateButton() {
        // Jika ada salah satu tiket belum validasi, status jadi "Belum Validasi"
        val adaBelumValidasi = transaksiData.tiketDetails.any { it.waktu_validasi == null }
        b.tvStatus.text = if (adaBelumValidasi) "Validasi CekIn" else "Validasi CekOut"

        b.btnValidasi.setOnClickListener {
            val ticketIds = transaksiData.tiketDetails
                .map { it.idDtTransaksi }

            showConfirmationDialog(ticketIds)
        }
    }

    private fun showConfirmationDialog(ticketIds: List<Int>) {
        val selectedTicketDetails = transaksiData.tiketDetails.filter { ticketIds.contains(it.idDtTransaksi) }
        val message = buildString {
            append("Validasi semua tiket berikut?\n\n")
            selectedTicketDetails.forEachIndexed { index, ticket ->
                append("${index + 1}. ${ticket.namaTiket}")
                if (ticket.jenisTiket.lowercase() == "parkir" && !ticket.noKendaraan.isNullOrEmpty()) {
                    append(" - ${ticket.noKendaraan}")
                }
                if (index < selectedTicketDetails.size - 1) append("\n")
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Validasi")
            .setMessage(message)
            .setPositiveButton("Validasi") { _, _ ->
                validateTickets(ticketIds)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun validateTickets(ticketIds: List<Int>) {
        b.progressBar?.visibility = View.VISIBLE

        // Use only the filtered IDs of non-validated tickets
        val request = TiketValidationRequest(transaksiData.orderId, ticketIds)

        ApiClient.ApiServiceAuth(this, sessionManager.getToken())
            .validateTickets(request)
            .enqueue(object : Callback<TiketValidationResponse> {
                override fun onResponse(
                    call: Call<TiketValidationResponse>,
                    response: Response<TiketValidationResponse>
                ) {
                    b.progressBar?.visibility = View.GONE

                    if (response.isSuccessful) {
                        response.body()?.let { validationResponse ->
                            if (validationResponse.status) {
                                showSuccessDialog(validationResponse.message)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@ValidasiTiketActivity,
                                    validationResponse.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@ValidasiTiketActivity,
                            "Gagal melakukan validasi: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<TiketValidationResponse>, t: Throwable) {
                    b.progressBar?.visibility = View.GONE
                    Toast.makeText(
                        this@ValidasiTiketActivity,
                        "Error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Validasi Berhasil")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
