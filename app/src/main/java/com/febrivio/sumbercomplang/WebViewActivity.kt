package com.febrivio.sumbercomplang

import android.content.Intent
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webView)

        val url = intent.getStringExtra("url")
        if (!url.isNullOrEmpty()) {
            webView.settings.javaScriptEnabled = true

            webView.webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val loadingUrl = request?.url.toString()

                    when {
                        loadingUrl.contains("payment/success") -> {
                            Toast.makeText(this@WebViewActivity, "Pembayaran Berhasil", Toast.LENGTH_SHORT).show()
                            finish()
                            return true
                        }

                        loadingUrl.contains("payment/failure") -> {
                            Toast.makeText(this@WebViewActivity, "Pembayaran Gagal", Toast.LENGTH_SHORT).show()
                            finish()
                            return true
                        }

                        else -> return false
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null) {
                        when {
                            url.contains("payment/success") -> {
                                Toast.makeText(this@WebViewActivity, "Pembayaran Berhasil", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                            url.contains("payment/failure") -> {
                                Toast.makeText(this@WebViewActivity, "Pembayaran Gagal", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }
            }

            webView.loadUrl(url)
        }
    }
}
