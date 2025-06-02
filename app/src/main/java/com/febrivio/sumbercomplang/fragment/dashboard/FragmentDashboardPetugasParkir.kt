package com.febrivio.sumbercomplang.fragment.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.TiketActivity
import com.febrivio.sumbercomplang.TransaksiTiketActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPetugasParkirBinding
import com.febrivio.sumbercomplang.services.SessionManager

class FragmentDashboardPetugasParkir :  Fragment() {

    lateinit var b: FragmentBerandaPetugasParkirBinding
    private lateinit var thisParent: MainActivity
    private lateinit var v: View

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize binding and view
        thisParent = activity as MainActivity
        b = FragmentBerandaPetugasParkirBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        b.tvUserName.setText(session.getUserName())
        b.tvUserEmail.setText(session.getUserEmail())

        b.cardTiket.setOnClickListener {
            val intent = Intent(thisParent, TiketActivity::class.java)
            intent.putExtra("jenis_tiket", "parkir")
            startActivity(intent)
        }

        b.cardScan.setOnClickListener {
            val intent = Intent(thisParent, TransaksiTiketActivity::class.java)
            startActivity(intent)
        }

        b.cardPengaturan.setOnClickListener {
            session.logout()
            val intent = Intent(thisParent, LoginActivity::class.java)
            startActivity(intent)
            thisParent.finish()
        }

        return v
    }
}