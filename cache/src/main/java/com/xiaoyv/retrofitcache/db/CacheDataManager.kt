package com.xiaoyv.retrofitcache.db

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import com.xiaoyv.retrofitcache.db.entity.CacheEntity

/**
 * CacheDataManager
 *
 * @author why
 * @since 2021/08/15
 **/
class CacheDataManager private constructor() {

    private var database: CacheDataBase? = null

    internal fun init(application: Application) {
        database = Room.databaseBuilder(
            application,
            CacheDataBase::class.java,
            CacheDataBase.CACHE_DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    private val cache: CacheDataBase
        get() {
            return database ?: throw RuntimeException("CacheDataManager is not init!")
        }

    @WorkerThread
    fun queryByCacheKey(cacheKey: String): CacheEntity? {
        return cache.cacheDao().queryCacheByKey(cacheKey)
    }

    /**
     * 插入或者更新
     */
    @WorkerThread
    fun insertOrUpdate(cacheEntity: CacheEntity) {
        val cacheKey = cacheEntity.cacheKey ?: return
        val oldCache = cache.cacheDao().queryCacheByKey(cacheKey)
        oldCache?.let {
            cacheEntity.id = it.id
            cache.cacheDao().updateCache(cacheEntity)
            RetrofitCacheUtils.logI("更新缓存数据：$cacheKey")
        } ?: kotlin.run {
            cache.cacheDao().insertCache(cacheEntity)
            RetrofitCacheUtils.logI("保存缓存数据：$cacheKey")
        }
    }

    @WorkerThread
    fun deleteByCacheKey(cacheKey: String) {
        val oldCache = cache.cacheDao().queryCacheByKey(cacheKey)
        oldCache?.let {
            cache.cacheDao().deleteCache(it)
            RetrofitCacheUtils.logI("删除缓存数据：$cacheKey")
        }
    }

    companion object {

        @JvmStatic
        val instance: CacheDataManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            CacheDataManager()
        }
    }
}