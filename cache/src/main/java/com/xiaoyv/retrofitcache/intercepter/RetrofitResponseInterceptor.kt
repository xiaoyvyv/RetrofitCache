package com.xiaoyv.retrofitcache.intercepter

import com.xiaoyv.retrofitcache.RetrofitCacheUtils
import com.xiaoyv.retrofitcache.RetrofitCacheUtils.doTry
import com.xiaoyv.retrofitcache.db.CacheDataManager
import com.xiaoyv.retrofitcache.model.HttpHeaders
import com.xiaoyv.retrofitcache.policy.BasePolicy
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * RetrofitResponseInterceptor
 *
 * 负责保存网络的响应数据
 *
 * @author why
 * @since 2021/08/14
 **/
class RetrofitResponseInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val headers = response.headers
        val responseBody: ResponseBody = response.body ?: return response

        // 缓冲全部内容
        var buffer = responseBody.source().also {
            it.request(Long.MAX_VALUE)
        }.buffer

        // 没有 Body 或者未知编码 不缓存
        if (!response.isSuccessful || !response.promisesBody() || bodyHasUnknownEncoding(response.headers)) {
            return response
        }

        // gzip 自动解压
        if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            }
        }

        // 不是纯文本则直接返回
        if (!RetrofitCacheUtils.isPlainText(buffer)) {
            return response
        }

        // 读取响应内容
        val charset = responseBody.contentType()?.charset(UTF8) ?: UTF8

        // 获取内容长度
        val contentLength = responseBody.contentLength()
        if (contentLength == 0L) {
            return response
        }

        // 缓存的响应信息
        val content = buffer.clone().readString(charset).let {
            if (it.isBlank()) "{}" else it
        }

        // 获取缓存的配置信息
        val cacheKey = BasePolicy.getCacheKey(request)
        val cacheMode = BasePolicy.getCacheModel(request)

        // 构建缓存实体
        val cacheEntity = RetrofitCacheUtils.createCacheEntity(
            headers, content, cacheMode, cacheKey
        )

        cacheEntity?.let {
            val isFromCacheResponse =
                HttpHeaders.fromCache(headers[HttpHeaders.HEAD_KEY_FROM_CACHE])

            // 是否来自已经缓存的内容，已缓存的不用再次更新
            if (!isFromCacheResponse) {
                doTry {
                    // 缓存命中，更新缓存
                    CacheDataManager.instance.insertOrUpdate(it)
                }
            }
        } ?: doTry {
            // 服务器不需要缓存，移除本地缓存
            CacheDataManager.instance.deleteByCacheKey(cacheKey)
        }

        return response
    }

    companion object {
        @JvmField
        internal val UTF8: Charset = StandardCharsets.UTF_8

        /**
         * 正文具有未知编码
         */
        private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
            val contentEncoding = headers["Content-Encoding"]
            return (contentEncoding != null
                    && !contentEncoding.equals("identity", ignoreCase = true)
                    && !contentEncoding.equals("gzip", ignoreCase = true))
        }
    }
}