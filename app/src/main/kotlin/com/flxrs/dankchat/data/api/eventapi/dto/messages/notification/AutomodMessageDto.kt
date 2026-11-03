package com.flxrs.dankchat.data.api.eventapi.dto.messages.notification

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * EventSub automod.message.hold v2 payload.
 * Fired when AutoMod catches a message for review.
 */
@Serializable
@SerialName("automod.message.hold")
data class AutomodMessageHoldDto(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: UserId,
    @SerialName("broadcaster_user_login")
    val broadcasterUserLogin: UserName,
    @SerialName("broadcaster_user_name")
    val broadcasterUserName: DisplayName,
    @SerialName("user_id")
    val userId: UserId,
    @SerialName("user_login")
    val userLogin: UserName,
    @SerialName("user_name")
    val userName: DisplayName,
    @SerialName("message_id")
    val messageId: String,
    val message: AutomodHeldMessageDto,
    @SerialName("held_at")
    val heldAt: String,
    // Discriminator string: "automod" or "blocked_term"
    val reason: String,
    // Present when reason == "automod"
    val automod: AutomodReasonDto? = null,
    // Present when reason == "blocked_term"
    @SerialName("blocked_term")
    val blockedTerm: BlockedTermReasonDto? = null,
) : NotificationEventDto

/**
 * EventSub automod.message.update v2 payload.
 * Fired when a held message is approved, denied, or expires.
 */
@Serializable
@SerialName("automod.message.update")
data class AutomodMessageUpdateDto(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: UserId,
    @SerialName("broadcaster_user_login")
    val broadcasterUserLogin: UserName,
    @SerialName("broadcaster_user_name")
    val broadcasterUserName: DisplayName,
    @SerialName("user_id")
    val userId: UserId,
    @SerialName("user_login")
    val userLogin: UserName,
    @SerialName("user_name")
    val userName: DisplayName,
    @SerialName("moderator_user_id")
    val moderatorUserId: String? = null,
    @SerialName("moderator_user_login")
    val moderatorUserLogin: String? = null,
    @SerialName("moderator_user_name")
    val moderatorUserName: String? = null,
    @SerialName("message_id")
    val messageId: String,
    val message: AutomodHeldMessageDto,
    val status: AutomodMessageStatus,
    @SerialName("held_at")
    val heldAt: String,
    val reason: String,
    val automod: AutomodReasonDto? = null,
    @SerialName("blocked_term")
    val blockedTerm: BlockedTermReasonDto? = null,
) : NotificationEventDto

/**
 * EventSub channel.chat.user_message_hold v1 payload.
 * Fired when the logged-in user's message is caught by AutoMod.
 */
@Serializable
@SerialName("channel.chat.user_message_hold")
data class ChannelChatUserMessageHoldDto(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: UserId,
    @SerialName("broadcaster_user_login")
    val broadcasterUserLogin: UserName,
    @SerialName("broadcaster_user_name")
    val broadcasterUserName: DisplayName,
    @SerialName("user_id")
    val userId: UserId,
    @SerialName("user_login")
    val userLogin: UserName,
    @SerialName("user_name")
    val userName: DisplayName,
    @SerialName("message_id")
    val messageId: String,
    val message: AutomodHeldMessageDto,
) : NotificationEventDto

/**
 * EventSub channel.chat.user_message_update v1 payload.
 * Fired when the logged-in user's held message is accepted or denied.
 */
@Serializable
@SerialName("channel.chat.user_message_update")
data class ChannelChatUserMessageUpdateDto(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: UserId,
    @SerialName("broadcaster_user_login")
    val broadcasterUserLogin: UserName,
    @SerialName("broadcaster_user_name")
    val broadcasterUserName: DisplayName,
    @SerialName("user_id")
    val userId: UserId,
    @SerialName("user_login")
    val userLogin: UserName,
    @SerialName("user_name")
    val userName: DisplayName,
    val status: AutomodMessageStatus,
    @SerialName("message_id")
    val messageId: String,
    val message: AutomodHeldMessageDto,
) : NotificationEventDto

@Serializable
data class AutomodHeldMessageDto(
    val text: String,
    val fragments: List<AutomodMessageFragmentDto> = emptyList(),
)

@Serializable
data class AutomodMessageFragmentDto(
    val text: String,
    val type: String, // "text", "emote", "cheermote"
)

@Serializable
data class AutomodReasonDto(
    val category: String,
    val level: Int,
)

@Serializable
data class BlockedTermReasonDto(
    @SerialName("terms_found")
    val termsFound: List<BlockedTermFoundDto>,
)

@Serializable
data class BlockedTermFoundDto(
    @SerialName("term_id")
    val termId: String,
    val boundary: AutomodBoundaryDto,
    @SerialName("owner_broadcaster_user_id")
    val ownerBroadcasterUserId: String? = null,
    @SerialName("owner_broadcaster_user_login")
    val ownerBroadcasterUserLogin: String? = null,
    @SerialName("owner_broadcaster_user_name")
    val ownerBroadcasterUserName: String? = null,
)

@Serializable
data class AutomodBoundaryDto(
    @SerialName("start_pos")
    val startPos: Int,
    @SerialName("end_pos")
    val endPos: Int,
)

@Serializable
enum class AutomodMessageStatus {
    @SerialName("approved")
    Approved,

    @SerialName("denied")
    Denied,

    @SerialName("expired")
    Expired,
}
