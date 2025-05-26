package com.febrivio.sumbercomplang

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.febrivio.sumbercomplang.databinding.LoginActivityBinding
import com.febrivio.sumbercomplang.model.LoginRequest
import com.febrivio.sumbercomplang.model.LoginResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.jvm.java

class LoginActivity: AppCompatActivity() {
    lateinit var b : LoginActivityBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = LoginActivityBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvRegister.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        session = SessionManager(this)

        if(session.isLoggedIn()){
            handleSession()
        }

        b.btnLogin.setOnClickListener {
            handleLogin()
        }

    }

    fun handleSession(){

        when (session.getUserRole()) {
            "petugas_kolam" -> {
                val intent = Intent(this@LoginActivity, DashboardPetugasKolamActvity::class.java)
                startActivity(intent)
                finish()
            }
            "petugas_parkir" -> {
                val intent = Intent(this@LoginActivity, DashboardPetugasParkirActivity::class.java)
                startActivity(intent)
                finish()
            }
            "pengunjung" -> {
                val intent = Intent(this@LoginActivity, DashboardPelangganActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> {
                Toast.makeText(this@LoginActivity, "Role tidak dikenali. Akses ditolak.", Toast.LENGTH_SHORT).show()
                // Tidak ada startActivity
            }
        }
    }

    fun handleLogin() {
        val email = b.edtEmailLogin.text.toString().trim()
        val password = b.edtPasswordLogin.text.toString()

        if (!isValidInput(email, password)) return

        val request = LoginRequest(email, password)

        // Nonaktifkan tombol login saat proses berlangsung
        b.btnLogin.isEnabled = false
        b.btnLogin.text = "Loading..."

        ApiClient.instance.loginUser(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                // Aktifkan kembali tombol setelah respon diterima
                b.btnLogin.isEnabled = true
                b.btnLogin.text = "Login"

                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.success == true) {
                        session.saveLogin(
                            res.token,
                            res.user.id_user,
                            res.user.name,
                            res.user.email,
                            res.user.role
                        )

                        Toast.makeText(this@LoginActivity, "Login sukses!", Toast.LENGTH_SHORT).show()

                        when (res.user.role.lowercase()) {
                            "petugas_kolam" -> {
                                val intent = Intent(this@LoginActivity, DashboardPetugasKolamActvity::class.java)
                                startActivity(intent)
                            }
                            "petugas_parkir" -> {
                                val intent = Intent(this@LoginActivity, DashboardPetugasParkirActivity::class.java)
                                startActivity(intent)
                            }
                            "pengunjung" -> {
                                val intent = Intent(this@LoginActivity, DashboardPelangganActivity::class.java)
                                startActivity(intent)
                            }
                            else -> {
                                Toast.makeText(this@LoginActivity, "Role tidak dikenali. Akses ditolak.", Toast.LENGTH_SHORT).show()
                                // Tidak ada startActivity
                            }
                        }


                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, res?.message ?: "Gagal login", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    when (response.code()) {
                        401 -> {
                            Toast.makeText(this@LoginActivity, "Email atau password salah!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this@LoginActivity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }


            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Aktifkan kembali tombol jika error jaringan
                b.btnLogin.isEnabled = true
                b.btnLogin.text = "Login"
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isValidInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            b.edtEmailLogin.error = "Email tidak boleh kosong"
            b.edtEmailLogin.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.edtEmailLogin.error = "Format email tidak valid"
            b.edtEmailLogin.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            b.edtPasswordLogin.error = "Password tidak boleh kosong"
            b.edtPasswordLogin.requestFocus()
            return false
        }

        if (password.length < 6) {
            b.edtPasswordLogin.error = "Password minimal 6 karakter"
            b.edtPasswordLogin.requestFocus()
            return false
        }

        return true
    }


}