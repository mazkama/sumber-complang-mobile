package com.febrivio.sumbercomplang.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://sumber-complang.mazkama.web.id/api/"

    val instance: ApiService by lazy {
        // Membuat client OkHttp
        val client = OkHttpClient.Builder()
            .build()  // Anda bisa menambahkan interceptors, timeouts, dll. jika diperlukan.

        // Membuat Retrofit instance
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun ApiServiceAuth(context: Context, token: String?): ApiService {
        // Menambahkan AuthInterceptor dengan token
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context, token))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

