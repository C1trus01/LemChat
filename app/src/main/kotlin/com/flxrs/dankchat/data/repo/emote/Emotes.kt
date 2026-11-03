package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.twitch.emote.CheermoteSet
import com.flxrs.dankchat.data.twitch.emote.GenericEmote

data class GlobalEmoteState(
    val twitchEmotes: List<GenericEmote> = emptyList(),
    val ffzEmotes: List<GenericEmote> = emptyList(),
    val bttvEmotes: List<GenericEmote> = emptyList(),
    val sevenTvEmotes: List<GenericEmote> = emptyList(),
)

data class ChannelEmoteState(
    val twitchEmotes: List<GenericEmote> = emptyList(),
    val ffzEmotes: List<GenericEmote> = emptyList(),
    val bttvEmotes: List<GenericEmote> = emptyList(),
    val sevenTvEmotes: List<GenericEmote> = emptyList(),
    val cheermoteSets: List<CheermoteSet> = emptyList(),
)

fun mergeEmotes(
    global: GlobalEmoteState,
    channel: ChannelEmoteState,
): Emotes {
    // Deduplicate twitch emotes by ID — channel (follower) emotes take precedence
    val channelEmoteIds = channel.twitchEmotes.mapTo(mutableSetOf()) { it.id }
    val deduplicatedGlobalTwitchEmotes = global.twitchEmotes.filterNot { it.id in channelEmoteIds }
    return Emotes(
        twitchEmotes = deduplicatedGlobalTwitchEmotes + channel.twitchEmotes,
        ffzChannelEmotes = channel.ffzEmotes,
        ffzGlobalEmotes = global.ffzEmotes,
        bttvChannelEmotes = channel.bttvEmotes,
        bttvGlobalEmotes = global.bttvEmotes,
        sevenTvChannelEmotes = channel.sevenTvEmotes,
        sevenTvGlobalEmotes = global.sevenTvEmotes,
    )
}

data class Emotes(
    val twitchEmotes: List<GenericEmote> = emptyList(),
    val ffzChannelEmotes: List<GenericEmote> = emptyList(),
    val ffzGlobalEmotes: List<GenericEmote> = emptyList(),
    val bttvChannelEmotes: List<GenericEmote> = emptyList(),
    val bttvGlobalEmotes: List<GenericEmote> = emptyList(),
    val sevenTvChannelEmotes: List<GenericEmote> = emptyList(),
    val sevenTvGlobalEmotes: List<GenericEmote> = emptyList(),
) {
    val sorted: List<GenericEmote> =
        buildList {
            addAll(twitchEmotes)

            addAll(ffzChannelEmotes)
            addAll(bttvChannelEmotes)
            addAll(sevenTvChannelEmotes)

            addAll(ffzGlobalEmotes)
            addAll(bttvGlobalEmotes)
            addAll(sevenTvGlobalEmotes)
        }.sortedBy(GenericEmote::code)

    val suggestions: List<GenericEmote> = sorted.distinctBy(GenericEmote::code)
}
