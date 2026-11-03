package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class ChannelDebugSection(
    private val chatChannelProvider: ChatChannelProvider,
    private val chatMessageRepository: ChatMessageRepository,
    private val channelRepository: ChannelRepository,
) : DebugSection {
    override val order = 4
    override val baseTitle = "Channel"

    override fun entries(): Flow<DebugSectionSnapshot> = chatChannelProvider.activeChannel.flatMapLatest { channel ->
        when (channel) {
            null -> {
                flowOf(DebugSectionSnapshot(title = baseTitle, entries = listOf(DebugEntry("Channel", "None"))))
            }

            else -> {
                chatMessageRepository.getChat(channel).map { messages ->
                    val roomState = channelRepository.getRoomState(channel)
                    val entries =
                        buildList {
                            add(DebugEntry("Messages in buffer", "${messages.size} / ${chatMessageRepository.scrollBackLength}"))
                            when (roomState) {
                                null -> {
                                    add(DebugEntry("Room state", "Unknown"))
                                }

                                else -> {
                                    val display = roomState.toDebugText()
                                    add(DebugEntry("Room state", display.ifEmpty { "None" }))
                                }
                            }
                        }
                    DebugSectionSnapshot(title = "$baseTitle (${channel.value})", entries = entries)
                }
            }
        }
    }
}
