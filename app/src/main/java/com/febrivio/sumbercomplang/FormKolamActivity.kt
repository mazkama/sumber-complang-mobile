package com.febrivio.sumbercomplang

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.ActivityFormKolamBinding
import com.febrivio.sumbercomplang.model.Kolam
import com.febrivio.sumbercomplang.model.KolamResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class FormKolamActivity : AppCompatActivity() {

    private lateinit var b: ActivityFormKolamBinding
    private var selectedImage: Uri? = null
    private var isEdit = false
    private var kolamId: Int = -1

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Mendapatkan gambar yang dipilih dari galeri
        if (uri != null) {
            selectedImage = uri
            b.imageKolam.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFormKolamBinding.inflate(layoutInflater)
        setContentView(b.root)

        isEdit = intent.hasExtra("kolam")
        val kolam = intent.getSerializableExtra("kolam") as? Kolam

        if (isEdit && kolam != null) {
            kolamId = kolam.id_kolam
            b.editNama.setText(kolam.nama)
            b.editDeskripsi.setText(kolam.deskripsi)
            b.editKedalaman.setText(kolam.kedalaman?.toString() ?: "")
            b.editLuas.setText(kolam.luas?.toString() ?: "")
            // Tampilkan gambar
            Picasso.get().load(kolam.url_foto).into(b.imageKolam)

            // Tampilkan tombol Hapus jika mode edit
            b.btnHapus.visibility = View.VISIBLE
            b.btnHapus.setOnClickListener {
                AlertDialog.Builder(this).apply {
                    setTitle("Hapus Kolam")
                    setMessage("Yakin ingin menghapus kolam ini?")
                    setPositiveButton("Hapus") { _, _ ->
                        deleteKolam(kolamId)
                    }
                    setNegativeButton("Batal", null)
                }.show()
            }
        } else {
            // Sembunyikan tombol Hapus jika tidak dalam mode edit
            b.btnHapus.visibility = View.GONE
        }


        b.btnPilihFoto.setOnClickListener {
            // Memulai pemilihan gambar menggunakan ActivityResultContracts
            selectImage.launch("image/*")
        }

        b.btnSimpan.setOnClickListener {
            if (isEdit) updateKolam() else createKolam()
        }

        b.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun createKolam() {
        val namaText = b.editNama.text.toString()
        val deskripsiText = b.editDeskripsi.text.toString()
        val kedalamanText = b.editKedalaman.text.toString()
        val luasText = b.editLuas.text.toString()

        // Validasi: minimal satu input harus diisi
        if (namaText.isBlank() && deskripsiText.isBlank() &&
            kedalamanText.isBlank() && luasText.isBlank() && selectedImage == null) {
            Toast.makeText(this, "Minimal satu field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val nama = namaText.toRequest()
        val deskripsi = deskripsiText.toRequest()
        val kedalaman = kedalamanText.toRequest()
        val luas = luasText.toRequest()
        val fotoPart = getImagePart(selectedImage)

        ApiClient.instance.createKolam(nama, deskripsi, kedalaman, luas, fotoPart)
            .enqueue(responseCallback())
    }


//    private fun updateKolam() {
//        val nama = b.editNama.text.toString().toRequest()
//        val deskripsi = b.editDeskripsi.text.toString().toRequest()
//        val kedalaman = b.editKedalaman.text.toString().toRequest()
//        val luas = b.editLuas.text.toString().toRequest()
//        val fotoPart = getImagePart(selectedImage)
//
//        kolamId?.let {
//            ApiClient.instance.updateKolam(it, nama, deskripsi, kedalaman, luas, fotoPart)
//                .enqueue(responseCallback())
//        }
//    }

    private fun updateKolam() {
        val nama = b.editNama.text.toString().toRequest()
        val deskripsi = b.editDeskripsi.text.toString().toRequest()
        val kedalaman = b.editKedalaman.text.toString().toRequest()
        val luas = b.editLuas.text.toString().toRequest()
        val fotoPart = getImagePart(selectedImage)

        if (fotoPart != null) {
            kolamId?.let {
                ApiClient.instance.updateKolamWithImage(it, nama, deskripsi, kedalaman, luas, fotoPart)
                    .enqueue(responseCallback())
            }
        } else {
            kolamId?.let {
                ApiClient.instance.updateKolamWithoutImage(it, nama, deskripsi, kedalaman, luas)
                    .enqueue(responseCallback())
            }
        }
    }


    private fun String.toRequest(): RequestBody {
        return this.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun getImagePart(uri: Uri?): MultipartBody.Part? {
        if (uri == null) {
            Toast.makeText(this, "Tidak ada gambar yang dipilih", Toast.LENGTH_SHORT).show()
            return null
        }
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "upload.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData("url_foto", file.name, requestFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saat memproses gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    private fun responseCallback(): Callback<KolamResponse> {
        return object : Callback<KolamResponse> {
            override fun onResponse(call: Call<KolamResponse>, response: Response<KolamResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@FormKolamActivity, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK) // Menyatakan hasil sukses
                    finish() // Menutup activity dan kembali ke activity sebelumnya
                } else {
                    val errorMessage = response.body()?.message ?: "Gagal menyimpan"
                    Toast.makeText(this@FormKolamActivity, errorMessage, Toast.LENGTH_SHORT).show() // Menampilkan pesan kesalahan yang lebih jelas
                }
            }

            override fun onFailure(call: Call<KolamResponse>, t: Throwable) {
                Toast.makeText(this@FormKolamActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show() // Menangani error jaringan
            }
        }
    }

    private fun deleteKolam(id: Int) {
        ApiClient.instance.deleteKolam(id).enqueue(object : Callback<KolamResponse> {
            override fun onResponse(call: Call<KolamResponse>, response: Response<KolamResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@FormKolamActivity, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@FormKolamActivity, "Gagal menghapus", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<KolamResponse>, t: Throwable) {
                Toast.makeText(this@FormKolamActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
