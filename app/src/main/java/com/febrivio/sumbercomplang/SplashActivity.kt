package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.services.SessionManager

class SplashActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengunci tampilan ke potret
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_splash)


        // Inisialisasi sessoin
        session = SessionManager(this)

        // Tambahkan animasi fade-in pada logo
        val splashImage: ImageView = findViewById(R.id.splash_image)
        splashImage.setImageResource(R.drawable.logo_sumber_complang)
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 2000
            fillAfter = true
        }
        splashImage.startAnimation(fadeIn)

        // Inisialisasi sessoin
        session = SessionManager(this)
        val token = session.getToken()

        Log.d("SplashActivity", "Token: $token") // Debugging

        // Gunakan Handler untuk tetap pindah layar meskipun token null
        Handler(Looper.getMainLooper()).postDelayed({
            if (token != null) {
                redirectToDashboard()
            } else {
                moveToLogin()
            }
        }, 2500) // Tunggu 2.5 detik sebelum berpindah
    }

    private fun redirectToDashboard() {
        val role = session.getUserRole()
        val intent = when (role?.lowercase()) {
            "petugas_kolam", "petugas_parkir","pengunjung"-> Intent(this, MainActivity::class.java)
            else -> {
                showToast("Gagal login, role tidak tersedia")
                return
            }
        }
        startActivityWithAnimation(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun moveToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityWithAnimation(intent)
    }

    private fun startActivityWithAnimation(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}