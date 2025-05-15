package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPetugasParkirBinding
import com.febrivio.sumbercomplang.services.SessionManager

class DashboardPetugasParkirActivity : AppCompatActivity() {

    lateinit var b: FragmentBerandaPetugasParkirBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = FragmentBerandaPetugasParkirBinding.inflate(layoutInflater)
        setContentView(b.root)

        session = SessionManager(this)

        b.tvUserName.setText(session.getUserName())
        b.tvUserEmail.setText(session.getUserEmail())

        b.cardTiket.setOnClickListener {
            val intent = Intent(this, TiketActivity::class.java)
            intent.putExtra("jenis_tiket", "parkir")
            startActivity(intent)
        }

        b.cardScan.setOnClickListener {
            val intent = Intent(this, TransaksiTiketActivity::class.java)
            startActivity(intent)
        }

        b.cardPengaturan.setOnClickListener {
            session.logout()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}