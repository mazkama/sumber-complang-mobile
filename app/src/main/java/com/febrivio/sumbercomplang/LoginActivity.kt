package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        // Inisialisasi sessoin
        session = SessionManager(this)

        b.tvRegister.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        b.btnLogin.setOnClickListener {
            handleLogin()
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

                        val role = res.user.role.lowercase()
                        val intent = when (role) {
                            "petugas_kolam", "petugas_parkir", "pengunjung" -> Intent(this@LoginActivity, MainActivity::class.java)
                            else -> {
                                showToast("Gagal login, role tidak tersedia")
                                return
                            }
                        }
                        startActivityWithAnimation(intent)



                        startActivityWithAnimation(intent)
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

    private fun startActivityWithAnimation(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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