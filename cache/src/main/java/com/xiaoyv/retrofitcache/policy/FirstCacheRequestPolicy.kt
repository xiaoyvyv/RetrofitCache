package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCache
import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.connection.RealCall
import java.util.*

/**
 * 读取缓存，缓存存在先返回缓存数据，同时请求网络数据并返回（响应会回调两次）
 *
 * @author why
 * @since 2021/01/31
 **/
class FirstCacheRequestPolicy : BasePolicy(), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = getHttpLink(request)

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，开始：$url")

        // 查询是否存在缓存
        val prepareCacheResponse = prepareCacheResponse(request)
        val cacheResponse = prepareCacheResponse.first
        val surplusMill = prepareCacheResponse.second

        if (cacheResponse != null) {
            val pairBack = queryCallback()
            val call = pairBack.first
            val queryCallback = pairBack.second
            call?.let {
                queryCallback?.onResponse(it, cacheResponse)
            }

            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，发现可适用缓存内容，剩余有效时间：$surplusMill ms")
            return cacheResponse
        }
        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，未发现缓存，开始请求网络资源")
        return chain.proceed(request)
    }

    private fun queryCallback(): Pair<Call?, Callback?> {
        return try {
            val dispatcher = RetrofitCache.instance.okHttpClient.dispatcher
            val field = dispatcher.javaClass.getDeclaredField("runningAsyncCalls")
            field.isAccessible = true
            val asyncCallField = field.get(dispatcher) as ArrayDeque<*>
            val asyncCall = asyncCallField.toList()[0]
            val responseCallbackField = asyncCall.javaClass.getDeclaredField("responseCallback")
            responseCallbackField.isAccessible = true
            val callback = responseCallbackField.get(asyncCall) as? Callback
            return dispatcher.runningCalls()[0] to callback
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
}