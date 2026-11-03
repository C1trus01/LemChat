package com.flxrs.dankchat.data.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Single

@Single
class DebugSectionRegistry(
    sections: List<DebugSection>,
) {
    private val sorted = sections.sortedBy { it.order }

    fun allSections(): Flow<List<DebugSectionSnapshot>> {
        if (sorted.isEmpty()) return flowOf(emptyList())
        return combine(sorted.map { it.entries() }) { it.toList() }
    }
}
