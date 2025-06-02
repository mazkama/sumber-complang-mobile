package com.febrivio.sumbercomplang.fragment.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.febrivio.sumbercomplang.LoginActivity
import com.febrivio.sumbercomplang.MainActivity
import com.febrivio.sumbercomplang.TransaksiTiketActivity
import com.febrivio.sumbercomplang.databinding.FragmentBerandaPengunjungBinding
import com.febrivio.sumbercomplang.services.SessionManager

class FragmentDashboardPengunjung : Fragment() {

    lateinit var b: FragmentBerandaPengunjungBinding
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
        b = FragmentBerandaPengunjungBinding.inflate(inflater, container, false)
        v = b.root

        session = SessionManager(thisParent)

        b.tvUserName.setText(session.getUserName())
        b.tvUserEmail.setText(session.getUserEmail())

        b.cardTiketKolam.setOnClickListener {
            val intent = Intent(thisParent, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "kolam")
            startActivity(intent)
        }

        b.cardParkir.setOnClickListener {
            val intent = Intent(thisParent, TransaksiTiketActivity::class.java)
            intent.putExtra("jenis_tiket", "parkir")
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