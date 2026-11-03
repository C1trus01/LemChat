package com.flxrs.dankchat.data.twitch.message

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.irc.IrcMessage
import com.flxrs.dankchat.data.toUserName
import java.util.UUID

data class UserNoticeMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val id: String = UUID.randomUUID().toString(),
    override val highlights: Set<Highlight> = emptySet(),
    val channel: UserName,
    val message: String,
    val childMessage: PrivMessage?,
    val tags: Map<String, String>,
) : Message {
    override val emoteData: Message.EmoteData? = childMessage?.emoteData
    override val badgeData: Message.BadgeData? = childMessage?.badgeData

    companion object {
        val USER_NOTICE_MSG_IDS_WITH_MESSAGE =
            listOf(
                "sub",
                "subgift",
                "resub",
                "bitsbadgetier",
                "ritual",
                "announcement",
                "viewermilestone",
                "modiversary",
            )

        // Mod streaks are handled like watch streaks
        val MILESTONE_MSG_IDS = listOf("viewermilestone", "modiversary")

        fun parseUserNotice(
            message: IrcMessage,
            findChannel: (UserId) -> UserName?,
        ): UserNoticeMessage? = with(message) {
            var msgId = tags["msg-id"]
            var mirrored = msgId == "sharedchatnotice"
            if (mirrored) {
                msgId = tags["source-msg-id"]
            } else {
                val roomId = tags["room-id"]
                val sourceRoomId = tags["source-room-id"]
                if (roomId != null && sourceRoomId != null) {
                    mirrored = roomId != sourceRoomId
                }
            }

            if (mirrored && msgId != "announcement") {
                return null
            }

            val id = tags["id"] ?: UUID.randomUUID().toString()
            val channel = params[0].substring(1)
            val defaultMessage = tags["system-msg"].orEmpty()
            val systemMsg =
                when {
                    msgId == "announcement" -> {
                        "Announcement"
                    }

                    msgId == "bitsbadgetier" -> {
                        val displayName = tags["display-name"]
                        val bitAmount = tags["msg-param-threshold"]?.toIntOrNull()
                        when {
                            displayName != null && bitAmount != null -> "$displayName just earned a new ${bitAmount / 1000}K Bits badge!"
                            else -> defaultMessage
                        }
                    }

                    msgId == "modiversary" -> {
                        // system-msg does not contain the username, prepend it manually
                        val displayName = tags["display-name"] ?: tags["login"].orEmpty()
                        "$displayName $defaultMessage".trim()
                    }

                    else -> {
                        defaultMessage
                    }
                }
            val ts = tags["tmi-sent-ts"]?.toLongOrNull() ?: System.currentTimeMillis()

            val childMessage =
                when (msgId) {
                    in USER_NOTICE_MSG_IDS_WITH_MESSAGE -> PrivMessage.parsePrivMessage(message, findChannel)
                    else -> null
                }

            return UserNoticeMessage(
                timestamp = ts,
                id = id,
                channel = channel.toUserName(),
                message = systemMsg,
                childMessage = childMessage?.takeIf { it.message.isNotBlank() },
                tags = tags,
            )
        }
    }
}

// TODO split into different user notice message types
val UserNoticeMessage.isSub: Boolean
    get() = tags["msg-id"].let { it != "announcement" && it !in UserNoticeMessage.MILESTONE_MSG_IDS }

val UserNoticeMessage.isAnnouncement: Boolean
    get() = tags["msg-id"] == "announcement"

val UserNoticeMessage.announcementColor: AnnouncementColor
    get() = AnnouncementColor.parse(tags["msg-param-color"])

val UserNoticeMessage.isMilestone: Boolean
    get() = tags["msg-id"] in UserNoticeMessage.MILESTONE_MSG_IDS
