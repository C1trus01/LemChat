package com.flxrs.dankchat.utils.extensions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class StringExtensionsTest {
    @Test
    fun `codePointSlice returns substring for valid ascii range`() {
        assertEquals(expected = "bad", actual = "this is a bad message".codePointSlice(10, 13))
    }

    @Test
    fun `codePointSlice slices full string`() {
        assertEquals(expected = "automod", actual = "automod".codePointSlice(0, 7))
    }

    @Test
    fun `codePointSlice counts surrogate pairs as single code points`() {
        // each emoji is one code point but two utf-16 chars
        assertEquals(expected = "bad", actual = "🐍🐍 bad".codePointSlice(3, 6))
    }

    @Test
    fun `codePointSlice returns null when begin is past the end`() {
        // begin 14 on a string of length 13, real automod boundary from a play console crash
        assertEquals(expected = null, actual = "thirteenchars".codePointSlice(14, 15))
    }

    @Test
    fun `codePointSlice returns null when end is past the end`() {
        assertEquals(expected = null, actual = "short".codePointSlice(2, 10))
    }

    @Test
    fun `codePointSlice returns null for degenerate ranges`() {
        assertEquals(expected = null, actual = "text".codePointSlice(-1, 2))
        assertEquals(expected = null, actual = "text".codePointSlice(2, 2))
        assertEquals(expected = null, actual = "text".codePointSlice(3, 1))
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup leaves text without emoji untouched`() {
        assertEquals(expected = "NaM forsenE Keepo" to emptyList(), actual = "NaM forsenE Keepo".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup separates classic emoji from adjacent text`() {
        assertEquals(expected = "NaM 😂 NaM" to listOf(3, 5), actual = "NaM😂NaM".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup keeps zwj sequence as a single group`() {
        assertEquals(expected = "NaM 🙅🏻‍♂️ NaM" to listOf(3, 10), actual = "NaM🙅🏻‍♂️NaM".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup separates flag emoji from adjacent text`() {
        assertEquals(expected = "NaM 🇩🇪 NaM" to listOf(3, 7), actual = "NaM🇩🇪NaM".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup matches unicode 16 emoji`() {
        // U+1FAE9 face with bags under eyes
        assertEquals(expected = "NaM 🫩 NaM" to listOf(3, 5), actual = "NaM🫩NaM".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup matches unicode 17 emoji`() {
        // U+1FAEA distorted face
        assertEquals(expected = "NaM 🫪 NaM" to listOf(3, 5), actual = "NaM🫪NaM".appendSpacesBetweenEmojiGroup())
    }

    @Test
    fun `analyzeCodePoints records supplementary positions in deduplicated codepoint coordinates`() {
        // "a   😂 Kappa": duplicate spaces at codepoints 2 and 3 are removed, the emoji lands
        // at codepoint 2 of the deduplicated string "a 😂 Kappa"
        val result = "a   😂 Kappa".analyzeCodePoints()
        assertEquals(expected = "a 😂 Kappa", actual = result.deduplicatedString)
        assertEquals(expected = listOf(2, 3), actual = result.removedSpacesPositions)
        assertEquals(expected = listOf(2), actual = result.supplementaryCodePointPositions)
    }

    @Test
    fun `analyzeCodePoints keeps supplementary positions without whitespace deduplication`() {
        val result = "🐍🐍 Kappa 🐍".analyzeCodePoints()
        assertEquals(expected = "🐍🐍 Kappa 🐍", actual = result.deduplicatedString)
        assertEquals(expected = emptyList(), actual = result.removedSpacesPositions)
        assertEquals(expected = listOf(0, 1, 9), actual = result.supplementaryCodePointPositions)
    }

    @Test
    fun `analyzeCodePoints adjusts each supplementary position by the removals before it`() {
        // "😂  😂 x": the duplicate space at codepoint 2 shifts the second emoji to codepoint 2
        val result = "😂  😂 x".analyzeCodePoints()
        assertEquals(expected = "😂 😂 x", actual = result.deduplicatedString)
        assertEquals(expected = listOf(2), actual = result.removedSpacesPositions)
        assertEquals(expected = listOf(0, 2), actual = result.supplementaryCodePointPositions)
    }

    @Test
    fun `appendSpacesBetweenEmojiGroup ignores invisible and control characters`() {
        // U+034F combining grapheme joiner, appended to bypass duplicate message detection
        val withInvisibleChar = "NaM $INVISIBLE_CHAR"
        assertEquals(expected = withInvisibleChar to emptyList(), actual = withInvisibleChar.appendSpacesBetweenEmojiGroup())

        // U+E0002 tag character, used by EmoteRepository to escape emote names
        val withEscapeTag = "NaM\uDB40\uDC02NaM"
        assertEquals(expected = withEscapeTag to emptyList(), actual = withEscapeTag.appendSpacesBetweenEmojiGroup())

        val withStandaloneZwj = "NaM\u200DNaM"
        assertEquals(expected = withStandaloneZwj to emptyList(), actual = withStandaloneZwj.appendSpacesBetweenEmojiGroup())

        val withStandaloneVariationSelector = "NaM\uFE0FNaM"
        assertEquals(expected = withStandaloneVariationSelector to emptyList(), actual = withStandaloneVariationSelector.appendSpacesBetweenEmojiGroup())
    }
}
