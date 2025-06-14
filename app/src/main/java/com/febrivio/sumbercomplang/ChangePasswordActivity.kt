package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.febrivio.sumbercomplang.databinding.ActivityChangepasswordBinding
import com.febrivio.sumbercomplang.model.ChangePasswordRequest
import com.febrivio.sumbercomplang.model.ChangePasswordResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity: AppCompatActivity() {
    lateinit var b : ActivityChangepasswordBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChangepasswordBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Inisialisasi session
        session = SessionManager(this)

        b.btnCancel.setOnClickListener {
            finish()
        }

        b.btnChangePassword.setOnClickListener {
            changePassword()
        }

    }    private fun changePassword() {
        try {
            // Get input values
            val currentPassword = b.etOldPassword.text.toString().trim()
            val newPassword = b.etNewPassword.text.toString().trim()
            val confirmPassword = b.etConfirmPassword.text.toString().trim()

            Log.d("ChangePassword", "Current: $currentPassword, New: $newPassword, Confirm: $confirmPassword")

            // Validate inputs
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Password lama harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Password baru harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Konfirmasi password harus diisi", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password baru minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password tidak sesuai", Toast.LENGTH_SHORT).show()
                return
            }

            if (currentPassword == newPassword) {
                Toast.makeText(this, "Password baru tidak boleh sama dengan password lama", Toast.LENGTH_SHORT).show()
                return
            }

            // Show loading
            b.progressBar.visibility = View.VISIBLE
            b.btnChangePassword.isEnabled = false
            b.btnCancel.isEnabled = false

            // Create request
            val changePasswordRequest = ChangePasswordRequest(
                current_password = currentPassword,
                new_password = newPassword,
                confirm_password = confirmPassword
            )

            // Get token and make API call
            val token = session.getToken()
            Log.d("ChangePassword", "Token: $token")
            
            if (token == null) {
                Toast.makeText(this, "Token tidak ditemukan, silakan login ulang", Toast.LENGTH_SHORT).show()
                b.progressBar.visibility = View.GONE
                b.btnChangePassword.isEnabled = true
                b.btnCancel.isEnabled = true
                return
            }

            val apiService = ApiClient.ApiServiceAuth(this, token)

            apiService.changePassword(changePasswordRequest).enqueue(object : Callback<ChangePasswordResponse> {
                override fun onResponse(call: Call<ChangePasswordResponse>, response: Response<ChangePasswordResponse>) {
                    try {
                        // Hide loading
                        b.progressBar.visibility = View.GONE
                        b.btnChangePassword.isEnabled = true
                        b.btnCancel.isEnabled = true

                        Log.d("ChangePassword", "Response code: ${response.code()}")
                        Log.d("ChangePassword", "Response body: ${response.body()}")

                        if (response.isSuccessful) {
                            val res = response.body()
                            when (res?.code) {
                                200 -> {
                                    // Success
                                    Toast.makeText(this@ChangePasswordActivity, res.message, Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                401 -> {
                                    // Password lama salah
                                    Toast.makeText(this@ChangePasswordActivity, res.message, Toast.LENGTH_SHORT).show()
                                }
                                422 -> {
                                    // Validation errors
                                    val errors = res.errors
                                    val errorMessage = StringBuilder()
                                    errors?.forEach { (field, messages) ->
                                        errorMessage.append("${messages.joinToString(", ")}\n")
                                    }
                                    Toast.makeText(this@ChangePasswordActivity, errorMessage.toString(), Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(this@ChangePasswordActivity, res?.message ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this@ChangePasswordActivity, "Gagal mengubah password: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ChangePassword", "Error in onResponse: ${e.message}", e)
                        b.progressBar.visibility = View.GONE
                        b.btnChangePassword.isEnabled = true
                        b.btnCancel.isEnabled = true
                        Toast.makeText(this@ChangePasswordActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                    try {
                        // Hide loading
                        b.progressBar.visibility = View.GONE
                        b.btnChangePassword.isEnabled = true
                        b.btnCancel.isEnabled = true

                        Log.e("ChangePassword", "API call failed: ${t.message}", t)
                        Toast.makeText(this@ChangePasswordActivity, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("ChangePassword", "Error in onFailure: ${e.message}", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ChangePassword", "Error in changePassword: ${e.message}", e)
            Toast.makeText(this, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            // Reset UI state
            b.progressBar.visibility = View.GONE
            b.btnChangePassword.isEnabled = true
            b.btnCancel.isEnabled = true
        }
    }


}