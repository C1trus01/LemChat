package com.flxrs.dankchat.data.repo.data

sealed interface EmoteLoadResult {
    data object Loaded : EmoteLoadResult

    data object CachedFallback : EmoteLoadResult
}
