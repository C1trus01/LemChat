package com.flxrs.dankchat.data.repo.crash

import kotlinx.serialization.Serializable

@Serializable
data class CrashData(
    val crashes: List<CrashEntry> = emptyList(),
    val hasUnshownCrash: Boolean = false,
    val knownFingerprints: Set<String> = emptySet(),
    val lastProcessExitTimestamp: Long = 0,
)
