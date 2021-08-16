package com.xiaoyv.retrofitcache.db.dao

import androidx.room.*
import com.xiaoyv.retrofitcache.db.entity.CacheEntity

@Dao
interface CacheDao {

    @Query("SELECT * FROM cache WHERE cacheKey = :cacheKey LIMIT 1")
    fun queryCacheByKey(cacheKey: String): CacheEntity?

    @Insert
    fun insertCache(cache: CacheEntity)

    @Update
    fun updateCache(cache: CacheEntity)

    @Delete
    fun deleteCache(cache: CacheEntity)

}
    