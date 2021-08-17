package com.xiaoyv.retrofitcache

import io.reactivex.Observable

/**
 * RetrofitCacheRx
 *
 * @author why
 * @since 2021/08/14
 **/
object RetrofitCacheRx {


    /**
     * 有缓存先回调缓存，再请求网络数据
     */
    @JvmStatic
    fun <T> Observable<T>.cacheConcatRequest(): Observable<T> {
        val net: Observable<T> = this
        val cache: Observable<T> = queryCache(net)
        return Observable.concat(cache, net)
    }

    @JvmStatic
    fun <T> queryCache(observable: Observable<T>): Observable<T> {
        return observable
    }
}