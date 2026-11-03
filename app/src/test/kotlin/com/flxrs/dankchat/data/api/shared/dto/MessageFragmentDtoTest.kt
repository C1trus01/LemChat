package com.flxrs.dankchat.data.api.shared.dto

import com.flxrs.dankchat.data.twitch.message.EmoteWithPositions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MessageFragmentDtoTest {
    @Test
    fun `emote fragment positions match the irc emotes tag convention`() {
        val fragments = listOf(
            textFragment("Welcome! "),
            emoteFragment("bleedPurple", id = "62835"),
            textFragment(" Type !rules"),
        )

        val emotes = fragments.toEmotesWithPositions()

        assertEquals(listOf(EmoteWithPositions(id = "62835", positions = listOf(9..19))), emotes)
    }

    @Test
    fun `positions are code point indexed for text with supplementary characters`() {
        val fragments = listOf(
            // Four code points, six utf-16 units
            textFragment("👍👍 ab"),
            emoteFragment("Kappa", id = "25"),
        )

        val emotes = fragments.toEmotesWithPositions()

        assertEquals(listOf(EmoteWithPositions(id = "25", positions = listOf(5..9))), emotes)
    }

    @Test
    fun `repeated emotes are grouped by id`() {
        val fragments = listOf(
            emoteFragment("Kappa", id = "25"),
            textFragment(" "),
            emoteFragment("Kappa", id = "25"),
        )

        val emotes = fragments.toEmotesWithPositions()

        assertEquals(listOf(EmoteWithPositions(id = "25", positions = listOf(0..4, 6..10))), emotes)
    }

    @Test
    fun `cheermote and mention fragments only advance the position`() {
        val fragments = listOf(
            MessageFragmentDto(
                type = MessageFragmentTypeDto.Cheermote,
                text = "Cheer100",
                cheermote = CheermoteFragmentDto(prefix = "Cheer", bits = 100, tier = 1),
            ),
            textFragment(" "),
            MessageFragmentDto(
                type = MessageFragmentTypeDto.Mention,
                text = "@flex3rs",
            ),
            textFragment(" "),
            emoteFragment("Kappa", id = "25"),
        )

        val emotes = fragments.toEmotesWithPositions()

        assertEquals(listOf(EmoteWithPositions(id = "25", positions = listOf(18..22))), emotes)
    }

    @Test
    fun `no emote fragments produce no emotes`() {
        val fragments = listOf(textFragment("just some text"))

        assertEquals(emptyList(), fragments.toEmotesWithPositions())
    }

    private fun textFragment(text: String) = MessageFragmentDto(type = MessageFragmentTypeDto.Text, text = text)

    private fun emoteFragment(
        text: String,
        id: String,
    ) = MessageFragmentDto(
        type = MessageFragmentTypeDto.Emote,
        text = text,
        emote = EmoteFragmentDto(id = id, emoteSetId = "0"),
    )
}
