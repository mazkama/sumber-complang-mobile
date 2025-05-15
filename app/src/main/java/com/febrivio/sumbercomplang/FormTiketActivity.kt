package com.febrivio.sumbercomplang

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.ActivityFormTiketBinding
import com.febrivio.sumbercomplang.model.TiketRequest
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.utils.CurrencyHelper.formatCurrency
import okhttp3.ResponseBody
import retrofit2.Call

class FormTiketActivity : AppCompatActivity() {

    lateinit var b: ActivityFormTiketBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFormTiketBinding.inflate(layoutInflater)
        setContentView(b.root)


        val id_tiket = intent.getIntExtra("id_tiket", 0)
        val namaTiket = intent.getStringExtra("nama_tiket")
        val harga = intent.getIntExtra("harga", 0)
        val kategori = intent.getStringExtra("kategori")
        val jenis = intent.getStringExtra("jenis")
        val deskripsi = intent.getStringExtra("deskripsi")

        // Set default value ke TextView dan EditText
        b.tvKategoriTiket.text = kategori
        b.tvNamaTiket.text = namaTiket
        b.tvDesTiket.text = deskripsi
        b.tvHargaTiket.text = "Rp ${formatCurrency(harga)}"

        b.editKetegori.setText(kategori)
        b.editNama.setText(namaTiket)
        b.editDeskripsi.setText(deskripsi)
        b.editHarga.setText(harga.toString())

        // TextWatcher untuk editNama -> tvNamaTiket
        b.editNama.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                b.tvNamaTiket.text = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // TextWatcher untuk editKetegori -> tvKategoriTiket
        b.editKetegori.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                b.tvKategoriTiket.text = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // TextWatcher untuk editDeskripsi -> tvDesTiket
        b.editDeskripsi.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                b.tvDesTiket.text = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // TextWatcher untuk editHarga -> tvHargaTiket
        b.editHarga.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hargaText = s.toString().replace("[^\\d]".toRegex(), "")
                val hargaInt = hargaText.toIntOrNull() ?: 0
                b.tvHargaTiket.text = "Rp ${formatCurrency(hargaInt)}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.btnBack.setOnClickListener {
            finish()
        }

        b.btnSimpan.setOnClickListener {
            if (id_tiket != null) {
                val request = TiketRequest(
                    nama_tiket = b.editNama.text.toString(),
                    harga = b.editHarga.text.toString().replace("[^\\d]".toRegex(), "").toIntOrNull() ?: 0,
                    kategori = b.editKetegori.text.toString(),
                    jenis = jenis ?: "",
                    deskripsi = b.editDeskripsi.text.toString()
                )
                updateTiket(id_tiket.toString(), request)
            } else {
                Toast.makeText(this, "ID Tiket tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTiket(id: String, tiket: TiketRequest) {
        ApiClient.instance.updateTiket(id, tiket)
            .enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: retrofit2.Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FormTiketActivity, "Tiket berhasil diupdate", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@FormTiketActivity, "Gagal update tiket: ${response.body()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@FormTiketActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}