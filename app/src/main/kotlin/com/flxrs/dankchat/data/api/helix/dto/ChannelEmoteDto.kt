package com.flxrs.dankchat.data.api.helix.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ChannelEmoteDto(
    @SerialName(value = "id") val id: String,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "emote_type") val emoteType: String,
    @SerialName(value = "emote_set_id") val emoteSetId: String,
)
