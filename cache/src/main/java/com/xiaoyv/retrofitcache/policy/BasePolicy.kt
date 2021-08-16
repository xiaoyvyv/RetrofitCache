package com.xiaoyv.retrofitcache.policy

import com.xiaoyv.retrofitcache.RetrofitCache
import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import com.xiaoyv.retrofitcache.RetrofitCacheUtils.doTry
import com.xiaoyv.retrofitcache.RetrofitCacheUtils.md5
import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.annotation.CachePolicy
import com.xiaoyv.retrofitcache.db.CacheDataManager
import com.xiaoyv.retrofitcache.db.entity.CacheEntity
import com.xiaoyv.retrofitcache.model.HttpHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Invocation
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * BasePolicy
 *
 * @author why
 * @since 2021/08/15
 **/
open class BasePolicy {


    companion object {
        /**
         * 获取请求带参数链接
         */
        @JvmStatic
        fun getHttpLink(request: Request): String = request.url.toUrl().toExternalForm()

        /**
         * 获取唯一 Key（根据全链接和请求体信息作为标识符）
         */
        @JvmStatic
        fun getUniqueKey(request: Request)
                : String = (getHttpLink(request) + getRequestBodyString(request)).md5()

        /**
         * 获取 RequestBody
         */
        @JvmStatic
        fun getRequestBodyString(request: Request): String {
            doTry {
                request.body?.let {
                    val buffer = Buffer().apply {
                        it.writeTo(this)
                    }
                    // 不是纯文本则返回
                    if (RetrofitCacheUtils.isPlainText(buffer)) {
                        return ""
                    }
                    // 读取 charset
                    val charset = it.contentType()?.charset(StandardCharsets.UTF_8)
                        ?: StandardCharsets.UTF_8
                    // 返回字符串
                    return buffer.readString(charset)
                }
            }
            return ""
        }

        /**
         * 获取缓存 Key
         *
         * @param request
         */
        @JvmStatic
        fun getCacheKey(request: Request): String {
            request.tag(Invocation::class.java)?.let {
                it.method().getAnnotation(CachePolicy::class.java)?.let { policy ->
                    val cacheKey = policy.cacheKey
                    return if (cacheKey == RetrofitCache.GlobalConfig.DEFAULT_FLAG_KEY) {
                        getUniqueKey(request)
                    } else {
                        cacheKey
                    }
                }
            }
            return getUniqueKey(request)
        }

        /**
         * 获取缓存模式
         *
         * @param request
         */
        @JvmStatic
        @CacheModel
        fun getCacheModel(request: Request): Int {
            val globalConfig = RetrofitCache.instance.globalConfig
            request.tag(Invocation::class.java)?.let {
                it.method().getAnnotation(CachePolicy::class.java)?.let { policy ->
                    val cacheMode = policy.cacheMode
                    return if (cacheMode == RetrofitCache.GlobalConfig.DEFAULT_FLAG_MODE) {
                        globalConfig.cacheMode
                    } else {
                        cacheMode
                    }
                }
            }
            return globalConfig.cacheMode
        }

        /**
         * 获取缓存时间
         *
         * @param request
         */
        @JvmStatic
        fun getCacheTime(request: Request): Long {
            val globalConfig = RetrofitCache.instance.globalConfig
            request.tag(Invocation::class.java)?.let {
                it.method().getAnnotation(CachePolicy::class.java)?.let { policy ->
                    val cacheTime = policy.cacheTime
                    return if (cacheTime == RetrofitCache.GlobalConfig.DEFAULT_FLAG_TIME) {
                        globalConfig.cacheTime
                    } else {
                        cacheTime
                    }
                }
            }
            return globalConfig.cacheTime
        }

        /**
         * 获取缓存的请求
         *
         * A: 缓存请求
         * B: 剩余有效时间
         */
        @JvmStatic
        fun prepareCacheResponse(request: Request): Pair<Response?, Long> {
            // 查询是否存在缓存
            var surplusMill = 0L
            val response = prepareCache(request)?.let { cache ->
                val cacheTime = getCacheTime(request)
                val responseHeaders = CacheEntity.toHeaders(cache.headers)

                // 剩余有效时间
                surplusMill = ((cache.localExpire ?: 0) + cacheTime) - System.currentTimeMillis()

                val contentType = responseHeaders[HttpHeaders.HEAD_KEY_CONTENT_TYPE]
                    ?: "text/plain"

                val content = cache.data.toString()

                // 创建缓存响应
                Response.Builder().apply {
                    responseHeaders.let { map ->
                        for (head in map.keys) {
                            map[head]?.let { v ->
                                header(head, v)
                            }
                        }
                    }
                    header(HttpHeaders.HEAD_KEY_FROM_CACHE, getCacheKey(request))
                    protocol(Protocol.HTTP_1_1)
                    message("Response from cache")
                    sentRequestAtMillis(System.currentTimeMillis())
                    receivedResponseAtMillis(System.currentTimeMillis())
                    request(request.newBuilder().build())
                    code(HttpsURLConnection.HTTP_OK)
                    body(content.toResponseBody(contentType.toMediaType()))
                }.build()
            }
            return Pair(response, surplusMill)
        }

        /**
         * 准备缓存数据，过期或没有返回 null
         */
        @JvmStatic
        fun prepareCache(request: Request): CacheEntity? {
            val cacheMode = getCacheModel(request)
            val cacheKey = getCacheKey(request)
            val cacheTime = getCacheTime(request)
            var cacheEntity: CacheEntity? = null

            // 检测缓存是否过期
            if (cacheMode != CacheModel.NO_CACHE) {
                cacheEntity = CacheDataManager.instance.queryByCacheKey(cacheKey)
                // 添加缓存请求头
                RetrofitCacheUtils.addCacheHeaders(request, cacheEntity, cacheMode)
                cacheEntity?.apply {
                    if (checkExpire(cacheMode, cacheTime, System.currentTimeMillis())) {
                        isExpire = true
                    }
                }
            }

            if (cacheEntity == null
                || cacheEntity.isExpire
                || cacheEntity.headers == null
                || cacheEntity.data.isNullOrBlank()
            ) {
                cacheEntity = null
            }
            return cacheEntity
        }
    }
}