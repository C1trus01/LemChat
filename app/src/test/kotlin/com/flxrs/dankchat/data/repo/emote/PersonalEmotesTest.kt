package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteDto
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventMessage
import com.flxrs.dankchat.data.toDisplayName
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.data.twitch.emote.ChatMessageEmoteType
import com.flxrs.dankchat.data.twitch.message.PrivMessage
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class PersonalEmotesTest {
    private val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    private val chatSettings = mockk<ChatSettingsDataStore> { every { current() } returns ChatSettings() }
    private val repository = EmoteRepository(mockk(), chatSettings, mockk(), mockk())
    private val subscriber = "1".toUserId()

    private fun emote(
        name: String,
        state: String = """["PERSONAL","LISTED"]""",
        flags: Long = 0,
    ) = json.decodeFromString<SevenTVEmoteDto>(
        """{"id":"$name-id","name":"$name","flags":0,"data":{"listed":true,"animated":false,"flags":$flags,"state":$state,"name":"$name","host":{"url":"//cdn.7tv.app/emote/$name","files":[{"name":"2x.webp","format":"WEBP","width":1,"height":1},{"name":"4x.webp","format":"WEBP","width":1,"height":1}]}}}""",
    )

    private fun update(
        vararg added: SevenTVEmoteDto,
        removed: List<String> = emptyList(),
    ) = SevenTVEventMessage.PersonalEmoteSetUpdated(
        emoteSetId = SET,
        added = added.toList(),
        removed = removed.map { SevenTVEventMessage.EmoteSetUpdated.RemovedEmote("$it-id", it) },
        updated = emptyList(),
    )

    private fun message(
        userId: String,
        text: String = "hello nnysBop",
    ) = PrivMessage(
        channel = "channel".toUserName(),
        sourceChannel = null,
        userId = userId.toUserId(),
        name = "user".toUserName(),
        displayName = "user".toDisplayName(),
        message = text,
        tags = emptyMap(),
    )

    private suspend fun parsedEmotes(message: PrivMessage) = assertIs<PrivMessage>(repository.parseEmotesAndBadges(message)).emotes

    @Test
    fun `personal emotes only show in messages of users with the set`() = runTest {
        repository.createPersonalEmoteSet(SET)
        repository.assignPersonalEmoteSet(SET, listOf(subscriber))
        repository.updatePersonalEmoteSet(update(emote("nnysBop")))

        val emote = parsedEmotes(message(userId = "1")).single()
        assertEquals("nnysBop", emote.code)
        assertIs<ChatMessageEmoteType.PersonalSevenTVEmote>(emote.type)
        assertEquals(emptyList(), parsedEmotes(message(userId = "2")))
    }

    @Test
    fun `emotes not approved for personal use are dropped`() = runTest {
        repository.createPersonalEmoteSet(SET)
        repository.assignPersonalEmoteSet(SET, listOf(subscriber))

        val changed = repository.updatePersonalEmoteSet(
            update(
                emote("nnysBop", state = """["LISTED"]"""),
                emote("Disallowed", flags = 1L shl 24),
            ),
        )

        assertEquals(emptySet(), changed)
        assertEquals(emptyList(), parsedEmotes(message(userId = "1", text = "nnysBop Disallowed")))
    }

    @Test
    fun `sets that were not announced as personal are ignored`() = runTest {
        repository.assignPersonalEmoteSet(SET, listOf(subscriber))

        assertEquals(emptySet(), repository.updatePersonalEmoteSet(update(emote("nnysBop"))))
        assertEquals(emptyList(), parsedEmotes(message(userId = "1")))
    }

    @Test
    fun `only changes report the users to parse again`() {
        repository.createPersonalEmoteSet(SET)

        // Nothing to show yet, the set is still empty
        assertEquals(emptySet(), repository.assignPersonalEmoteSet(SET, listOf(subscriber)))
        assertEquals(setOf(subscriber), repository.updatePersonalEmoteSet(update(emote("nnysBop"))))
        // 7TV repeats both events for every presence
        assertEquals(emptySet(), repository.updatePersonalEmoteSet(update(emote("nnysBop"))))
        assertEquals(emptySet(), repository.assignPersonalEmoteSet(SET, listOf(subscriber)))
        // A second user gets a set that already has emotes
        assertEquals(setOf("2".toUserId()), repository.assignPersonalEmoteSet(SET, listOf("2".toUserId())))
    }

    @Test
    fun `removed emotes stop showing`() = runTest {
        repository.createPersonalEmoteSet(SET)
        repository.assignPersonalEmoteSet(SET, listOf(subscriber))
        repository.updatePersonalEmoteSet(update(emote("nnysBop")))

        repository.updatePersonalEmoteSet(update(removed = listOf("nnysBop")))

        assertEquals(emptyList(), parsedEmotes(message(userId = "1")))
    }

    @Test
    fun `the setting turns personal emotes off`() = runTest {
        repository.createPersonalEmoteSet(SET)
        repository.assignPersonalEmoteSet(SET, listOf(subscriber))
        repository.updatePersonalEmoteSet(update(emote("nnysBop")))
        every { chatSettings.current() } returns ChatSettings(sevenTVPersonalEmotes = false)

        assertEquals(emptyList(), parsedEmotes(message(userId = "1")))
    }

    private companion object {
        const val SET = "set"
    }
}
