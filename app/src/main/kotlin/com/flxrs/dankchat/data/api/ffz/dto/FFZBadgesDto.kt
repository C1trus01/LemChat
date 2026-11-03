package com.flxrs.dankchat.data.api.ffz.dto

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class FFZBadgesDto(
    val badges: List<FFZBadgeDto> = emptyList(),
    // Twitch user ids per badge id
    val users: Map<String, List<Long>> = emptyMap(),
)

@Keep
@Serializable
data class FFZBadgeDto(
    val id: Long,
    val title: String? = null,
    val color: String? = null,
    val urls: Map<String, String> = emptyMap(),
) {
    val imageUrl: String? get() = urls["4"] ?: urls["2"] ?: urls["1"]

    /** The image is only a shape, this color is what it sits on. */
    val backgroundColor: Int?
        get() {
            val hex = color?.removePrefix("#") ?: return null
            if (hex.length != 6) {
                return null
            }
            return hex.toLongOrNull(radix = 16)?.let { (OPAQUE or it).toInt() }
        }

    private companion object {
        const val OPAQUE = 0xFF000000L
    }
}
