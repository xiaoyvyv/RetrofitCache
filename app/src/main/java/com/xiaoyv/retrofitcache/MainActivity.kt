package com.xiaoyv.retrofitcache

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xiaoyv.retrofitcache.databinding.ActivityMainBinding
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var toastUtil: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initData()
    }

    private fun initView() {
        binding.getSex.setOnClickListener {
            val sexy: Call<ResponseBody> = ApiManager.instance.apiService.getSexy(2)
//            RetrofitCache.instance.okHttpClient.newCall(
//                Request.Builder()
//                    .url("https://www.baidu.com")
//                    .build()
//            ).enqueue(object : okhttp3.Callback {
//
//                override fun onFailure(call: okhttp3.Call, e: IOException) {
//                }
//
//                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
//                    println("success")
//                }
//            })

            sexy.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    println("success")
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    toastUtil?.cancel()
                    toastUtil = Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT)
                    toastUtil?.show()
                }
            })
        }
    }

    private fun initData() {


    }
}