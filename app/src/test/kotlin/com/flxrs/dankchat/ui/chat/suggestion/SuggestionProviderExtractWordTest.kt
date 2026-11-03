package com.flxrs.dankchat.ui.chat.suggestion

import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SuggestionProviderExtractWordTest {
    private val suggestionProvider =
        SuggestionProvider(
            emoteRepository = mockk(),
            usersRepository = mockk(),
            commandRepository = mockk(),
            emoteUsageRepository = mockk(),
            emojiRepository = mockk(),
        )

    @Test
    fun `cursor at end of single word returns full word`() {
        val result = suggestionProvider.extractCurrentWord("asd", cursorPosition = 3)
        assertEquals(expected = "asd", actual = result)
    }

    @Test
    fun `cursor at start of text returns empty string`() {
        val result = suggestionProvider.extractCurrentWord("asd asdasd", cursorPosition = 0)
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `cursor at end of first word returns first word`() {
        val result = suggestionProvider.extractCurrentWord("asd asdasd", cursorPosition = 3)
        assertEquals(expected = "asd", actual = result)
    }

    @Test
    fun `cursor at end of second word returns second word`() {
        val result = suggestionProvider.extractCurrentWord("asd asdasd", cursorPosition = 10)
        assertEquals(expected = "asdasd", actual = result)
    }

    @Test
    fun `cursor in middle of word returns partial word up to cursor`() {
        val result = suggestionProvider.extractCurrentWord("hello world", cursorPosition = 8)
        assertEquals(expected = "wo", actual = result)
    }

    @Test
    fun `cursor right after space returns empty string`() {
        val result = suggestionProvider.extractCurrentWord("hello world", cursorPosition = 6)
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `cursor at space between words returns first word`() {
        val result = suggestionProvider.extractCurrentWord("hello world", cursorPosition = 5)
        assertEquals(expected = "hello", actual = result)
    }

    @Test
    fun `handles at-mention prefix`() {
        val result = suggestionProvider.extractCurrentWord("hello @us", cursorPosition = 9)
        assertEquals(expected = "@us", actual = result)
    }

    @Test
    fun `handles slash command prefix`() {
        val result = suggestionProvider.extractCurrentWord("/ti", cursorPosition = 3)
        assertEquals(expected = "/ti", actual = result)
    }

    @Test
    fun `cursor position clamped to text length`() {
        val result = suggestionProvider.extractCurrentWord("abc", cursorPosition = 100)
        assertEquals(expected = "abc", actual = result)
    }

    @Test
    fun `cursor position clamped to zero`() {
        val result = suggestionProvider.extractCurrentWord("abc", cursorPosition = -5)
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `empty text returns empty string`() {
        val result = suggestionProvider.extractCurrentWord("", cursorPosition = 0)
        assertEquals(expected = "", actual = result)
    }

    @Test
    fun `cursor mid-text with multiple words returns word being typed`() {
        // "one t<cursor>wo three" — cursor at position 5, word starts at 4
        val result = suggestionProvider.extractCurrentWord("one two three", cursorPosition = 5)
        assertEquals(expected = "t", actual = result)
    }

    @Test
    fun `typing at beginning of existing text`() {
        // User typed "asd" before existing "asd asdasd": "asd<cursor>asd asdasd"
        val result = suggestionProvider.extractCurrentWord("asdasd asdasd", cursorPosition = 3)
        assertEquals(expected = "asd", actual = result)
    }
}
