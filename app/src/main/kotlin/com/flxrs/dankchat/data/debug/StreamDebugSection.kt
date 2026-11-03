package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.stream.StreamDataRepository
import com.flxrs.dankchat.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single

@Single
class StreamDebugSection(
    private val streamDataRepository: StreamDataRepository,
    private val chatChannelProvider: ChatChannelProvider,
) : DebugSection {
    override val order = 5
    override val baseTitle = "Stream"

    override fun entries(): Flow<DebugSectionSnapshot> = combine(chatChannelProvider.activeChannel, streamDataRepository.streamData) { channel, streams ->
        val channelSuffix = channel?.let { " (${it.value})" }.orEmpty()
        val stream = channel?.let { ch -> streams.find { it.channel == ch } }
        when (stream) {
            null -> {
                DebugSectionSnapshot(
                    title = "$baseTitle$channelSuffix",
                    entries = listOf(DebugEntry("Status", "Offline")),
                )
            }

            else -> {
                DebugSectionSnapshot(
                    title = "$baseTitle$channelSuffix",
                    entries =
                        listOf(
                            DebugEntry("Status", "Live"),
                            DebugEntry("Viewers", "${stream.viewerCount}"),
                            DebugEntry("Uptime", DateTimeUtils.calculateUptime(stream.startedAt)),
                            DebugEntry("Category", stream.category?.ifBlank { null } ?: "None"),
                            DebugEntry("Stream fetches", "${streamDataRepository.fetchCount}"),
                        ),
                )
            }
        }
    }
}
