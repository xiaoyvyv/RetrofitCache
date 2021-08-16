package com.xiaoyv.retrofitcache

import android.app.Application

/**
 * App
 *
 * @author why
 * @since 2021/08/14
 **/
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitCache.init(this)
    }
}