package com.flxrs.dankchat.data.repo.crash

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CrashEntry(
    val id: Long,
    val fingerprint: String,
    val timestamp: String,
    val version: String,
    val androidInfo: String,
    val device: String,
    val threadName: String,
    val exceptionHeader: String,
    val stackTrace: String,
)
