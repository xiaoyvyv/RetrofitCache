package com.xiaoyv.retrofitcache.intercepter

import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.policy.*
import okhttp3.Interceptor
import okhttp3.Response

/**
 * RetrofitCacheInterceptor
 *
 * @author why
 * @since 2021/08/14
 **/
class RetrofitCacheInterceptor : BasePolicy(), Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        when (getCacheModel(request)) {
            CacheModel.DEFAULT -> {
                return DEFAULT.intercept(chain)
            }
            CacheModel.FIRST_CACHE_THEN_REQUEST -> {
                return FIRST_CACHE_THEN_REQUEST.intercept(chain)
            }
            CacheModel.NO_CACHE -> {
                return NO_CACHE.intercept(chain)
            }
            CacheModel.IF_NONE_CACHE_REQUEST -> {
                return IF_NONE_CACHE_REQUEST.intercept(chain)
            }
            CacheModel.REQUEST_FAILED_READ_CACHE -> {
                return REQUEST_FAILED_READ_CACHE.intercept(chain)
            }
        }
        return chain.proceed(request)
    }


    /**
     * 五种缓存拦截器
     */
    companion object {
        @JvmStatic
        val DEFAULT: DefaultCachePolicy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DefaultCachePolicy()
        }

        @JvmStatic
        val FIRST_CACHE_THEN_REQUEST: FirstCacheRequestPolicy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FirstCacheRequestPolicy()
        }

        @JvmStatic
        val NO_CACHE: NoCachePolicy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NoCachePolicy()
        }

        @JvmStatic
        val IF_NONE_CACHE_REQUEST: NoCacheRequestPolicy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NoCacheRequestPolicy()
        }

        @JvmStatic
        val REQUEST_FAILED_READ_CACHE: RequestFailedCachePolicy by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RequestFailedCachePolicy()
        }
    }
}