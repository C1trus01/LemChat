package com.flxrs.dankchat.data.api.helix.dto

import com.flxrs.dankchat.data.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManageAutomodMessageRequestDto(
    @SerialName("user_id")
    val userId: UserId,
    @SerialName("msg_id")
    val msgId: String,
    val action: String,
)
