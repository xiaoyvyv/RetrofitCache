package com.xiaoyv.retrofitcache

import com.google.gson.Gson
import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.api.ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ApiManager
 *
 * @author why
 * @since 2021/08/14
 **/
class ApiManager private constructor() {
    val apiService: ApiService

    init {
        val httpClient = OkHttpClient.Builder()
            .build()

        val cacheClient =
            RetrofitCache.instance.initClient(httpClient, RetrofitCache.GlobalConfig().also {
                it.cacheMode = CacheModel.FIRST_CACHE_THEN_REQUEST
                it.cacheTime = 1000 * 600L
            })

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.mzitu.com/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .client(cacheClient)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    companion object {
        val instance: ApiManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ApiManager()
        }
    }
}