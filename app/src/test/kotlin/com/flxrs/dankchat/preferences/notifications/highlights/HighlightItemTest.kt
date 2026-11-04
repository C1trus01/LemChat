package com.flxrs.dankchat.preferences.notifications.highlights

import com.flxrs.dankchat.data.database.entity.MessageHighlightEntity
import com.flxrs.dankchat.data.database.entity.MessageHighlightEntityType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HighlightItemTest {
    private fun messageItem(
        pattern: String,
        isRegex: Boolean = false,
    ) = MessageHighlightItem(
        id = 1,
        enabled = true,
        type = MessageHighlightItem.Type.Custom,
        pattern = pattern,
        isRegex = isRegex,
        isCaseSensitive = false,
        createNotification = false,
        loggedIn = true,
        notificationsEnabled = true,
        customColor = null,
    )

    private fun savedEntity(pattern: String) = MessageHighlightEntity(
        id = 1,
        enabled = true,
        type = MessageHighlightEntityType.Custom,
        pattern = pattern,
    )

    @Test
    fun `saving a word removes spaces around it`() {
        assertEquals("цитрус", messageItem(" цитрус ").toEntity().pattern)
    }

    @Test
    fun `saving a regex keeps its spaces`() {
        assertEquals("a b ", messageItem("a b ", isRegex = true).toEntity().pattern)
    }

    @Test
    fun `saving a user or badge removes spaces around it`() {
        val user = UserHighlightItem(id = 1, enabled = true, username = "forsen ", createNotification = false, notificationsEnabled = true, customColor = null)
        val badge = BadgeHighlightItem(id = 1, enabled = true, badgeName = "vip ", isCustom = true, customColor = null, createNotification = false, notificationsEnabled = true)
        val ignored = BlacklistedUserItem(id = 1, enabled = true, username = "bot ", isRegex = false)
        assertEquals("forsen", user.toEntity().username)
        assertEquals("vip", badge.toEntity().badgeName)
        assertEquals("bot", ignored.toEntity().username)
    }

    @Test
    fun `an entry saved earlier with a trailing space still matches at the end of a message`() {
        val regex = checkNotNull(savedEntity("цитрус ").regex)
        assertTrue("привет цитрус".contains(regex))
        assertTrue("ЦИТРУС, привет".contains(regex))
    }

    @Test
    fun `a word does not match inside a longer word`() {
        val regex = checkNotNull(savedEntity("dank").regex)
        assertFalse("dankest".contains(regex))
    }
}
