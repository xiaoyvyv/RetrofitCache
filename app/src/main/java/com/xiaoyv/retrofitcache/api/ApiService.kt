package com.xiaoyv.retrofitcache.api

import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.annotation.CachePolicy
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface ApiService {

    @GET("xinggan/page/{page}/")
    @Headers("User-Agent:test")
    @CachePolicy(
        cacheMode = CacheModel.FIRST_CACHE_THEN_REQUEST,
        cacheTime = 10000000,
    )
    fun getSexy(@Path("page") page: Int? = null): Call<ResponseBody>
}