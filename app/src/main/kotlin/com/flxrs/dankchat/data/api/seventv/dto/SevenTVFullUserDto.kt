package com.flxrs.dankchat.data.api.seventv.dto

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SevenTVFullUserDto(
    val roles: List<String> = emptyList(),
    @SerialName("emote_sets") val emoteSets: List<SevenTVUserEmoteSetDto> = emptyList(),
) {
    /** A personal set outlives the subscription, only the role tells whether it can still be used. */
    val activePersonalEmoteSetId: String?
        get() = when (SUBSCRIBER_ROLE_ID) {
            in roles -> emoteSets.firstOrNull { it.flags and PERSONAL_FLAG != 0L }?.id
            else -> null
        }

    private companion object {
        const val SUBSCRIBER_ROLE_ID = "01F37R3RFR0000K96678WEQT01"
        const val PERSONAL_FLAG = 1L shl 2
    }
}

@Keep
@Serializable
data class SevenTVUserEmoteSetDto(
    val id: String,
    val flags: Long = 0,
)
