package com.febrivio.sumbercomplang

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()
    private var hasFetchedTransaction = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra("url")
        val orderId = intent.getStringExtra("order_id")

        if (url != null && orderId != null) {
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true

            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d("WEBVIEW_START", "Loading: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d("WEBVIEW_FINISH", "Finished: $url")

                    // If URL contains finish and no transaction fetched yet, fetch transaction status
                    if (!hasFetchedTransaction && url != null && url.contains("payment/finish")) {
                        hasFetchedTransaction = true
                        fetchFinalTransaksi(orderId)
                    }
                }
            }

            webView.loadUrl(url)
        } else {
            Toast.makeText(this, "Data URL atau Order ID tidak lengkap", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchFinalTransaksi(orderId: String) {
        val apiUrl = "https://sumber-complang.mazkama.web.id/api/final-transaksi/$orderId"

        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WebViewActivity, "Gagal mengambil data transaksi", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        val json = JSONObject(responseData ?: "{}")

                        val isSuccess = json.optBoolean("success")
                        val status = json.optJSONObject("data")?.optString("status")

                        // Check if the transaction is successful and settled
                        if (isSuccess && status == "settlement") {
                            val intent = Intent(this@WebViewActivity, DetailTransaksiActivity::class.java)
                            intent.putExtra("transaksi_data", json.getJSONObject("data").toString())
                            startActivity(intent)
                            finish()
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@WebViewActivity, "Transaksi belum selesai atau gagal", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@WebViewActivity, "Terjadi kesalahan server", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        })
    }
}
