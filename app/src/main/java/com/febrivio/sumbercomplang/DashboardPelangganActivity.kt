package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPengunjungBinding
import com.febrivio.sumbercomplang.services.SessionManager

class DashboardPelangganActivity: AppCompatActivity() {

    lateinit var b: FragmentBerandaPengunjungBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = FragmentBerandaPengunjungBinding.inflate(layoutInflater)
        setContentView(b.root)

        session = SessionManager(this)

        b.tvUserName.setText(session.getUserName())
        b.tvUserEmail.setText(session.getUserEmail())

        b.cardTiketKolam.setOnClickListener {
            val intent = Intent(this, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "kolam")
            startActivity(intent)
        }

        b.cardParkir.setOnClickListener {
            val intent = Intent(this, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "parkir")
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