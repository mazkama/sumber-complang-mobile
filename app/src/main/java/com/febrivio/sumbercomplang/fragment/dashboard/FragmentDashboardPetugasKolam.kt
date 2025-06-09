package com.febrivio.sumbercomplang.fragment.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.febrivio.sumbercomplang.KolamActivity
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.ScanTiketActivity
import com.febrivio.sumbercomplang.TiketActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPetugasKolamBinding
import com.febrivio.sumbercomplang.services.SessionManager

class FragmentDashboardPetugasKolam : Fragment() {

    lateinit var b: FragmentBerandaPetugasKolamBinding
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
        b = FragmentBerandaPetugasKolamBinding.inflate(inflater, container, false)
        v = b.root


        session = SessionManager(thisParent)

        b.tvUserName.setText(session.getUserName())
        b.tvUsername.setText("@${session.getUserUsername()}")

        b.cardKolam.setOnClickListener {
            val intent = Intent(thisParent, KolamActivity::class.java)
            startActivity(intent)
        }

        b.cardTiket.setOnClickListener {
            val intent = Intent(thisParent, TiketActivity::class.java)
            intent.putExtra("jenis_tiket", "kolam")
            startActivity(intent)
        }


        b.cardScan.setOnClickListener {
            val intent = Intent(thisParent, ScanTiketActivity::class.java)
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