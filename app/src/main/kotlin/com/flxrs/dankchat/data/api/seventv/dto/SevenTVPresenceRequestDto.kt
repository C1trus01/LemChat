package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVPresenceRequestDto(
    val kind: Int,
    val data: SevenTVPresenceDataDto,
)

@Keep
@Serializable
data class SevenTVPresenceDataDto(
    val id: String,
    val platform: String,
)
