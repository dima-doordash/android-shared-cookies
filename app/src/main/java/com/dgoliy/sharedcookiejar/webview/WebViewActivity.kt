package com.dgoliy.sharedcookiejar.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chuckerteam.chucker.api.Chucker
import com.dgoliy.sharedcookiejar.Constants
import com.dgoliy.sharedcookiejar.databinding.ActivityWebviewBinding

class WebViewActivity : AppCompatActivity() {
    companion object {
        private const val BUNDLE_KEY_URL = "key_url"
        fun getLaunchIntent(context: Context, url: String): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(BUNDLE_KEY_URL, url)
            return intent
        }
    }

    private lateinit var binding: ActivityWebviewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(BUNDLE_KEY_URL) ?: ""
        binding.webView.webViewClient = WebViewClient()
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.loadUrl(url)

        configureButtons()
    }

    private fun configureButtons() {
        binding.btnCheckCookies.setOnClickListener {
            val manager = CookieManager.getInstance()
            val cookie = manager.getCookie(Constants.URL_WEBVIEW) ?: "null"
            Toast.makeText(this, "cookies: $cookie", Toast.LENGTH_SHORT).show()
            Log.d("Dima", cookie)
        }
    }
}
