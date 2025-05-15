package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPetugasKolamBinding

class MainActivity : AppCompatActivity() {

    lateinit var b: FragmentBerandaPetugasKolamBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = FragmentBerandaPetugasKolamBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.cardKolam.setOnClickListener {
            val intent = Intent(this, KolamActivity::class.java)
            startActivity(intent)
        }

        b.cardTiket.setOnClickListener {
            val intent = Intent(this, TiketActivity::class.java)
            startActivity(intent)
        }

        b.cardScan.setOnClickListener {
            val intent = Intent(this, TransaksiTiketActivity::class.java)
            startActivity(intent)
        }
    }
}