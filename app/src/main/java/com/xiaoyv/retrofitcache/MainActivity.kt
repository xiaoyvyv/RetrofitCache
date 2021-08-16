package com.xiaoyv.retrofitcache

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xiaoyv.retrofitcache.databinding.ActivityMainBinding
import com.xiaoyv.retrofitcache.db.entity.CacheEntity
import io.reactivex.Observable
import io.reactivex.ObservableConverter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            val sexy: Call<CacheEntity> = ApiManager.instance.apiService.getSexy(2)
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

            sexy.enqueue(object : Callback<CacheEntity> {
                override fun onResponse(
                    call: Call<CacheEntity>,
                    response: Response<CacheEntity>
                ) {
                    println("RetrofitCache:success")
                }

                override fun onFailure(call: Call<CacheEntity>, t: Throwable) {
                    toastUtil?.cancel()
                    toastUtil = Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT)
                    toastUtil?.show()
                }
            })
        }


        binding.getSexOb.setOnClickListener {
            val subscribe = ApiManager.instance.apiService.getSexyObservable(2)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(object : ObservableConverter<ResponseBody, Observable<ResponseBody>> {
                    override fun apply(upstream: Observable<ResponseBody>): Observable<ResponseBody> {


                        return upstream
                    }
                })
                .subscribe({
                    println("RetrofitCache:success")
                }, {
                    println("RetrofitCache:error")
                })
        }
    }

    private fun initData() {


    }
}