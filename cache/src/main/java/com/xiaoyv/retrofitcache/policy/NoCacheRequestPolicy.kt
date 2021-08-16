package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 读取缓存，缓存存在则直接返回，缓存不存在则请求网络再返回
 *
 * @author why
 * @since 2021/01/31
 **/
class NoCacheRequestPolicy : BasePolicy(), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = getHttpLink(request)

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，开始：$url")

        // 查询是否存在缓存
        val prepareCacheResponse = prepareCacheResponse(request)
        val cacheResponse = prepareCacheResponse.first
        val surplusMill = prepareCacheResponse.second

        if (cacheResponse != null) {
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，发现可适用缓存内容，剩余有效时间：$surplusMill ms")
            return cacheResponse
        }
        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，未发现缓存，开始请求网络资源")
        return chain.proceed(request)
    }
}