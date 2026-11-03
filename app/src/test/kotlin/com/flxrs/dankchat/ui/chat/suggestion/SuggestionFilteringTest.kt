package com.flxrs.dankchat.ui.chat.suggestion

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.repo.emote.EmojiData
import com.flxrs.dankchat.data.twitch.emote.EmoteType
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SuggestionFilteringTest {
    private val provider =
        SuggestionProvider(
            emoteRepository = mockk(),
            usersRepository = mockk(),
            commandRepository = mockk(),
            emoteUsageRepository = mockk(),
            emojiRepository = mockk(),
        )

    private fun emote(
        code: String,
        id: String = code,
    ) = GenericEmote(code = code, url = "", lowResUrl = "", id = id, scale = 1, emoteType = EmoteType.GlobalTwitchEmote)

    @Test
    fun `emotes sorted by score - shorter before longer`() {
        val emotes = listOf(emote("PogChamp"), emote("PogU"), emote("Pog"))
        val result = provider.filterEmotesScored(emotes, "Pog", emptySet())

        assertEquals(
            expected = listOf("Pog", "PogU", "PogChamp"),
            actual = result.map { (it.suggestion as Suggestion.EmoteSuggestion).emote.code },
        )
    }

    @Test
    fun `emotes sorted by score - exact case beats case mismatch at same length`() {
        val emotes = listOf(emote("POGX"), emote("PogX"))
        val result = provider.filterEmotesScored(emotes, "Pog", emptySet())

        // PogX: 1 case diff + 1*100 = 101, POGX: 2 case diffs + 1*100 = 102
        assertEquals(
            expected = listOf("PogX", "POGX"),
            actual = result.map { (it.suggestion as Suggestion.EmoteSuggestion).emote.code },
        )
    }

    @Test
    fun `shorter match beats case mismatch longer match`() {
        val emotes = listOf(emote("wikked"), emote("Wink"))
        val result = provider.filterEmotesScored(emotes, "wi", emptySet())

        // Wink: 1 case diff + 2*100 = 201, wikked: -10 + 4*100 = 390
        assertEquals(
            expected = listOf("Wink", "wikked"),
            actual = result.map { (it.suggestion as Suggestion.EmoteSuggestion).emote.code },
        )
    }

    @Test
    fun `recently used emote gets boost`() {
        val emotes = listOf(emote("PogChamp", id = "1"), emote("PogU", id = "2"))
        val result = provider.filterEmotesScored(emotes, "Pog", setOf("1"))

        // PogChamp: -10 + 5*100 - 50 = 440, PogU: -10 + 1*100 = 90
        // PogU still wins due to length dominance
        assertEquals(
            expected = listOf("PogU", "PogChamp"),
            actual = result.map { (it.suggestion as Suggestion.EmoteSuggestion).emote.code },
        )
    }

    @Test
    fun `non-matching emotes are excluded`() {
        val emotes = listOf(emote("Kappa"), emote("PogChamp"), emote("LUL"))
        val result = provider.filterEmotesScored(emotes, "Pog", emptySet())

        assertEquals(
            expected = listOf("PogChamp"),
            actual = result.map { (it.suggestion as Suggestion.EmoteSuggestion).emote.code },
        )
    }

    // endregion

    // region filterUsers

    @Test
    fun `users sorted alphabetically`() {
        val users = setOf(DisplayName("Zed"), DisplayName("Alice"), DisplayName("Mike"))
        val result = provider.filterUsers(users, "")

        assertEquals(
            expected = listOf("Alice", "Mike", "Zed"),
            actual = result.map { it.name.value },
        )
    }

    @Test
    fun `users filtered by prefix and sorted`() {
        val users = setOf(DisplayName("Bob"), DisplayName("Anna"), DisplayName("Alex"))
        val result = provider.filterUsers(users, "A")

        assertEquals(
            expected = listOf("Alex", "Anna"),
            actual = result.map { it.name.value },
        )
    }

    @Test
    fun `users with at-prefix get leading at`() {
        val users = setOf(DisplayName("Bob"), DisplayName("Bea"))
        val result = provider.filterUsers(users, "@B")

        assertEquals(
            expected = listOf("@Bea", "@Bob"),
            actual = result.map { it.toString() },
        )
    }

    // endregion

    // region filterUsersScored

    @Test
    fun `scored users use emote scoring with penalty`() {
        val users = setOf(DisplayName("Pog"), DisplayName("PogChamp"))
        val result = provider.filterUsersScored(users, "Pog")

        assertEquals(
            expected = listOf("Pog", "PogChamp"),
            actual = result.map { (it.suggestion as Suggestion.UserSuggestion).name.value },
        )
        // Pog: -10 + 25 = 15, PogChamp: -10 + 5*100 + 25 = 515
        assertEquals(15, result[0].score)
        assertEquals(515, result[1].score)
    }

    @Test
    fun `scored users exclude non-matching names`() {
        val users = setOf(DisplayName("Alice"), DisplayName("Bob"))
        val result = provider.filterUsersScored(users, "Pog")

        assertEquals(emptyList(), result)
    }

    @Test
    fun `scored users have higher score than equivalent emotes`() {
        val provider = this.provider
        val emoteScore = provider.scoreEmote("Pog", "Pog", isRecentlyUsed = false)
        val users = setOf(DisplayName("Pog"))
        val userResult = provider.filterUsersScored(users, "Pog")

        assertTrue(userResult[0].score > emoteScore)
    }

    // endregion

    // region filterCommands

    @Test
    fun `commands sorted alphabetically`() {
        val commands = listOf("/timeout", "/ban", "/mod")
        val result = provider.filterCommands(commands, "/")

        assertEquals(
            expected = listOf("/ban", "/mod", "/timeout"),
            actual = result.map { it.command },
        )
    }

    @Test
    fun `commands filtered by prefix`() {
        val commands = listOf("/timeout", "/ban", "/title")
        val result = provider.filterCommands(commands, "/ti")

        assertEquals(
            expected = listOf("/timeout", "/title"),
            actual = result.map { it.command },
        )
    }

    // endregion

    // region filterEmojis

    @Test
    fun `emojis filtered by shortcode`() {
        val emojis =
            listOf(
                EmojiData("smile", "\uD83D\uDE04"),
                EmojiData("wave", "\uD83D\uDC4B"),
                EmojiData("smirk", "\uD83D\uDE0F"),
            )
        val result = provider.filterEmojis(emojis, "smi")

        assertEquals(
            expected = listOf("smile", "smirk"),
            actual = result.map { it.suggestion as Suggestion.EmojiSuggestion }.map { it.emoji.code },
        )
    }

    @Test
    fun `emojis use same scoring as emotes`() {
        val emojis =
            listOf(
                EmojiData("smirk", "\uD83D\uDE0F"),
                EmojiData("smile", "\uD83D\uDE04"),
            )
        val result = provider.filterEmojis(emojis, "smi")

        assertEquals(2, result.size)
    }

    @Test
    fun `non-matching emojis excluded`() {
        val emojis =
            listOf(
                EmojiData("wave", "\uD83D\uDC4B"),
                EmojiData("heart", "\u2764\uFE0F"),
            )
        val result = provider.filterEmojis(emojis, "smi")

        assertEquals(emptyList(), result)
    }

    // endregion
}
