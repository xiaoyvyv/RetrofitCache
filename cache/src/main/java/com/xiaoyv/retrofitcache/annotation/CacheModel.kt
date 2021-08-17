package com.xiaoyv.retrofitcache.annotation

import androidx.annotation.IntDef

/**
 * CacheModel
 *
 * @author why
 * @since 2021/01/31
 **/
@IntDef(
    CacheModel.DEFAULT,
    CacheModel.NO_CACHE,
    CacheModel.REQUEST_FAILED_READ_CACHE,
    CacheModel.IF_NONE_CACHE_REQUEST,
    CacheModel.FIRST_CACHE_THEN_REQUEST
)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class CacheModel {
    companion object {
        /**
         * 按照HTTP协议的默认缓存规则，例如有304响应头时缓存
         */
        const val DEFAULT = 0

        /**
         * 不使用缓存
         */
        const val NO_CACHE = 1

        /**
         * 请求网络失败后，读取缓存
         */
        const val REQUEST_FAILED_READ_CACHE = 2

        /**
         * 如果缓存不存在才请求网络，否则使用缓存
         */
        const val IF_NONE_CACHE_REQUEST = 3

        /**
         * 先使用缓存，不管是否存在，仍然请求网络并返回（缓存存在则回调两次）
         */
        @Deprecated("弃用，无效")
        const val FIRST_CACHE_THEN_REQUEST = 4
    }
}
