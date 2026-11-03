package com.flxrs.dankchat.data.debug

import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.emote.EmojiRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class EmoteDebugSection(
    private val emoteRepository: EmoteRepository,
    private val emojiRepository: EmojiRepository,
    private val chatChannelProvider: ChatChannelProvider,
) : DebugSection {
    override val order = 6
    override val baseTitle = "Emotes"

    override fun entries(): Flow<DebugSectionSnapshot> = combine(
        chatChannelProvider.activeChannel.flatMapLatest { channel ->
            when (channel) {
                null -> flowOf(null)
                else -> emoteRepository.getEmotes(channel).map { channel to it }
            }
        },
        emojiRepository.emojis,
    ) { channelEmotes, emojis ->
        val (channel, emotes) = channelEmotes ?: (null to null)
        val channelSuffix = channel?.let { " (${it.value})" }.orEmpty()
        when (emotes) {
            null -> {
                DebugSectionSnapshot(
                    title = "$baseTitle$channelSuffix",
                    entries = listOf(DebugEntry("Emojis", "${emojis.size}")),
                )
            }

            else -> {
                val twitch = emotes.twitchEmotes.size
                val ffz = emotes.ffzChannelEmotes.size + emotes.ffzGlobalEmotes.size
                val bttv = emotes.bttvChannelEmotes.size + emotes.bttvGlobalEmotes.size
                val sevenTv = emotes.sevenTvChannelEmotes.size + emotes.sevenTvGlobalEmotes.size
                val total = twitch + ffz + bttv + sevenTv
                DebugSectionSnapshot(
                    title = "$baseTitle$channelSuffix",
                    entries =
                        listOf(
                            DebugEntry("Twitch", "$twitch"),
                            DebugEntry("FFZ", "$ffz"),
                            DebugEntry("BTTV", "$bttv"),
                            DebugEntry("7TV", "$sevenTv"),
                            DebugEntry("Total emotes", "$total"),
                            DebugEntry("Emojis", "${emojis.size}"),
                        ),
                )
            }
        }
    }
}
