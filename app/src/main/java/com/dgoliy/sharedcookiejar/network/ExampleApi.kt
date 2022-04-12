package com.dgoliy.sharedcookiejar.network

import android.content.Context
import retrofit2.Call

class ExampleApi private constructor(
    private val service: ExampleService
) {
    companion object {
        lateinit var instance: ExampleApi

        fun init(context: Context) {
            val factory = NetworkFactory.instance
            val client = factory.createOkHttpClient(context)
            val retrofit = factory.createRetrofit(client)
            val service = factory.createExampleService(retrofit)

            instance = ExampleApi(service)
        }
    }

    fun callGet(): Call<String> {
        return service.testGet()
    }

    fun callSet(): Call<String> {
        return service.testSet()
    }
}
