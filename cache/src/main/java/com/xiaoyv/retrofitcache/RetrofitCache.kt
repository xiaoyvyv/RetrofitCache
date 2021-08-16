package com.xiaoyv.retrofitcache

import android.app.Application
import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.db.CacheDataManager
import com.xiaoyv.retrofitcache.intercepter.RetrofitCacheInterceptor
import com.xiaoyv.retrofitcache.intercepter.RetrofitResponseInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * RetrofitCache
 *
 * 自定义五种缓存策略，支持 GET POST PUT DELETE 等的缓存
 *
 * 若使用该框架的策略模式（DefaultCachePolicy 除外），将会抛弃 HTTP 相关缓存规则，框架内部维护了自己的缓存规则
 *
 * @author why
 * @since 2021/08/14
 **/
class RetrofitCache private constructor() {
    private var client: OkHttpClient? = null

    var globalConfig: GlobalConfig = GlobalConfig()

    val okHttpClient: OkHttpClient
        get() {
            return client ?: throw RuntimeException("请先初始化 RetrofitCache.initClient()")
        }

    /**
     * 返回缓存配置后的 OkHttpClient
     */
    @JvmOverloads
    fun initClient(
        originalClient: OkHttpClient,
        globalConfig: GlobalConfig? = null
    ): OkHttpClient {
        globalConfig?.let {
            this@RetrofitCache.globalConfig = it
        }

        val cacheBuilder = originalClient.newBuilder()
            .addInterceptor(RetrofitResponseInterceptor())
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = HttpLoggingInterceptor.Level.HEADERS
            })
            .addInterceptor(RetrofitCacheInterceptor())

        // 拦截器排序
        cacheBuilder.interceptors().sortBy {
            when (it) {
                is RetrofitResponseInterceptor -> 1
                is RetrofitCacheInterceptor -> 2
                is HttpLoggingInterceptor -> 1000
                else -> 999
            }
        }
        return cacheBuilder.build().also {
            this.client = it
        }
    }

    /**
     * 全局默认配置
     */
    data class GlobalConfig @JvmOverloads constructor(
        /**
         * 全局默认缓存配置
         */
        @CacheModel var cacheMode: Int = CacheModel.DEFAULT,
        /**
         * 全局过期时间
         */
        var cacheTime: Long = CACHE_TIME_NO_EXPIRED
    ) {
        companion object {
            /**
             * 永不过期
             */
            const val CACHE_TIME_NO_EXPIRED: Long = -1

            /**
             * 注解全局配置的占位，运行时若注解未配置值，则将此占位替换为全局配置的值
             */
            const val DEFAULT_FLAG_MODE = -2
            const val DEFAULT_FLAG_TIME = -2L
            const val DEFAULT_FLAG_KEY = "DEFAULT_KEY"
        }
    }

    companion object {
        const val TAG = "RetrofitCache"

        @JvmStatic
        fun init(app: Application) {
            CacheDataManager.instance.init(app)
        }

        @JvmStatic
        val instance: RetrofitCache by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RetrofitCache()
        }
    }
}