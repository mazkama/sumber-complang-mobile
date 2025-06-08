package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.ActivityDetailKolamBinding
import com.febrivio.sumbercomplang.model.Kolam
import com.squareup.picasso.Picasso

class DetailKolamActivity : AppCompatActivity() {

    private lateinit var b: ActivityDetailKolamBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetailKolamBinding.inflate(layoutInflater)
        setContentView(b.root)

        val kolam = intent.getSerializableExtra("kolam") as? Kolam

        b.tvTitle.setText(kolam?.nama)
        b.tvDescription.setText(kolam?.deskripsi)
        b.tvDepth.setText("Luas: ${ kolam?.kedalaman?.toString() } m²" ?: "")
        b.tvWidth.setText("Kedalaman: ${ kolam?.luas?.toString() } m²" ?: "")
        // Tampilkan gambar
        Picasso.get().load(kolam?.url_foto).into(b.ivPoolImage)

        b.btnTiketKolam.setOnClickListener {
            val intent = Intent(this@DetailKolamActivity, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "kolam")
            startActivity(intent)
        }

        b.btnBack.setOnClickListener {
            finish()
        }

    }

}
