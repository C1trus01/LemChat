package com.flxrs.dankchat.ui.chat.suggestion

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.repo.emote.EmojiData
import com.flxrs.dankchat.data.twitch.emote.GenericEmote

@Immutable
sealed interface Suggestion {
    data class EmoteSuggestion(
        val emote: GenericEmote,
    ) : Suggestion {
        override fun toString() = emote.toString()
    }

    data class UserSuggestion(
        val name: DisplayName,
        val withLeadingAt: Boolean = false,
    ) : Suggestion {
        override fun toString() = if (withLeadingAt) "@$name" else name.toString()
    }

    data class EmojiSuggestion(
        val emoji: EmojiData,
    ) : Suggestion {
        override fun toString() = emoji.unicode
    }

    data class CommandSuggestion(
        val command: String,
    ) : Suggestion {
        override fun toString() = command
    }

    data class FilterSuggestion(
        val keyword: String,
        @param:StringRes val descriptionRes: Int,
        val displayText: String? = null,
    ) : Suggestion {
        override fun toString() = keyword
    }
}
