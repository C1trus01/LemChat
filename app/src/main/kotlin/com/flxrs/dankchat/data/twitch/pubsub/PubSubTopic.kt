package com.flxrs.dankchat.data.twitch.pubsub

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName

sealed class PubSubTopic(
    val topic: String,
    val requiresAuth: Boolean = true,
) {
    abstract val channelName: UserName

    data class PointRedemptions(
        val channelId: UserId,
        override val channelName: UserName,
    ) : PubSubTopic(topic = "community-points-channel-v1.$channelId", requiresAuth = false)

    data class ModeratorActions(
        val userId: UserId,
        val channelId: UserId,
        override val channelName: UserName,
    ) : PubSubTopic(topic = "chat_moderator_actions.$userId.$channelId")

    data class PinnedChatUpdates(
        val channelId: UserId,
        override val channelName: UserName,
    ) : PubSubTopic(topic = "pinned-chat-updates-v1.$channelId", requiresAuth = false)
}
