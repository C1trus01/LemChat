package com.flxrs.dankchat.ui.chat.suggestion

import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SuggestionScoringTest {
    private val provider =
        SuggestionProvider(
            emoteRepository = mockk(),
            usersRepository = mockk(),
            commandRepository = mockk(),
            emoteUsageRepository = mockk(),
            emojiRepository = mockk(),
        )

    @Test
    fun `exact full match scores lowest`() {
        val score = provider.scoreEmote("Pog", "Pog", isRecentlyUsed = false)
        assertEquals(expected = -10, actual = score)
    }

    @Test
    fun `shorter match beats longer match regardless of case`() {
        val shorter = provider.scoreEmote("Wink", "wi", isRecentlyUsed = false)
        val longer = provider.scoreEmote("wikked", "wi", isRecentlyUsed = false)
        assertTrue(shorter < longer)
    }

    @Test
    fun `exact case beats case mismatch at same length`() {
        val exactCase = provider.scoreEmote("wink", "wi", isRecentlyUsed = false)
        val caseMismatch = provider.scoreEmote("Wink", "wi", isRecentlyUsed = false)
        assertTrue(exactCase < caseMismatch)
    }

    @Test
    fun `recently used emote gets boost`() {
        val notRecent = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = false)
        val recent = provider.scoreEmote("PogChamp", "Pog", isRecentlyUsed = true)
        assertTrue(recent < notRecent)
    }

    @Test
    fun `no match returns NO_MATCH`() {
        val score = provider.scoreEmote("Kappa", "Pog", isRecentlyUsed = false)
        assertEquals(SuggestionProvider.NO_MATCH, score)
    }

    @Test
    fun `substring match has same extra chars cost as prefix`() {
        val prefix = provider.scoreEmote("wink", "wi", isRecentlyUsed = false)
        val substring = provider.scoreEmote("owie", "wi", isRecentlyUsed = false)
        // Both have 2 extra chars, same case match → same score
        assertEquals(prefix, substring)
    }

    @Test
    fun `multiple case diffs add up`() {
        val noDiff = provider.scoreEmote("pog", "pog", isRecentlyUsed = false)
        val twoDiffs = provider.scoreEmote("POG", "pog", isRecentlyUsed = false)
        assertTrue(noDiff < twoDiffs)
        // noDiff = -10, twoDiffs = 3 (three case diffs)
        assertEquals(-10, noDiff)
        assertEquals(3, twoDiffs)
    }
}
