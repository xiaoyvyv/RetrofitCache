package com.xiaoyv.retrofitcache.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.xiaoyv.retrofitcache.RetrofitCache
import com.xiaoyv.retrofitcache.annotation.CacheModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

/**
 * CacheEntity
 *
 * @author why
 * @since 2021/08/15
 **/
@Entity(
    tableName = CacheEntity.TABLE_NAME,
    indices = [Index(value = ["cacheKey"], unique = true)]
)
data class CacheEntity(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    val cacheKey: String?,
    val localExpire: Long?,
    var data: String? = "{}",
    val headers: ByteArray? = byteArrayOf()
) {
    @Ignore
    var isExpire = false

    /**
     * 检测是否缓存过期
     *
     * 304 的默认缓存模式,设置缓存时间无效,需要依靠服务端的响应头控制
     *
     * @param cacheTime 允许的缓存时间
     * @param baseTime  基准时间,小于当前时间视为过期
     * @return 是否过期
     */
    fun checkExpire(
        @CacheModel cacheMode: Int,
        cacheTime: Long,
        baseTime: Long
    ): Boolean {
        val local = localExpire ?: 0
        return when {
            cacheMode == CacheModel.DEFAULT -> {
                local < baseTime
            }
            cacheTime == RetrofitCache.GlobalConfig.CACHE_TIME_NO_EXPIRED -> {
                false
            }
            cacheTime < 0 -> {
                false
            }
            else -> {
                (local + cacheTime) < baseTime
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is CacheEntity -> {
                this.cacheKey == other.cacheKey
                        && this.localExpire == other.localExpire
                        && this.data == other.data
                        && this.headers.contentEquals(other.headers)
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return super.hashCode() + Arrays.hashCode(headers)
    }

    override fun toString(): String {
        return "id: $id, " +
                "isExpire: $isExpire, " +
                "cacheKey: $cacheKey, " +
                "expireDate: $localExpire, " +
                "data: ${data?.length}, " +
                "headers: ${toHeaders(headers)}"
    }

    companion object {
        const val TABLE_NAME = "cache"

        @JvmStatic
        fun toByteArray(input: Any?): ByteArray {
            val inputObj = input ?: return byteArrayOf()
            return try {
                val arrayOutputStream = ByteArrayOutputStream()
                ObjectOutputStream(arrayOutputStream).use {
                    it.writeObject(inputObj)
                    it.flush()
                }
                arrayOutputStream.use {
                    it.toByteArray()
                }
            } catch (e: Exception) {
                byteArrayOf()
            }
        }

        @JvmStatic
        fun toHeaders(byteArray: ByteArray?): LinkedHashMap<String, String> {
            val inputArray = byteArray ?: return linkedMapOf()
            val obj = try {
                ByteArrayInputStream(inputArray).use {
                    ObjectInputStream(it).use { objInputStream ->
                        objInputStream.readObject()
                    }
                }
            } catch (e: Exception) {
                linkedMapOf<String, String>()
            }
            return if (obj is LinkedHashMap<*, *>) {
                obj as LinkedHashMap<String, String>
            } else linkedMapOf()
        }
    }
}
