package com.flxrs.dankchat.ui.main

import com.flxrs.dankchat.preferences.appearance.HubAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HubUiStateTest {
    @Test
    fun `stream cannot be started without stream data`() {
        val tile = HubUiState(hasChannel = true).tileFor(HubAction.Stream)

        assertNotNull(tile)
        assertFalse(tile!!.enabled)
    }

    @Test
    fun `running stream can always be hidden`() {
        val tile = HubUiState(isStreamActive = true).tileFor(HubAction.Stream)

        assertTrue(tile!!.enabled)
        assertTrue(tile.isActive)
    }

    @Test
    fun `audio only exists only while a stream runs`() {
        assertNull(HubUiState().tileFor(HubAction.AudioOnly))
        assertNotNull(HubUiState(isStreamActive = true).tileFor(HubAction.AudioOnly))
    }

    @Test
    fun `moderation exists only for moderators`() {
        assertNull(HubUiState(hasChannel = true).tileFor(HubAction.ModActions))
        assertNotNull(HubUiState(hasChannel = true, isModerator = true).tileFor(HubAction.ModActions))
    }

    @Test
    fun `fullscreen is hidden in theater`() {
        assertNull(HubUiState(isTheaterMode = true).tileFor(HubAction.Fullscreen))
        assertNotNull(HubUiState().tileFor(HubAction.Fullscreen))
    }

    @Test
    fun `debug exists only in debug mode`() {
        assertNull(HubUiState().tileFor(HubAction.Debug))
        assertNotNull(HubUiState(debugMode = true).tileFor(HubAction.Debug))
    }

    @Test
    fun `channel actions need a channel`() {
        val noChannel = HubUiState(hasChannel = false)
        val withChannel = HubUiState(hasChannel = true)

        listOf(HubAction.Search, HubAction.ReloadEmotes).forEach { action ->
            assertFalse(noChannel.tileFor(action)!!.enabled, "$action without a channel")
            assertTrue(withChannel.tileFor(action)!!.enabled, "$action with a channel")
        }
    }

    @Test
    fun `channels stay reachable without a channel to add the first one`() {
        assertTrue(HubUiState(hasChannel = false).tileFor(HubAction.Channel)!!.enabled)
    }

    @Test
    fun `pinned message exists only while the channel has one`() {
        assertNull(HubUiState().tileFor(HubAction.PinnedMessage))

        val shown = HubUiState(hasPinnedMessage = true, isPinnedMessageShown = true).tileFor(HubAction.PinnedMessage)
        assertTrue(shown!!.isActive)
        assertFalse(HubUiState(hasPinnedMessage = true).tileFor(HubAction.PinnedMessage)!!.isActive)
    }

    @Test
    fun `last message needs an enabled input and a sent message`() {
        assertFalse(HubUiState(inputEnabled = true).tileFor(HubAction.LastMessage)!!.enabled)
        assertFalse(HubUiState(hasLastMessage = true).tileFor(HubAction.LastMessage)!!.enabled)
        assertTrue(HubUiState(inputEnabled = true, hasLastMessage = true).tileFor(HubAction.LastMessage)!!.enabled)
    }

    @Test
    fun `tile keeps the action it was built for`() {
        val state = HubUiState(hasChannel = true, isStreamActive = true, isModerator = true, debugMode = true, hasPinnedMessage = true)

        HubAction.entries.forEach { action ->
            assertEquals(action, state.tileFor(action)?.action)
        }
    }
}
