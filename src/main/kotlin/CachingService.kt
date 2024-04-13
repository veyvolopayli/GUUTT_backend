package org.example

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

class CachingService(maxSize: Long, expireAfter: Pair<Long, TimeUnit>) {
    private val caffeine = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(expireAfter.first, expireAfter.second)
        .build<String, Any>()

    fun getCache(key: String): Any? {
        return caffeine.getIfPresent(key)
    }

    fun <T> putCache(key: String, value: T) {
        caffeine.put(key, value)
    }

    fun removeCache(key: String) {
        caffeine.invalidate(key)
    }

    fun clearAllCache() {
        caffeine.invalidateAll()
    }
}