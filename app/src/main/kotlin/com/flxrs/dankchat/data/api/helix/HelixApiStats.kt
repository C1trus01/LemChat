package com.flxrs.dankchat.data.api.helix

import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.plusAssign

@Single
class HelixApiStats {
    private val _totalRequests = AtomicInt(0)
    private val _statusCounts = ConcurrentHashMap<Int, AtomicInt>()

    val totalRequests: Int get() = _totalRequests.load()
    val statusCounts: Map<Int, Int> get() = _statusCounts.mapValues { it.value.load() }

    fun recordResponse(statusCode: Int) {
        _totalRequests += 1
        _statusCounts.getOrPut(statusCode) { AtomicInt(0) } += 1
    }
}
