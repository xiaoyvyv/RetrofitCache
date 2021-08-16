package com.xiaoyv.retrofitcache.model

import android.text.TextUtils
import com.xiaoyv.retrofitcache.RetrofitCacheUtils.doTry
import java.text.SimpleDateFormat
import java.util.*

/**
 * HttpHeaders
 *
 * @author why
 * @since 2021/01/31
 */
object HttpHeaders {
    const val FORMAT_HTTP_DATA = "EEE, dd MMM y HH:mm:ss 'GMT'"
    const val HEAD_KEY_RESPONSE_CODE = "ResponseCode"
    const val HEAD_KEY_RESPONSE_MESSAGE = "ResponseMessage"
    const val HEAD_KEY_ACCEPT = "Accept"
    const val HEAD_KEY_ACCEPT_ENCODING = "Accept-Encoding"
    const val HEAD_VALUE_ACCEPT_ENCODING = "gzip, deflate"
    const val HEAD_KEY_ACCEPT_LANGUAGE = "Accept-Language"
    const val HEAD_KEY_CONTENT_TYPE = "Content-Type"
    const val HEAD_KEY_CONTENT_LENGTH = "Content-Length"
    const val HEAD_KEY_CONTENT_ENCODING = "Content-Encoding"
    const val HEAD_KEY_CONTENT_DISPOSITION = "Content-Disposition"
    const val HEAD_KEY_CONTENT_RANGE = "Content-Range"
    const val HEAD_KEY_RANGE = "Range"
    const val HEAD_KEY_CACHE_CONTROL = "Cache-Control"
    const val HEAD_KEY_CONNECTION = "Connection"
    const val HEAD_VALUE_CONNECTION_KEEP_ALIVE = "keep-alive"
    const val HEAD_VALUE_CONNECTION_CLOSE = "close"
    const val HEAD_KEY_DATE = "Date"
    const val HEAD_KEY_EXPIRES = "Expires"
    const val HEAD_KEY_E_TAG = "ETag"
    const val HEAD_KEY_PRAGMA = "Pragma"
    const val HEAD_KEY_IF_MODIFIED_SINCE = "If-Modified-Since"
    const val HEAD_KEY_IF_NONE_MATCH = "If-None-Match"
    const val HEAD_KEY_LAST_MODIFIED = "Last-Modified"
    const val HEAD_KEY_LOCATION = "Location"
    const val HEAD_KEY_USER_AGENT = "User-Agent"
    const val HEAD_KEY_COOKIE = "Cookie"
    const val HEAD_KEY_COOKIE2 = "Cookie2"
    const val HEAD_KEY_SET_COOKIE = "Set-Cookie"
    const val HEAD_KEY_SET_COOKIE2 = "Set-Cookie2"
    const val HEAD_KEY_FROM_CACHE = "From-Cache"

    @JvmField
    val GMT_TIME_ZONE: TimeZone = TimeZone.getTimeZone("GMT")
        ?: TimeZone.getDefault()

    /**
     * Accept-Language: zh-CN,zh;q=0.8
     */
    @JvmStatic
    fun getAcceptLanguage(acceptLanguage: String?): String? {
        if (TextUtils.isEmpty(acceptLanguage)) {
            val locale = Locale.getDefault()
            val language = locale.language
            val country = locale.country
            val acceptLanguageBuilder = StringBuilder(language)
            if (!TextUtils.isEmpty(country)) {
                acceptLanguageBuilder.append('-').append(country).append(',')
                    .append(language).append(";q=0.8")
            }
            return acceptLanguageBuilder.toString()
        }
        return acceptLanguage
    }

    /**
     * 获取 UserAgent
     */
    @JvmStatic
    fun getUserAgent(): String? {
        doTry {
            return System.getProperty("http.agent")
        }
        return "Mozilla/5.0 (Linux; U; Android 10.0.2; zh-cn; CacheManager)"
    }

    @JvmStatic
    fun getDate(gmtTime: String?): Long {
        return parseGMTToMillis(gmtTime, 0)
    }

    @JvmStatic
    fun getDate(milliseconds: Long): String {
        return formatMillisToGMT(milliseconds)
    }

    @JvmStatic
    fun getExpiration(expiresTime: String?): Long {
        return parseGMTToMillis(expiresTime, -1)
    }

    @JvmStatic
    fun getLastModified(lastModified: String?): Long {
        return parseGMTToMillis(lastModified, 0)
    }

    @JvmStatic
    fun getCacheControl(cacheControl: String?, pragma: String?): String {
        return cacheControl ?: pragma ?: "no-cache"
    }

    @JvmStatic
    fun fromCache(fromCache: String?): Boolean {
        return fromCache != null
    }

    @JvmStatic
    @JvmOverloads
    fun parseGMTToMillis(gmtTime: String?, defaultLong: Long = 0): Long {
        doTry {
            gmtTime ?: return defaultLong
            val formatter = SimpleDateFormat(FORMAT_HTTP_DATA, Locale.US)
            formatter.timeZone = GMT_TIME_ZONE
            val date = formatter.parse(gmtTime)
            date ?: return defaultLong
            return date.time
        }
        return defaultLong
    }

    @JvmStatic
    fun formatMillisToGMT(milliseconds: Long): String {
        val date = Date(milliseconds)
        val simpleDateFormat = SimpleDateFormat(FORMAT_HTTP_DATA, Locale.US)
        simpleDateFormat.timeZone = GMT_TIME_ZONE
        return simpleDateFormat.format(date)
    }
}