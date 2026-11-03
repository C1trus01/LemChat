package com.flxrs.dankchat.ui.chat.search

object ChatSearchFilterParser {
    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun parse(query: String): List<ChatSearchFilter> {
        if (query.isBlank()) return emptyList()

        val tokens = query.trim().split(WHITESPACE_REGEX)
        val lastTokenIncomplete = !query.endsWith(' ')

        return tokens.mapIndexedNotNull { index, token ->
            val isBeingTyped = lastTokenIncomplete && index == tokens.lastIndex
            parseToken(token, isBeingTyped)
        }
    }

    private fun parseToken(
        token: String,
        isBeingTyped: Boolean,
    ): ChatSearchFilter? {
        if (token.isBlank()) return null

        val (negate, raw) = extractNegation(token)
        val colonIndex = raw.indexOf(':')

        if (colonIndex > 0) {
            val prefix = raw.substring(0, colonIndex).lowercase()
            val value = raw.substring(colonIndex + 1)

            when (prefix) {
                "from" -> return when {
                    isBeingTyped || value.isEmpty() -> null
                    else -> ChatSearchFilter.Author(name = value, negate = negate)
                }

                "has" -> return when (value.lowercase()) {
                    "link" -> {
                        ChatSearchFilter.HasLink(negate = negate)
                    }

                    "emote" -> {
                        ChatSearchFilter.HasEmote(emoteName = null, negate = negate)
                    }

                    "" -> {
                        null
                    }

                    else -> {
                        when {
                            isBeingTyped -> null
                            else -> ChatSearchFilter.HasEmote(emoteName = value, negate = negate)
                        }
                    }
                }

                "badge" -> return when {
                    isBeingTyped || value.isEmpty() -> null
                    else -> ChatSearchFilter.BadgeFilter(badgeName = value.lowercase(), negate = negate)
                }
            }
        }

        return ChatSearchFilter.Text(query = raw, negate = negate)
    }

    private fun extractNegation(token: String): Pair<Boolean, String> = when {
        token.startsWith('!') || token.startsWith('-') -> true to token.substring(1)
        else -> false to token
    }
}
