package com.flxrs.dankchat.data.api.cache

sealed interface CachedResult<out T> {
    data class Success<T>(
        val data: T,
    ) : CachedResult<T>

    data class CachedFallback<T>(
        val data: T,
        val error: Throwable,
    ) : CachedResult<T>

    data class Failure(
        val error: Throwable,
    ) : CachedResult<Nothing>
}
