package com.flxrs.dankchat.data.api.bttv.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class BTTVUserBadgeDto(
    // The Twitch user id
    val providerId: String,
    val badge: BTTVBadgeDto,
)

@Keep
@Serializable
data class BTTVBadgeDto(
    val description: String? = null,
    val svg: String,
)
