package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.utils.TextResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class ErrorsDebugSection(
    private val dataRepository: DataRepository,
    private val chatMessageRepository: ChatMessageRepository,
) : DebugSection {
    override val order = 9
    override val baseTitle = "Errors"

    override fun entries(): Flow<DebugSectionSnapshot> = combine(dataRepository.dataLoadingFailures, chatMessageRepository.chatLoadingFailures) { dataFailures, chatFailures ->
        val totalFailures = dataFailures.size + chatFailures.size
        val entries =
            buildList {
                add(DebugEntry("Total failures", "$totalFailures"))
                dataFailures.forEach { failure ->
                    add(DebugEntry(label(failure.step.displayNameRes, failure.step.channel?.value), failure.failure.message ?: "Unknown error", stacked = true))
                }
                chatFailures.forEach { failure ->
                    add(DebugEntry(label(failure.step.displayNameRes, failure.step.channel?.value), failure.failure.message ?: "Unknown error", stacked = true))
                }
            }
        DebugSectionSnapshot(title = baseTitle, entries = entries)
    }

    private fun label(
        displayNameRes: Int,
        channel: String?,
    ): TextResource = when (channel) {
        null -> TextResource.Res(displayNameRes)
        else -> TextResource.Res(R.string.data_loading_step_with_channels, persistentListOf(TextResource.Res(displayNameRes), channel))
    }
}
