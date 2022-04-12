package com.dgoliy.sharedcookiejar.network

import com.dgoliy.sharedcookiejar.Constants
import retrofit2.Call
import retrofit2.http.GET

interface ExampleService {
    @GET(Constants.ENDPOINT_GET)
    fun testGet(): Call<String>

    @GET(Constants.ENDPOINT_SET)
    fun testSet(): Call<String>
}
