package com.flxrs.dankchat.data.api.eventapi

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodMessageHoldDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.AutomodMessageUpdateDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.ChannelChatUserMessageHoldDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.ChannelChatUserMessageUpdateDto
import com.flxrs.dankchat.data.api.eventapi.dto.messages.notification.ChannelModerateDto
import kotlin.time.Instant

sealed interface EventSubMessage

data class SystemMessage(
    val message: String,
) : EventSubMessage

data class ModerationAction(
    val id: String,
    val timestamp: Instant,
    val channelName: UserName,
    val data: ChannelModerateDto,
) : EventSubMessage

data class AutomodHeld(
    val id: String,
    val timestamp: Instant,
    val channelName: UserName,
    val data: AutomodMessageHoldDto,
) : EventSubMessage

data class AutomodUpdate(
    val id: String,
    val timestamp: Instant,
    val channelName: UserName,
    val data: AutomodMessageUpdateDto,
) : EventSubMessage

data class UserMessageHeld(
    val id: String,
    val timestamp: Instant,
    val channelName: UserName,
    val data: ChannelChatUserMessageHoldDto,
) : EventSubMessage

data class UserMessageUpdated(
    val id: String,
    val timestamp: Instant,
    val channelName: UserName,
    val data: ChannelChatUserMessageUpdateDto,
) : EventSubMessage
