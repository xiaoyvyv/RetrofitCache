package com.xiaoyv.retrofitcache.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xiaoyv.retrofitcache.db.dao.CacheDao
import com.xiaoyv.retrofitcache.db.entity.CacheEntity

/**
 * CacheDataBase
 *
 * @author why
 * @since 2021/08/15
 **/
@Database(
    entities = [
        CacheEntity::class
    ],
    version = CacheDataBase.CACHE_DATABASE_VERSION
)
abstract class CacheDataBase : RoomDatabase() {

    abstract fun cacheDao(): CacheDao

    companion object {
        const val CACHE_DATABASE_NAME = "retrofitCache.db"
        const val CACHE_DATABASE_VERSION = 2
    }
}