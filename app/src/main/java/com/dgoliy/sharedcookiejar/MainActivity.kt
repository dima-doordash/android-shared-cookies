package com.dgoliy.sharedcookiejar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chuckerteam.chucker.api.Chucker
import com.dgoliy.sharedcookiejar.databinding.ActivityMainBinding
import com.dgoliy.sharedcookiejar.network.ExampleApi
import com.dgoliy.sharedcookiejar.webview.WebViewActivity
import retrofit2.Call
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureButtons()
    }

    private fun configureButtons() {
        binding.openWebview.setOnClickListener {
            startActivity(WebViewActivity.getLaunchIntent(this, Constants.URL_WEBVIEW))
        }
        binding.doGetRequest.setOnClickListener {
            callGet()
        }
        binding.doSetRequest.setOnClickListener {
            callSet()
        }
        binding.launchDirectly.setOnClickListener {
            startActivity(Chucker.getLaunchIntent(this))
        }
    }

    private fun callGet() {
        ExampleApi.instance.callGet().enqueue(
            object : retrofit2.Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    Toast.makeText(
                        this@MainActivity,
                        "Get: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "Get ERROR: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        )
    }

    private fun callSet() {
        ExampleApi.instance.callSet().enqueue(
            object : retrofit2.Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    Toast.makeText(
                        this@MainActivity,
                        "Set: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "Set ERROR: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        )
    }
}
