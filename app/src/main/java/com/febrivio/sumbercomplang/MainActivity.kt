package com.febrivio.sumbercomplang

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.febrivio.sumbercomplang.databinding.ActivityMainBinding
import com.febrivio.sumbercomplang.databinding.FragmentRiwayatTransaksiBinding
import com.febrivio.sumbercomplang.fragment.FragmentProfil
import com.febrivio.sumbercomplang.fragment.dashboard.FragmentDashboardPengunjung
import com.febrivio.sumbercomplang.fragment.dashboard.FragmentDashboardPetugasKolam
import com.febrivio.sumbercomplang.fragment.dashboard.FragmentDashboardPetugasParkir
import com.febrivio.sumbercomplang.fragment.riwayat.FragmentRiwayatTransaksi
import com.febrivio.sumbercomplang.services.SessionManager
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    lateinit var b: ActivityMainBinding
    private lateinit var session: SessionManager

    lateinit var fDashboardPelanggan: FragmentDashboardPengunjung
    lateinit var fBerandaPetugasKolam: FragmentDashboardPetugasKolam
    lateinit var fBerandaPetugasParkir: FragmentDashboardPetugasParkir
    lateinit var fRiwayatTransaksi: FragmentRiwayatTransaksi
    lateinit var fProfil: FragmentProfil
    lateinit var ft : FragmentTransaction
    lateinit var role : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Inisialisasi sessoin
        session = SessionManager(this)
        role = session.getUserRole().toString()

        b.bottomNavigationView.setOnItemSelectedListener(this)

        fDashboardPelanggan = FragmentDashboardPengunjung()
        fBerandaPetugasKolam = FragmentDashboardPetugasKolam()
        fBerandaPetugasParkir = FragmentDashboardPetugasParkir()
        fRiwayatTransaksi = FragmentRiwayatTransaksi()
        fProfil = FragmentProfil()

        b.bottomNavigationView.setSelectedItemId(R.id.nav_dashboard)

        ft = supportFragmentManager.beginTransaction()

        // Tentukan fragment yang akan ditampilkan berdasarkan role
        val selectedFragment = when (role.lowercase()) {
            "pengunjung" -> fDashboardPelanggan
            "petugas_kolam" -> fBerandaPetugasKolam
            "petugas_parkir" -> fBerandaPetugasParkir
            else -> {
                showToast("Role tidak dikenali")
                return
            }
        }

        // Tampilkan fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, selectedFragment)
            .commit()

        // Tampilkan background putih dan buat layout terlihat
        b.frameLayout.setBackgroundColor(Color.WHITE)
        b.frameLayout.visibility = View.VISIBLE

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_dashboard -> {
                // Tentukan fragment yang akan ditampilkan berdasarkan role
                val selectedFragment = when (role.lowercase()) {
                    "pengunjung" -> fDashboardPelanggan
                    "petugas_kolam" -> fBerandaPetugasKolam
                    "petugas_parkir" -> fBerandaPetugasParkir
                    else -> {
                        showToast("Role tidak dikenali")
                        return false
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, selectedFragment)
                    .commit()

                b.frameLayout.setBackgroundColor(Color.WHITE)
                b.frameLayout.visibility = View.VISIBLE
            }
            R.id.nav_history -> {
                // Tentukan fragment yang akan ditampilkan berdasarkan role
                val selectedFragment = when (role.lowercase()) {
                    "pengunjung" -> fRiwayatTransaksi
                    else -> {
                        return false
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, selectedFragment)
                    .commit()

                b.frameLayout.setBackgroundColor(Color.WHITE)
                b.frameLayout.visibility = View.VISIBLE
            }

            R.id.nav_profile -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fProfil)
                    .commit()

                b.frameLayout.setBackgroundColor(Color.WHITE)
                b.frameLayout.visibility = View.VISIBLE
            }
        }
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}