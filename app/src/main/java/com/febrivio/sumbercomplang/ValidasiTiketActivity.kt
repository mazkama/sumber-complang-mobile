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
    private val selectedTickets = mutableSetOf<Int>() // Track selected ticket IDs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityValidasiTiketBinding.inflate(layoutInflater)
        setContentView(b.root)

        sessionManager = SessionManager(this)

        // Set back button
        b.btnBack.setOnClickListener { onBackPressed() }

        // Get transaction data from intent
        transaksiData = intent.getSerializableExtra("transaksi") as? TransaksiData
            ?: run {
                Toast.makeText(this, "Data transaksi tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        // Set order ID in header
        b.tvOrderId.text = "ID#${transaksiData.orderId}"

        // Set RecyclerView Layout Manager
        b.rvItemValidasi.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        setupAdapter()

        // Add "Select All" checkbox if needed
        if (b.cbSelectAll != null) {
            setupSelectAllCheckbox()
        }

        // Setup validate button
        setupValidateButton()
    }

    private fun setupAdapter() {
        validasiTiketAdapter = ValidasiTiketAdapter(
            transaksiData.tiketDetails
        ) { ticket, isChecked ->
            if (isChecked) {
                selectedTickets.add(ticket.idDtTransaksi)
            } else {
                selectedTickets.remove(ticket.idDtTransaksi)
            }

            // Update validate button state
            updateValidateButtonState()
        }

        b.rvItemValidasi.adapter = validasiTiketAdapter
    }

    private fun setupSelectAllCheckbox() {
        b.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            validasiTiketAdapter.toggleSelectAll(isChecked)

            if (isChecked) {
                selectedTickets.clear()
                // Only add non-validated tickets
                transaksiData.tiketDetails.forEach { ticket ->
                    if (ticket.waktu_validasi == null) {
                        selectedTickets.add(ticket.idDtTransaksi)
                    }
                }
            } else {
                selectedTickets.clear()
            }

            updateValidateButtonState()
        }
    }

    private fun setupValidateButton() {
        b.btnValidasi.setOnClickListener {
            if (selectedTickets.isEmpty()) {
                Toast.makeText(this, "Pilih tiket terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showConfirmationDialog()
        }

        // Initial button state
        updateValidateButtonState()
    }

    private fun updateValidateButtonState() {
        if (::b.isInitialized && b.btnValidasi != null) {
            // Check if there are any non-validated tickets selected
            val hasValidTicketsSelected = selectedTickets.isNotEmpty() &&
                transaksiData.tiketDetails.any {
                    selectedTickets.contains(it.idDtTransaksi) && it.waktu_validasi == null
                }

            b.btnValidasi.isEnabled = hasValidTicketsSelected
            b.btnValidasi.alpha = if (hasValidTicketsSelected) 1.0f else 0.5f
        }
    }

    private fun showConfirmationDialog() {
        // Filter to only include non-validated tickets
        val selectedTicketDetails = transaksiData.tiketDetails.filter { ticket ->
            selectedTickets.contains(ticket.idDtTransaksi) && ticket.waktu_validasi == null
        }

        if (selectedTicketDetails.isEmpty()) {
            Toast.makeText(this, "Tidak ada tiket yang dapat divalidasi", Toast.LENGTH_SHORT).show()
            return
        }

        val message = buildString {
            append("Validasi tiket berikut?\n\n")

            selectedTicketDetails.forEachIndexed { index, ticket ->
                append("${index + 1}. ${ticket.namaTiket}")

                if (ticket.jenisTiket.lowercase() == "parkir" && !ticket.noKendaraan.isNullOrEmpty()) {
                    append(" - ${ticket.noKendaraan}")
                }

                if (index < selectedTicketDetails.size - 1) {
                    append("\n")
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Validasi")
            .setMessage(message)
            .setPositiveButton("Validasi") { _, _ ->
                validateTickets(selectedTicketDetails.map { it.idDtTransaksi })
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
                            } else {
                                Toast.makeText(
                                    this@ValidasiTiketActivity,
                                    validationResponse.message,
                                    Toast.LENGTH_SHORT
                                ).show()
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
