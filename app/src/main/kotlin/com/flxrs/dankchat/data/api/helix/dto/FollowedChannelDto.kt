package com.flxrs.dankchat.data.api.helix.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FollowedChannelDto(
    @SerialName(value = "broadcaster_id") val broadcasterId: UserId,
    @SerialName(value = "followed_at") val followedAt: String,
)
