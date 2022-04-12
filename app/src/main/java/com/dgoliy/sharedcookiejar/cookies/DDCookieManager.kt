package com.dgoliy.sharedcookiejar.cookies

import android.os.Build
import com.company.android.logging.DDLog
import java.net.CookieHandler
import java.net.CookiePolicy
import java.net.CookieStore
import java.net.HttpCookie
import java.util.concurrent.atomic.AtomicReference

class DDCookieManager private constructor(
    private val store: CookieStore
) : java.net.CookieManager(store, CookiePolicy.ACCEPT_ALL) {
    companion object {
        private const val TAG = "DDCookieManager"

        private val _instance = AtomicReference<DDCookieManager>()
        val instance: DDCookieManager
            get() = _instance.get()

        fun init(store: CookieStore) {
            val manager = DDCookieManager(store)
            CookieHandler.setDefault(manager)
            _instance.set(manager)
        }
    }

    private val webkitCookieManager: android.webkit.CookieManager =
        android.webkit.CookieManager.getInstance()

    init {

        webkitCookieManager.setAcceptCookie(true)
    }

    // private inline fun handleExceptions(delegate: () -> Unit) {
    //     try {
    //         delegate()
    //     } catch (t: Throwable) {
    //         reportErrorOrThrow(t)
    //     }
    // }

    fun syncCookiesToWebkit() {
        DDLog.d(TAG, "syncCookiesToWebkit for ${store.cookies.size} cookies")
        store.cookies.forEach {
            val protocol = if (it.secure) {
                "https"
            } else {
                "http"
            }
            val hostUrl = "$protocol://${it.adjustedDomain}${it.path}"
            val cookieString = httpCookieToHeaderString(it)
            webkitCookieManager.setCookie(hostUrl, cookieString)
        }

        webkitCookieManager.flush()
    }

    private fun httpCookieToHeaderString(cookie: HttpCookie): String {
        val maxAge = if (cookie.maxAge != -1L) {
            "; max_age=${cookie.maxAge}"
        } else {
            ""
        }
        val path = if (cookie.path != null) {
            "; path=${cookie.path}"
        } else {
            ""
        }
        val domain = if (cookie.domain != null) {
            "; domain=${cookie.adjustedDomain}"
        } else {
            ""
        }
        val secure = if (cookie.secure) {
            "; secure"
        } else {
            ""
        }
        val httpOnly = if (Build.VERSION.SDK_INT >= 24 && cookie.isHttpOnly) {
            "; httponly"
        } else {
            ""
        }

        return "${cookie.name}=${cookie.value}$maxAge$path$domain$secure$httpOnly"
    }

    private val HttpCookie.adjustedDomain: String
        get() {
            return if (domain.startsWith('.')) {
                domain.substring(1)
            } else {
                domain
            }
        }
}
