package com.flxrs.dankchat.ui.chat.emote

import androidx.annotation.StringRes
import com.flxrs.dankchat.data.DisplayName

data class EmoteInfoItem(
    val id: String,
    val name: String,
    val baseName: String?,
    val imageUrl: String,
    @param:StringRes val emoteType: Int,
    val providerUrl: String,
    val isZeroWidth: Boolean,
    val creatorName: DisplayName?,
)
