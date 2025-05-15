package com.febrivio.sumbercomplang.network

import android.content.Context
import android.content.Intent
import com.febrivio.sumbercomplang.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val context: Context,
    private val token: String? // Dapatkan token dari SessionManager atau sumber lain
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Menambahkan header Authorization jika token ada
        val requestWithToken = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")  // Menambahkan header Authorization
                .addHeader("Accept", "application/json")  // Menambahkan header Accept
                .build()
        } else {
            originalRequest  // Tidak menambahkan header jika token tidak ada
        }

        val response = chain.proceed(requestWithToken)

        // Jika status code 401 (Unauthorized), arahkan ke LoginActivity
        if (response.code == 401) {
            // Token tidak valid atau sudah kedaluwarsa -> redirect ke LoginActivity
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }

        return response
    }
}