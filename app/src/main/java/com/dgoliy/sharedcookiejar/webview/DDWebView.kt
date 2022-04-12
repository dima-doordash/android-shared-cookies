package com.dgoliy.sharedcookiejar.webview

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import com.dgoliy.sharedcookiejar.cookies.DDCookieManager

class DDWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    override fun loadUrl(url: String) {
        DDCookieManager.instance.syncCookiesToWebkit()
        super.loadUrl(url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        DDCookieManager.instance.syncCookiesToWebkit()
        super.loadUrl(url, additionalHttpHeaders)
    }
}
