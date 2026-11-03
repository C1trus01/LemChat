package com.flxrs.dankchat.data.twitch.emote

import androidx.annotation.ColorInt

data class CheermoteSet(
    val prefix: String,
    val regex: Regex,
    val tiers: List<CheermoteTier>,
)

data class CheermoteTier(
    val minBits: Int,
    @param:ColorInt val color: Int,
    val animatedUrl: String,
    val staticUrl: String,
)
