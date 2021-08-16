package com.xiaoyv.retrofitcache

import android.text.TextUtils
import android.util.Log
import com.xiaoyv.retrofitcache.annotation.CacheModel
import com.xiaoyv.retrofitcache.db.entity.CacheEntity
import com.xiaoyv.retrofitcache.model.HttpHeaders
import okhttp3.Headers
import okhttp3.Request
import okio.Buffer
import java.io.EOFException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * RetrofitCacheUtils
 *
 * @author why
 * @since 2021/08/15
 **/
object RetrofitCacheUtils {

    @JvmStatic
    fun logI(message: String?) {
        Log.i(RetrofitCache.TAG, message.orEmpty())
    }

    @JvmStatic
    fun logE(message: String?) {
        Log.e(RetrofitCache.TAG, message.orEmpty())
    }

    /**
     * 如果相关正文可能包含人类可读文本，则返回 true。
     * 使用少量代码点样本来检测二进制文件签名中常用的 unicode 控制字符。
     */
    @JvmStatic
    fun isPlainText(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (e: EOFException) {
            false
        }
    }

    @JvmStatic
    fun convertHeaderMap(okHeaders: Headers?): LinkedHashMap<String, String> {
        val headersMap: LinkedHashMap<String, String> = LinkedHashMap()
        okHeaders?.let {
            it.names().forEach { name ->
                headersMap[name] = it[name].orEmpty()
            }
        }
        return headersMap
    }

    @JvmStatic
    inline fun doTry(onError: (Exception) -> Unit = {}, action: () -> Unit = {}) {
        try {
            action.invoke()
        } catch (e: Exception) {
            onError.invoke(e)
        }
    }

    @JvmStatic
    fun String?.md5(): String {
        val string = this ?: return ""
        try {
            val bytes: ByteArray = MessageDigest.getInstance("MD5").digest(string.toByteArray())
            val result = StringBuilder()
            for (b in bytes) {
                var temp = Integer.toHexString((b.toInt() and 0xff))
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result.append(temp)
            }
            return result.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 根据请求结果生成对应的缓存实体类，以下为缓存相关的响应头
     *
     * Cache-Control: public                             响应被缓存，并且在多用户间共享
     * Cache-Control: private                            响应只能作为私有缓存，不能在用户之间共享
     * Cache-Control: no-cache                           提醒浏览器要从服务器提取文档进行验证
     * Cache-Control: no-store                           绝对禁止缓存（用于机密，敏感文件）
     * Cache-Control: max-age=60                         60秒之后缓存过期（相对时间）,优先级比Expires高
     * Date: Mon, 19 Nov 2012 08:39:00 GMT               当前response发送的时间
     * Expires: Mon, 19 Nov 2012 08:40:01 GMT            缓存过期的时间（绝对时间）
     * Last-Modified: Mon, 19 Nov 2012 08:38:01 GMT      服务器端文件的最后修改时间
     * ETag: "20b1add7ec1cd1:0"                          服务器端文件的ETag值
     * 如果同时存在cache-control和Expires，浏览器总是优先使用cache-control
     *
     * @param responseHeaders 返回数据中的响应头
     * @param data            解析出来的数据
     * @param cacheMode       缓存的模式
     * @param cacheKey        缓存的key
     * @return 缓存的实体类
     */
    fun createCacheEntity(
        responseHeaders: Headers,
        data: String?,
        @CacheModel cacheMode: Int,
        cacheKey: String?
    ): CacheEntity? {
        // 缓存相对于本地的到期时间
        var localExpire: Long = 0

        // 默认模式
        if (cacheMode == CacheModel.DEFAULT) {
            val date = HttpHeaders.getDate(responseHeaders[HttpHeaders.HEAD_KEY_DATE])
            val expires = HttpHeaders.getExpiration(responseHeaders[HttpHeaders.HEAD_KEY_EXPIRES])
            val cacheControl = HttpHeaders.getCacheControl(
                responseHeaders[HttpHeaders.HEAD_KEY_CACHE_CONTROL],
                responseHeaders[HttpHeaders.HEAD_KEY_PRAGMA]
            )

            // 没有缓存头控制，不需要缓存
            if (TextUtils.isEmpty(cacheControl) && expires <= 0) {
                return null
            }
            var maxAge: Long = 0
            if (!TextUtils.isEmpty(cacheControl)) {
                val tokens = StringTokenizer(cacheControl, ",")
                while (tokens.hasMoreTokens()) {
                    val token = tokens.nextToken().trim().lowercase()
                    if ("no-cache" == token || "no-store" == token) {
                        // 服务器指定不缓存
                        return null
                    } else if (token.startsWith("max-age=")) {
                        doTry {
                            // 获取最大缓存时间
                            maxAge = token.substring(8).toLong()
                            // 服务器缓存设置立马过期，不缓存
                            if (maxAge <= 0) {
                                return null
                            }
                        }
                    }
                }
            }

            // 获取基准缓存时间，优先使用response中的date头，如果没有就使用本地时间
            var now = System.currentTimeMillis()
            if (date > 0) {
                now = date
            }
            if (maxAge > 0) {
                // Http1.1 优先验证 Cache-Control 头
                localExpire = now + maxAge * 1000
            } else if (expires >= 0) {
                // Http1.0 验证 Expires 头
                localExpire = expires
            }
        } else {
            localExpire = System.currentTimeMillis()
        }

        // 转换为 HashMap
        val headerMap = convertHeaderMap(responseHeaders)

        // 构建缓存实体对象
        return CacheEntity(
            cacheKey = cacheKey,
            localExpire = localExpire,
            data = data,
            headers = CacheEntity.toByteArray(headerMap)
        )
    }

    /**
     * 对每个请求添加默认的请求头，如果有缓存，并返回缓存实体对象
     *
     * Cache-Control: max-age=0                            以秒为单位
     * If-Modified-Since: Mon, 19 Nov 2012 08:38:01 GMT    缓存文件的最后修改时间。
     * If-None-Match: "0693f67a67cc1:0"                    缓存文件的ETag值
     * Cache-Control: no-cache                             不使用缓存
     * Pragma: no-cache                                    不使用缓存
     * Accept-Language: zh-CN,zh;q=0.8                     支持的语言
     * User-Agent:                                         用户代理，它的信息包括硬件平台、系统软件、应用软件和用户个人偏好
     * Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36
     *
     * @param request     请求类
     * @param cacheEntity 缓存实体类
     * @param cachePolicy 缓存模式
     */
    fun addCacheHeaders(
        request: Request,
        cacheEntity: CacheEntity?,
        @CacheModel cachePolicy: Int
    ): Request {
        // 1. 按照标准的 http 协议，添加304相关请求头
        if (cacheEntity != null && cachePolicy == CacheModel.DEFAULT) {
            val responseHeaders = CacheEntity.toHeaders(cacheEntity.headers)

            val eTag = responseHeaders[HttpHeaders.HEAD_KEY_E_TAG].toString()
            if (eTag.isNotEmpty()) {
                return request.newBuilder()
                    .header(HttpHeaders.HEAD_KEY_IF_NONE_MATCH, eTag)
                    .build()
            }
            val lastModified = HttpHeaders.getLastModified(
                responseHeaders[HttpHeaders.HEAD_KEY_LAST_MODIFIED].toString()
            )
            if (lastModified > 0) {
                return request.newBuilder().header(
                    HttpHeaders.HEAD_KEY_IF_MODIFIED_SINCE,
                    HttpHeaders.formatMillisToGMT(lastModified)
                ).build()
            }
        }
        return request
    }
}