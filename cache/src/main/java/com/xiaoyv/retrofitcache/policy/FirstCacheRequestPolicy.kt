package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCache
import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.*
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

        var cacheSuccess = false

        queryCallback(request)?.let {
            val call = it.first
            val callback = it.second
            if (cacheResponse != null) {
                cacheSuccess = true
                callback.onResponse(call, cacheResponse)
                RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，发现可适用缓存内容提前返回，剩余有效时间：$surplusMill ms")
            } else {
                RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，未发现可适用缓存内容")
            }
        }

        try {
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，继续请求网络数据")
            return chain.proceed(request)
        } catch (e: Exception) {
            if (cacheSuccess && cacheResponse != null) {
                RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，继续请求网络数据失败，且有缓存则再次回调缓存")
                return cacheResponse
            }
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，继续请求网络数据失败，且无缓存，抛出异常")
            throw e
        }
    }

    private fun queryCallback(request: Request): Pair<Call, Callback>? {
        try {
            val dispatcher = RetrofitCache.instance.okHttpClient.dispatcher

            // 反射：runningAsyncCalls
            val runningAsyncCallsField =
                dispatcher.javaClass.getDeclaredField("runningAsyncCalls").apply {
                    isAccessible = true
                }

            // 正在执行的异步请求集合
            val runningAsyncCalls =
                runningAsyncCallsField.get(dispatcher) as? ArrayDeque<*> ?: return null

            // 遍历判断
            runningAsyncCalls.toList().forEach { asyncCall ->
                if (asyncCall is RealCall.AsyncCall) {
                    val itemRequest = asyncCall.request
                    val itemCall = asyncCall.call

                    if (request == itemRequest) {
                        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，请求匹配成功，准备查询回调函数")

                        // 反射：responseCallback
                        val responseCallbackField =
                            asyncCall.javaClass.getDeclaredField("responseCallback").apply {
                                isAccessible = true
                            }

                        // 请求回调接口
                        val responseCallback =
                            responseCallbackField.get(asyncCall) as? Callback ?: return@forEach

                        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，请求匹配成功，回调函数查询成功")

                        RetrofitCacheUtils.logI(responseCallback.javaClass.name)

                        return itemCall to responseCallback
                    }
                }
            }
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，请求匹配失败，未查询到回调函数")
        } catch (e: Exception) {
            e.printStackTrace()
            RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，反射查询失败，未查询到回调函数")
        }
        return null
    }
}