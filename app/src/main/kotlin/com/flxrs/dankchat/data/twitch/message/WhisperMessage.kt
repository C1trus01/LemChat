package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.badge.Badge
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.message.Message.Companion.parseEmoteTag
import com.flxrs.dankchat.utils.extensions.parseColorOrNull
import java.util.UUID

data class WhisperMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val id: String = UUID.randomUUID().toString(),
    override val highlights: Set<Highlight> = emptySet(),
    val userId: UserId?,
    val name: UserName,
    val displayName: DisplayName,
    val color: Int? = null,
    val recipientId: UserId?,
    val recipientName: UserName,
    val recipientDisplayName: DisplayName,
    val recipientColor: Int? = null,
    val message: String,
    val rawEmotes: String,
    val rawBadges: String?,
    val rawBadgeInfo: String? = null,
    val originalMessage: String = message,
    val emotes: List<ChatMessageEmote> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val userDisplay: UserDisplay? = null,
    val recipientDisplay: UserDisplay? = null,
    override val emoteData: Message.EmoteData = Message.EmoteData(originalMessage, WHISPER_CHANNEL, parseEmoteTag(originalMessage, rawEmotes)),
    override val badgeData: Message.BadgeData = Message.BadgeData(userId, channel = null, badgeTag = rawBadges, badgeInfoTag = rawBadgeInfo),
) : Message {
    companion object {
        val WHISPER_CHANNEL = "w".toUserName()

        fun parseFromIrc(
            ircMessage: IrcMessage,
            recipientName: DisplayName,
            recipientColorTag: String?,
        ): WhisperMessage = with(ircMessage) {
            val name = prefix.substringBefore('!')
            val displayName = tags["display-name"] ?: name
            val color = tags["color"]?.parseColorOrNull()
            val recipientColor = recipientColorTag?.parseColorOrNull()
            val emoteTag = tags["emotes"].orEmpty()
            val message = params.getOrElse(1) { "" }

            return WhisperMessage(
                timestamp = tags["tmi-sent-ts"]?.toLongOrNull() ?: System.currentTimeMillis(),
                id = tags["id"] ?: UUID.randomUUID().toString(),
                userId = tags["user-id"]?.toUserId(),
                name = name.toUserName(),
                displayName = displayName.toDisplayName(),
                color = color,
                recipientId = null,
                recipientName = recipientName.toUserName(),
                recipientDisplayName = recipientName,
                recipientColor = recipientColor,
                message = message,
                rawEmotes = emoteTag,
                rawBadges = tags["badges"],
                rawBadgeInfo = tags["badge-info"],
            )
        }
    }
}

val WhisperMessage.senderAliasOrFormattedName: String
    get() = userDisplay?.alias ?: name.formatWithDisplayName(displayName)

val WhisperMessage.recipientAliasOrFormattedName: String
    get() = recipientDisplay?.alias ?: recipientName.formatWithDisplayName(recipientDisplayName)
