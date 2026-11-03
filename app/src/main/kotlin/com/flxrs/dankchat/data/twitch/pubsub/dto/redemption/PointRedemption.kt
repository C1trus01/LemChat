package com.flxrs.dankchat.data.twitch.pubsub.dto.redemption

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Keep
@Serializable
data class PointRedemption(
    val redemption: PointRedemptionData,
    val timestamp: Instant,
)
