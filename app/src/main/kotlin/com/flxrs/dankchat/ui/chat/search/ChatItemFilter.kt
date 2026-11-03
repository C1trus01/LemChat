package com.flxrs.dankchat.ui.chat.search

import com.flxrs.dankchat.data.chat.ChatItem
import com.flxrs.dankchat.data.twitch.message.ModerationMessage
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.data.twitch.message.UserNoticeMessage

object ChatItemFilter {
    private val URL_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

    fun matches(
        item: ChatItem,
        filters: List<ChatSearchFilter>,
    ): Boolean {
        if (filters.isEmpty()) return true
        return filters.all { filter ->
            val result =
                when (filter) {
                    is ChatSearchFilter.Text -> matchText(item, filter.query)
                    is ChatSearchFilter.Author -> matchAuthor(item, filter.name)
                    is ChatSearchFilter.HasLink -> matchLink(item)
                    is ChatSearchFilter.HasEmote -> matchEmote(item, filter.emoteName)
                    is ChatSearchFilter.BadgeFilter -> matchBadge(item, filter.badgeName)
                }
            if (filter.negate) !result else result
        }
    }

    private fun matchText(
        item: ChatItem,
        query: String,
    ): Boolean = when (val message = item.message) {
        is PrivMessage ->
            message.message.contains(query, ignoreCase = true) ||
                message.name.value.contains(query, ignoreCase = true) ||
                message.displayName.value.contains(query, ignoreCase = true)

        is UserNoticeMessage ->
            message.message.contains(query, ignoreCase = true) ||
                message.childMessage
                    ?.name
                    ?.value
                    ?.contains(query, ignoreCase = true) == true ||
                message.childMessage
                    ?.displayName
                    ?.value
                    ?.contains(query, ignoreCase = true) == true

        else -> false
    }

    private fun matchAuthor(
        item: ChatItem,
        name: String,
    ): Boolean = when (val message = item.message) {
        is PrivMessage -> {
            message.name.value.equals(name, ignoreCase = true) ||
                message.displayName.value.equals(name, ignoreCase = true)
        }

        is ModerationMessage -> {
            message.targetUser?.value?.equals(name, ignoreCase = true) == true ||
                message.targetUserDisplay?.value?.equals(name, ignoreCase = true) == true
        }

        else -> {
            false
        }
    }

    private fun matchLink(item: ChatItem): Boolean = when (val message = item.message) {
        is PrivMessage -> URL_REGEX.containsMatchIn(message.message)
        else -> false
    }

    private fun matchEmote(
        item: ChatItem,
        emoteName: String?,
    ): Boolean = when (val message = item.message) {
        is PrivMessage -> {
            when (emoteName) {
                null -> message.emotes.isNotEmpty()
                else -> message.emotes.any { it.code.equals(emoteName, ignoreCase = true) }
            }
        }

        else -> {
            false
        }
    }

    private fun matchBadge(
        item: ChatItem,
        badgeName: String,
    ): Boolean = when (val message = item.message) {
        is PrivMessage -> {
            message.badges.any { badge ->
                badge.badgeTag?.substringBefore('/')?.equals(badgeName, ignoreCase = true) == true ||
                    badge.title?.contains(badgeName, ignoreCase = true) == true
            }
        }

        else -> {
            false
        }
    }
}
