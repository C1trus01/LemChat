package com.flxrs.dankchat.data.api.shared.dto

import androidx.annotation.Keep
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.message.EmoteWithPositions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured chat message fragment as used by Helix (e.g. Get Pinned Chat Message) and EventSub
 * chat payloads, both share the same schema.
 */
@Keep
@Serializable
data class MessageFragmentDto(
    // Unknown fragment types are coerced to Text
    @SerialName("type") val type: MessageFragmentTypeDto = MessageFragmentTypeDto.Text,
    @SerialName("text") val text: String,
    @SerialName("emote") val emote: EmoteFragmentDto? = null,
    @SerialName("cheermote") val cheermote: CheermoteFragmentDto? = null,
    @SerialName("mention") val mention: MentionFragmentDto? = null,
)

@Keep
@Serializable
enum class MessageFragmentTypeDto {
    @SerialName("text")
    Text,

    @SerialName("emote")
    Emote,

    @SerialName("cheermote")
    Cheermote,

    @SerialName("mention")
    Mention,
}

@Keep
@Serializable
data class EmoteFragmentDto(
    @SerialName("id") val id: String,
    @SerialName("emote_set_id") val emoteSetId: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("format") val format: List<String> = emptyList(),
)

@Keep
@Serializable
data class CheermoteFragmentDto(
    @SerialName("prefix") val prefix: String,
    @SerialName("bits") val bits: Int,
    @SerialName("tier") val tier: Int,
)

@Keep
@Serializable
data class MentionFragmentDto(
    @SerialName("user_id") val userId: UserId,
    @SerialName("user_login") val userLogin: UserName,
    @SerialName("user_name") val userName: DisplayName,
)

/**
 * Extracts Twitch emotes from message fragments in the same convention as the IRC emotes tag:
 * code point indexed, inclusive ranges over the concatenated fragment text.
 */
fun List<MessageFragmentDto>.toEmotesWithPositions(): List<EmoteWithPositions> {
    val positionsById = LinkedHashMap<String, MutableList<IntRange>>()
    var codePointIndex = 0
    forEach { fragment ->
        val codePointLength = fragment.text.codePointCount(0, fragment.text.length)
        val emoteId = fragment.emote?.id
        if (fragment.type == MessageFragmentTypeDto.Emote && emoteId != null && codePointLength > 0) {
            positionsById.getOrPut(emoteId) { mutableListOf() } += codePointIndex..codePointIndex + codePointLength - 1
        }
        codePointIndex += codePointLength
    }
    return positionsById.map { (id, positions) -> EmoteWithPositions(id, positions) }
}
