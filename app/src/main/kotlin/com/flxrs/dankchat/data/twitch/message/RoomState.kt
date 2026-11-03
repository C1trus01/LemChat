package com.flxrs.dankchat.data.twitch.message

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.utils.DateTimeUtils
import com.flxrs.dankchat.utils.TextResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class RoomState(
    val channel: UserName,
    val channelId: UserId,
    val tags: Map<RoomStateTag, Int> =
        mapOf(
            RoomStateTag.EMOTE to 0,
            RoomStateTag.SUBS to 0,
            RoomStateTag.SLOW to 0,
            RoomStateTag.R9K to 0,
            RoomStateTag.FOLLOW to -1,
        ),
) {
    val isEmoteMode get() = tags.getOrDefault(RoomStateTag.EMOTE, 0) > 0
    val isSubscriberMode get() = tags.getOrDefault(RoomStateTag.SUBS, 0) > 0
    val isSlowMode get() = tags.getOrDefault(RoomStateTag.SLOW, 0) > 0
    val isUniqueChatMode get() = tags.getOrDefault(RoomStateTag.R9K, 0) > 0
    val isFollowMode get() = tags.getOrDefault(RoomStateTag.FOLLOW, 0) >= 0

    val followerModeDuration get() = tags[RoomStateTag.FOLLOW]?.takeIf { it >= 0 }
    val slowModeWaitTime get() = tags[RoomStateTag.SLOW]?.takeIf { it > 0 }

    fun toDebugText(): String = tags
        .filter { (it.key == RoomStateTag.FOLLOW && it.value >= 0) || it.value > 0 }
        .map { (tag, value) ->
            when (tag) {
                RoomStateTag.FOLLOW -> {
                    when (value) {
                        0 -> "follow"
                        else -> "follow(${DateTimeUtils.formatSeconds(value * 60)})"
                    }
                }

                RoomStateTag.SLOW -> {
                    "slow(${DateTimeUtils.formatSeconds(value)})"
                }

                else -> {
                    tag.name.lowercase()
                }
            }
        }.joinToString()

    fun toDisplayTextResources(): ImmutableList<TextResource> = tags
        .filter { (it.key == RoomStateTag.FOLLOW && it.value >= 0) || it.value > 0 }
        .map { (tag, value) ->
            when (tag) {
                RoomStateTag.EMOTE -> {
                    TextResource.Res(R.string.room_state_emote_only)
                }

                RoomStateTag.SUBS -> {
                    TextResource.Res(R.string.room_state_subscriber_only)
                }

                RoomStateTag.R9K -> {
                    TextResource.Res(R.string.room_state_unique_chat)
                }

                RoomStateTag.SLOW -> {
                    TextResource.Res(R.string.room_state_slow_mode_duration, persistentListOf(DateTimeUtils.formatSeconds(value)))
                }

                RoomStateTag.FOLLOW -> {
                    when (value) {
                        0 -> TextResource.Res(R.string.room_state_follower_only)
                        else -> TextResource.Res(R.string.room_state_follower_only_duration, persistentListOf(DateTimeUtils.formatSeconds(value * 60)))
                    }
                }
            }
        }.toImmutableList()

    fun copyFromIrcMessage(msg: IrcMessage): RoomState = copy(
        tags = tags.mapValues { (key, value) -> msg.getRoomStateTag(key, value) },
    )

    private fun IrcMessage.getRoomStateTag(
        tag: RoomStateTag,
        default: Int,
    ): Int = tags[tag.ircTag]?.toIntOrNull() ?: default
}
