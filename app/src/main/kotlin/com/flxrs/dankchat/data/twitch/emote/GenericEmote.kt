package com.flxrs.dankchat.data.twitch.emote

data class GenericEmote(
    val code: String,
    val url: String,
    val lowResUrl: String,
    val id: String,
    val scale: Int,
    val emoteType: EmoteType,
    val isOverlayEmote: Boolean = false,
) : Comparable<GenericEmote> {
    override fun toString(): String = code

    override fun compareTo(other: GenericEmote): Int = code.compareTo(other.code)
}
