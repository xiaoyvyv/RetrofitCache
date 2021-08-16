package com.xiaoyv.retrofitcache.annotation

import com.xiaoyv.retrofitcache.RetrofitCache

/**
 * CachePolicy
 *
 * @author why
 * @since 2021/01/31
 **/
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CachePolicy(
    /**
     * 缓存模式
     */
    @CacheModel val cacheMode: Int = RetrofitCache.GlobalConfig.DEFAULT_FLAG_MODE,
    /**
     * 缓存的 Key
     */
    val cacheKey: String = RetrofitCache.GlobalConfig.DEFAULT_FLAG_KEY,
    /**
     * 缓存时间
     */
    val cacheTime: Long = RetrofitCache.GlobalConfig.DEFAULT_FLAG_TIME
)

