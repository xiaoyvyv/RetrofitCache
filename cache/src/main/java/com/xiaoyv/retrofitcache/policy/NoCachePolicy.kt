package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 禁用缓存的拦截器
 *
 * 会清除响应头和请求头的所有缓存相关配置
 *
 * @author why
 * @since 2021/01/31
 **/
class NoCachePolicy : BasePolicy(), Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = getHttpLink(request)

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，开始：$url")

        // 清除缓存控制
        val newRequest = chain.request().newBuilder()
            .removeHeader("Cache-Control")
            .header("Cache-Control", "no-cache")
            .removeHeader("Pragma")
            .build()

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，清除请求头缓存配置")

        // 清除缓存控制
        val newBuilder = chain.proceed(newRequest).newBuilder()
        newBuilder.removeHeader("Cache-Control")
        newBuilder.header("Cache-Control", "no-cache")
        newBuilder.removeHeader("Pragma")

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，清除响应头缓存配置")

        return newBuilder.build()
    }
}