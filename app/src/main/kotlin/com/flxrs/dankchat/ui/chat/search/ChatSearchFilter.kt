package com.flxrs.dankchat.ui.chat.search

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ChatSearchFilter {
    val negate: Boolean

    data class Text(
        val query: String,
        override val negate: Boolean = false,
    ) : ChatSearchFilter

    data class Author(
        val name: String,
        override val negate: Boolean = false,
    ) : ChatSearchFilter

    data class HasLink(
        override val negate: Boolean = false,
    ) : ChatSearchFilter

    data class HasEmote(
        val emoteName: String?,
        override val negate: Boolean = false,
    ) : ChatSearchFilter

    data class BadgeFilter(
        val badgeName: String,
        override val negate: Boolean = false,
    ) : ChatSearchFilter
}
