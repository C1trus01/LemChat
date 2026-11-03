package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.api.helix.HelixApiStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Single

@Single
class ApiDebugSection(
    private val helixApiStats: HelixApiStats,
) : DebugSection {
    override val order = 10
    override val baseTitle = "API"

    override fun entries(): Flow<DebugSectionSnapshot> {
        val ticker =
            flow {
                while (true) {
                    emit(Unit)
                    delay(2_000)
                }
            }
        return combine(ticker) {
            val statusCounts =
                helixApiStats.statusCounts
                    .entries
                    .sortedBy { entry -> entry.key }
                    .map { (code, count) -> DebugEntry("HTTP $code", "$count") }

            DebugSectionSnapshot(
                title = baseTitle,
                entries = listOf(DebugEntry("Total Helix requests", "${helixApiStats.totalRequests}")) + statusCounts,
            )
        }
    }
}
