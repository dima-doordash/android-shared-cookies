package com.dgoliy.sharedcookiejar.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.dgoliy.sharedcookiejar.Constants
import com.dgoliy.sharedcookiejar.cookies.DDCookieManager
import com.dgoliy.sharedcookiejar.cookies.DDCookieStore
import com.google.gson.GsonBuilder
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class NetworkFactory {
    companion object {
        lateinit var instance: NetworkFactory

        fun init() {
            instance = NetworkFactory()
        }
    }

    private val sharedCookieJar = JavaNetCookieJar(DDCookieManager.instance)

    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(sharedCookieJar)
            .addNetworkInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
            .addInterceptor(
                ChuckerInterceptor.Builder(context)
                    .collector(
                        ChuckerCollector(
                            context = context,
                            // Toggles visibility of the push notification
                            showNotification = true,
                            // Allows to customize the retention period of collected data
                            retentionPeriod = RetentionManager.Period.FOREVER
                        )
                    )
                    // Read the whole response body even when the client does not consume the response completely.
                    // This is useful in case of parsing errors or when the response body
                    // is closed before being read like in Retrofit with Void and Unit types.
                    .alwaysReadResponseBody(true)
                    .build()
            )
            .build()
    }

    fun createRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.URL_BASE)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().create()
                )
            )
            .build()
    }

    fun createExampleService(retrofit: Retrofit): ExampleService {
        return retrofit.create(ExampleService::class.java)
    }
}
