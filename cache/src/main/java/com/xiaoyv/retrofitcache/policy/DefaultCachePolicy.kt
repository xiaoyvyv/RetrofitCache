package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCache
import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.connection.RealCall

/**
 * 默认缓存拦截器，不做处理
 *
 * 使用此策略，则整个框架的逻辑不会发挥作用（若您已经配置了 OkHttp 相关的缓存参数，则会生效 OkHttp 相关的缓存功能）
 *
 * OkHttp 相关的缓存功能的规则是按照 标准的 HTTP 协议头来的
 *
 * 此框架不会处理
 *
 * @author why
 * @since 2021/01/31
 **/
class DefaultCachePolicy : BasePolicy(), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = getHttpLink(request)

        RetrofitCacheUtils.logI("执行策略：${javaClass.simpleName}，开始：$url")

        // 执行OkHttp 默认缓存协议
        return chain.proceed(chain.request())
    }
}