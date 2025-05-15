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
            val name = b.edtNameRegister.text.toString().trim()
            val email = b.edtEmailRegister.text.toString().trim()
            val password = b.edtPasswordRegister.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isLoading = true

            val request = RegisterRequest(name, email, password)

            ApiClient.instance.registerUser(request).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val res = response.body()
                        Toast.makeText(this@RegisterActivity, res?.message ?: "Berhasil", Toast.LENGTH_SHORT).show()
                        finish()
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
