package com.flxrs.dankchat.data.api.helix.dto

import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendChatMessageRequestDto(
    @SerialName("broadcaster_id") val broadcasterId: UserId,
    @SerialName("sender_id") val senderId: UserId,
    val message: String,
    @SerialName("reply_parent_message_id") val replyParentMessageId: String? = null,
)

@Serializable
data class SendChatMessageResponseDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("is_sent") val isSent: Boolean,
    @SerialName("drop_reason") val dropReason: DropReasonDto? = null,
)

@Serializable
data class DropReasonDto(
    val code: String,
    val message: String,
)
