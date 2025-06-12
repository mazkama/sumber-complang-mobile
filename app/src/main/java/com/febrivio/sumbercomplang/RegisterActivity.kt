package com.febrivio.sumbercomplang

import com.febrivio.sumbercomplang.databinding.ActivityRegisterBinding
import com.febrivio.sumbercomplang.model.RegisterRequest
import com.febrivio.sumbercomplang.model.RegisterResponse
import com.febrivio.sumbercomplang.network.ApiClient
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    lateinit var b: ActivityRegisterBinding
    private var isLoading = false
        set(value) {
            field = value
            b.btnRegister.visibility = if (value) View.GONE else View.VISIBLE
            b.progressBarRegister.visibility = if (value) View.VISIBLE else View.GONE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvLogin.setOnClickListener {
            finish()
        }

        b.btnRegister.setOnClickListener {
            val username = b.edtUsernameRegister.text.toString().trim()
            val name = b.edtNameRegister.text.toString().trim()
            val no_hp = b.edtPhone.text.toString().trim()
            val password = b.edtPasswordRegister.text.toString().trim()

            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || no_hp.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isLoading = true

            val request = RegisterRequest(username, name, no_hp, password)

            ApiClient.instance.registerUser(request).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val res = response.body()
                        when (res?.code) {
                            201 -> {
                                Toast.makeText(this@RegisterActivity, res.message ?: "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            422 -> {
                                // Tampilkan error validasi
                                val errors = res.errors
                                val errorMessage = StringBuilder()
                                errors?.forEach { (field, messages) ->
                                    errorMessage.append("${messages.joinToString(", ")}\n")
                                }
                                Toast.makeText(this@RegisterActivity, errors.toString(), Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@RegisterActivity, res?.message ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Gagal: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    isLoading = false
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
