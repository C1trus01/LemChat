package com.flxrs.dankchat.data.api.helix.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.shared.dto.MessageFragmentDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Keep
@Serializable
data class PinnedChatMessageDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_user_id") val senderUserId: UserId,
    @SerialName("sender_user_login") val senderUserLogin: UserName,
    @SerialName("sender_user_name") val senderUserName: DisplayName,
    @SerialName("pinned_by_user_id") val pinnedByUserId: UserId,
    @SerialName("pinned_by_user_login") val pinnedByUserLogin: UserName,
    @SerialName("pinned_by_user_name") val pinnedByUserName: DisplayName,
    @SerialName("message") val message: PinnedChatMessageContentDto,
    @SerialName("starts_at") val startsAt: Instant,
    // May be missing or an empty string for pins without an end time
    @SerialName("ends_at") val endsAt: String? = null,
)

@Keep
@Serializable
data class PinnedChatMessageContentDto(
    @SerialName("text") val text: String,
    @SerialName("fragments") val fragments: List<MessageFragmentDto> = emptyList(),
)
