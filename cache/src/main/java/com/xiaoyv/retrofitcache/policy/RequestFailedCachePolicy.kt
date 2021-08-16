package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 网络请求失败则读取缓存返回
 *
 * @author why
 * @since 2021/01/31
 **/
class RequestFailedCachePolicy : BasePolicy(), Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = getHttpLink(request)

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，开始：$url")

        try {
            val response = chain.proceed(request)
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，网络响应成功")
            return response
        } catch (e: Exception) {
            // 读取缓存
            val cachePair: Pair<Response?, Long> = prepareCacheResponse(request)
            val cacheResponse = cachePair.first
            val surplusMill = cachePair.second
            if (cacheResponse != null) {
                RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，网络响应失败，发现可适用缓存内容，剩余有效时间：$surplusMill ms")
                return cacheResponse
            }
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，网络响应失败，且未发现缓存，抛出异常")
            throw e
        }
    }
}