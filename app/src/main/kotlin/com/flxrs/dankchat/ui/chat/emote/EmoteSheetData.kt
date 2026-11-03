package com.flxrs.dankchat.ui.chat.emote

import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmote
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import com.flxrs.dankchat.data.twitch.emote.toChatMessageEmoteType

data class EmoteSheetData(
    val id: String,
    val code: String,
    val url: String,
    val type: ChatMessageEmoteType,
)

fun ChatMessageEmote.toEmoteSheetData(): EmoteSheetData = EmoteSheetData(id = id, code = code, url = url, type = type)

fun GenericEmote.toEmoteSheetData(): EmoteSheetData = EmoteSheetData(id = id, code = code, url = url, type = emoteType.toChatMessageEmoteType())
