package com.dgoliy.sharedcookiejar

import android.app.Application
import android.webkit.CookieManager
import android.webkit.ValueCallback
import com.dgoliy.sharedcookiejar.cookies.DDCookieManager
import com.dgoliy.sharedcookiejar.cookies.DDCookieStore
import com.dgoliy.sharedcookiejar.network.ExampleApi
import com.dgoliy.sharedcookiejar.network.NetworkFactory

class SharedCookieJarApp: Application() {
    override fun onCreate() {
        super.onCreate()

        CookieManager.getInstance().removeAllCookies { true }
        DDCookieManager.init(
            DDCookieStore()
        )
        NetworkFactory.init()
        ExampleApi.init(this)
    }
}
