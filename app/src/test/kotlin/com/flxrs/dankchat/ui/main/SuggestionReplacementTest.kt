package com.flxrs.dankchat.ui.main

import com.flxrs.dankchat.ui.main.input.SuggestionReplacementResult
import com.flxrs.dankchat.ui.main.input.computeSuggestionReplacement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SuggestionReplacementTest {
    @Test
    fun `replaces word at end of text`() {
        val result = computeSuggestionReplacement("hello as", cursorPos = 8, suggestionText = "asd")
        assertEquals(expected = 6, actual = result.replaceStart)
        assertEquals(expected = 8, actual = result.replaceEnd)
        assertEquals(expected = "asd ", actual = result.replacement)
        assertEquals(expected = 10, actual = result.newCursorPos)

        val newText = "hello as".replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "hello asd ", actual = newText)
    }

    @Test
    fun `replaces typed portion at beginning, preserves text after cursor`() {
        // "asd<cursor>asd asdasd" -> typing "asd" before existing text
        val text = "asdasd asdasd"
        val result = computeSuggestionReplacement(text, cursorPos = 3, suggestionText = "asd")
        assertEquals(expected = 0, actual = result.replaceStart)
        assertEquals(expected = 3, actual = result.replaceEnd)
        assertEquals(expected = "asd ", actual = result.replacement)

        val newText = text.replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "asd asd asdasd", actual = newText)
        assertEquals(expected = 4, actual = result.newCursorPos)
    }

    @Test
    fun `replaces typed portion mid-text`() {
        // "hello as<cursor> world"
        val text = "hello as world"
        val result = computeSuggestionReplacement(text, cursorPos = 8, suggestionText = "asd")

        val newText = text.replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "hello asd  world", actual = newText)
        assertEquals(expected = 10, actual = result.newCursorPos)
    }

    @Test
    fun `replaces at cursor position 0 with no preceding text`() {
        val text = "existing"
        val result = computeSuggestionReplacement(text, cursorPos = 0, suggestionText = "new")
        assertEquals(expected = 0, actual = result.replaceStart)
        assertEquals(expected = 0, actual = result.replaceEnd)
        assertEquals(expected = "new ", actual = result.replacement)

        val newText = text.replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "new existing", actual = newText)
    }

    @Test
    fun `replaces at-mention typed mid-text`() {
        // "hello @us<cursor> more text"
        val text = "hello @us more text"
        val result = computeSuggestionReplacement(text, cursorPos = 9, suggestionText = "@user123")

        val newText = text.replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "hello @user123  more text", actual = newText)
        assertEquals(expected = 15, actual = result.newCursorPos)
    }

    @Test
    fun `single word fully typed at end`() {
        val result = computeSuggestionReplacement("Kappa", cursorPos = 5, suggestionText = "Kappa")

        val newText = "Kappa".replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "Kappa ", actual = newText)
        assertEquals(expected = 6, actual = result.newCursorPos)
    }

    @Test
    fun `replacement includes trailing space`() {
        val result = computeSuggestionReplacement("he", cursorPos = 2, suggestionText = "hello")
        assertEquals(expected = "hello ", actual = result.replacement)
    }

    @Test
    fun `cursor after space replaces nothing before suggestion`() {
        // "hello <cursor>world" — cursor right after space, no typed chars
        val text = "hello world"
        val result = computeSuggestionReplacement(text, cursorPos = 6, suggestionText = "emote")
        assertEquals(expected = 6, actual = result.replaceStart)
        assertEquals(expected = 6, actual = result.replaceEnd)

        val newText = text.replaceRange(result.replaceStart, result.replaceEnd, result.replacement)
        assertEquals(expected = "hello emote world", actual = newText)
    }
}
