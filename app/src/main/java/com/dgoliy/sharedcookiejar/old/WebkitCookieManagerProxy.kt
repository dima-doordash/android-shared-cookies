package com.dgoliy.sharedcookiejar.old

import java.io.IOException
import java.lang.UnsupportedOperationException
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.URI
import java.util.Arrays
import java.util.HashMap
import kotlin.jvm.JvmOverloads
import kotlin.Throws

internal class WebkitCookieManagerProxy @JvmOverloads constructor(
    store: CookieStore? = null,
    cookiePolicy: CookiePolicy? = null
) : java.net.CookieManager(null, cookiePolicy) {
    companion object {
        private const val TAG = ""
    }

    private val webkitCookieManager: android.webkit.CookieManager = android.webkit.CookieManager.getInstance()

    init {
        webkitCookieManager.setAcceptCookie(true)
    }

    @Throws(IOException::class)
    override fun put(uri: URI?, responseHeaders: Map<String, List<String>>?) {
        super.put(uri, responseHeaders)
        // make sure our args are valid
        if (uri == null || responseHeaders == null) return

        // save our url once
        val url = uri.toString()

        // go over the headers
        for (headerKey in responseHeaders.keys) {
            // ignore headers which aren't cookie related
            if (!(headerKey.equals(
                    "Set-Cookie2",
                    ignoreCase = true
                ) || headerKey.equals("Set-Cookie", ignoreCase = true))
            ) continue

            // process each of the headers
            for (headerValue in responseHeaders[headerKey]!!) {
                webkitCookieManager.setCookie(url, headerValue)
            }
        }
    }

    @Throws(IOException::class)
    override fun get(
        uri: URI?,
        requestHeaders: Map<String, List<String>>?
    ): Map<String, List<String>> {
        // make sure our args are valid
        require(!(uri == null || requestHeaders == null)) { "Argument is null" }

        // save our url once
        val url = uri.toString()

        // prepare our response
        val res: MutableMap<String, List<String>> = HashMap()

        // get the cookie
        val cookie = webkitCookieManager.getCookie(url)

        // return it
        if (cookie != null) res["Cookie"] = Arrays.asList(cookie)
        return res
    }

    override fun getCookieStore(): CookieStore {
        // we don't want anyone to work with this cookie store directly
        throw UnsupportedOperationException()
    }

}
