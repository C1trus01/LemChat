package com.flxrs.dankchat.data.api.eventapi

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubBroadcasterUserConditionDto
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubMethod
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubModeratorConditionDto
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubSubscriptionRequestDto
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubSubscriptionType
import com.flxrs.dankchat.data.api.eventapi.dto.EventSubTransportDto

sealed interface EventSubTopic {
    fun createRequest(sessionId: String): EventSubSubscriptionRequestDto

    fun shortFormatted(): String

    data class ChannelModerate(
        val channel: UserName,
        val broadcasterId: UserId,
        val moderatorId: UserId,
    ) : EventSubTopic {
        override fun createRequest(sessionId: String) = EventSubSubscriptionRequestDto(
            type = EventSubSubscriptionType.ChannelModerate,
            version = "2",
            condition =
                EventSubModeratorConditionDto(
                    broadcasterUserId = broadcasterId,
                    moderatorUserId = moderatorId,
                ),
            transport =
                EventSubTransportDto(
                    sessionId = sessionId,
                    method = EventSubMethod.Websocket,
                ),
        )

        override fun shortFormatted(): String = "ChannelModerate($channel)"
    }

    data class AutomodMessageHold(
        val channel: UserName,
        val broadcasterId: UserId,
        val moderatorId: UserId,
    ) : EventSubTopic {
        override fun createRequest(sessionId: String) = EventSubSubscriptionRequestDto(
            type = EventSubSubscriptionType.AutomodMessageHold,
            version = "2",
            condition =
                EventSubModeratorConditionDto(
                    broadcasterUserId = broadcasterId,
                    moderatorUserId = moderatorId,
                ),
            transport =
                EventSubTransportDto(
                    sessionId = sessionId,
                    method = EventSubMethod.Websocket,
                ),
        )

        override fun shortFormatted(): String = "AutomodMessageHold($channel)"
    }

    data class AutomodMessageUpdate(
        val channel: UserName,
        val broadcasterId: UserId,
        val moderatorId: UserId,
    ) : EventSubTopic {
        override fun createRequest(sessionId: String) = EventSubSubscriptionRequestDto(
            type = EventSubSubscriptionType.AutomodMessageUpdate,
            version = "2",
            condition =
                EventSubModeratorConditionDto(
                    broadcasterUserId = broadcasterId,
                    moderatorUserId = moderatorId,
                ),
            transport =
                EventSubTransportDto(
                    sessionId = sessionId,
                    method = EventSubMethod.Websocket,
                ),
        )

        override fun shortFormatted(): String = "AutomodMessageUpdate($channel)"
    }

    data class UserMessageHold(
        val channel: UserName,
        val broadcasterId: UserId,
        val userId: UserId,
    ) : EventSubTopic {
        override fun createRequest(sessionId: String) = EventSubSubscriptionRequestDto(
            type = EventSubSubscriptionType.ChannelChatUserMessageHold,
            version = "1",
            condition =
                EventSubBroadcasterUserConditionDto(
                    broadcasterUserId = broadcasterId,
                    userId = userId,
                ),
            transport =
                EventSubTransportDto(
                    sessionId = sessionId,
                    method = EventSubMethod.Websocket,
                ),
        )

        override fun shortFormatted(): String = "UserMessageHold($channel)"
    }

    data class UserMessageUpdate(
        val channel: UserName,
        val broadcasterId: UserId,
        val userId: UserId,
    ) : EventSubTopic {
        override fun createRequest(sessionId: String) = EventSubSubscriptionRequestDto(
            type = EventSubSubscriptionType.ChannelChatUserMessageUpdate,
            version = "1",
            condition =
                EventSubBroadcasterUserConditionDto(
                    broadcasterUserId = broadcasterId,
                    userId = userId,
                ),
            transport =
                EventSubTransportDto(
                    sessionId = sessionId,
                    method = EventSubMethod.Websocket,
                ),
        )

        override fun shortFormatted(): String = "UserMessageUpdate($channel)"
    }
}

data class SubscribedTopic(
    val id: String,
    val topic: EventSubTopic,
)
