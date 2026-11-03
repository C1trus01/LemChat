package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.utils.TextResource
import kotlinx.coroutines.flow.Flow

interface DebugSection {
    val baseTitle: String
    val order: Int

    fun entries(): Flow<DebugSectionSnapshot>
}

data class DebugSectionSnapshot(
    val title: String,
    val entries: List<DebugEntry>,
)

data class DebugEntry(
    val label: TextResource,
    val value: String,
    val copyValue: String? = null,
    val stacked: Boolean = false,
) {
    constructor(
        label: String,
        value: String,
        copyValue: String? = null,
        stacked: Boolean = false,
    ) : this(TextResource.Plain(label), value, copyValue, stacked)
}
