package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.database.dao.EmoteUsageDao
import com.flxrs.dankchat.data.database.entity.EmoteUsageEntity
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class EmoteUsageRepository(
    private val emoteUsageDao: EmoteUsageDao,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)

    val recentEmoteIds: StateFlow<Set<String>> =
        emoteUsageDao
            .getRecentUsages()
            .map { usages -> usages.mapTo(mutableSetOf()) { it.emoteId } }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    init {
        scope.launch {
            runCatching {
                emoteUsageDao.deleteOldUsages()
            }
        }
    }

    suspend fun addEmoteUsage(emoteId: String) {
        val entity =
            EmoteUsageEntity(
                emoteId = emoteId,
                lastUsed = Instant.now(),
            )
        emoteUsageDao.addUsage(entity)
    }

    suspend fun clearUsages() = emoteUsageDao.clearUsages()

    fun getRecentUsages() = emoteUsageDao.getRecentUsages()
}
