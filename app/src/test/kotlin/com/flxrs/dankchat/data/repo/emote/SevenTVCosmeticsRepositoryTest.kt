package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventApiClient
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.toUserName
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettings
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The scheduler is driven by hand, runTest would wait for the endless sampling of the repository to become idle
@OptIn(ExperimentalCoroutinesApi::class)
internal class SevenTVCosmeticsRepositoryTest {
    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(scheduler)
    private val dispatchers =
        object : DispatchersProvider {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val immediate: CoroutineDispatcher = testDispatcher
        }
    private val events = MutableSharedFlow<SevenTVEventMessage>(extraBufferCapacity = 16)
    private val eventClient = mockk<SevenTVEventApiClient> { every { messages } returns events }
    private val chatMessageRepository = mockk<ChatMessageRepository>(relaxed = true)
    private val chatSettings = mockk<ChatSettingsDataStore> { every { settings } returns MutableStateFlow(ChatSettings()) }
    private val repository = SevenTVCosmeticsRepository(eventClient, mockk(), mockk(), chatMessageRepository, chatSettings, mockk(), dispatchers)

    private val paint =
        Json { ignoreUnknownKeys = true }.decodeFromString<SevenTVPaintDto>(
            """{"id":"paint","function":"URL","image_url":"https://cdn.7tv.app/paint/p/1x.webp"}""",
        )
    private val userId = "1".toUserId()
    private val userName = "littietaco".toUserName()

    private fun emit(event: SevenTVEventMessage) {
        scheduler.runCurrent()
        events.tryEmit(event)
        scheduler.runCurrent()
    }

    @Test
    fun `mentions find the paint by name regardless of its case`() {
        emit(SevenTVEventMessage.PaintCreated(paint))
        emit(SevenTVEventMessage.PaintAssigned("paint", listOf(userId), listOf(userName)))

        assertEquals("paint", repository.paintForName("LittIeTaco".toUserName())?.id)
        assertEquals("paint", repository.paintFor(userId)?.id)
        assertNull(repository.paintForName("other".toUserName()))
    }

    @Test
    fun `removing a paint also removes it from mentions`() {
        emit(SevenTVEventMessage.PaintCreated(paint))
        emit(SevenTVEventMessage.PaintAssigned("paint", listOf(userId), listOf(userName)))
        emit(SevenTVEventMessage.PaintRemoved("paint", listOf(userId), listOf(userName)))

        assertNull(repository.paintForName(userName))
        assertNull(repository.paintFor(userId))
    }

    @Test
    fun `a changed paint replaces the old one in any event order`() {
        val newPaint = paint.copy(id = "new")
        emit(SevenTVEventMessage.PaintCreated(paint))
        emit(SevenTVEventMessage.PaintCreated(newPaint))
        emit(SevenTVEventMessage.PaintAssigned("paint", listOf(userId), listOf(userName)))

        emit(SevenTVEventMessage.PaintAssigned("new", listOf(userId), listOf(userName)))
        emit(SevenTVEventMessage.PaintRemoved("paint", listOf(userId), listOf(userName)))

        assertEquals("new", repository.paintFor(userId)?.id)
        assertEquals("new", repository.paintForName(userName)?.id)
        scheduler.advanceTimeBy(3_000)
        scheduler.runCurrent()
        verify { chatMessageRepository.remapMessagesOfUsers(setOf(userId), setOf(userName)) }
    }

    @Test
    fun `a paint arriving later updates the messages mentioning the user`() {
        emit(SevenTVEventMessage.PaintCreated(paint))
        emit(SevenTVEventMessage.PaintAssigned("paint", listOf(userId), listOf(userName)))
        scheduler.advanceTimeBy(3_000)
        scheduler.runCurrent()

        verify { chatMessageRepository.remapMessagesOfUsers(setOf(userId), setOf(userName)) }
    }

    @Test
    fun `paint changes do not parse messages again`() {
        emit(SevenTVEventMessage.PaintCreated(paint))
        emit(SevenTVEventMessage.PaintAssigned("paint", listOf(userId), listOf(userName)))
        scheduler.advanceTimeBy(3_000)
        scheduler.runCurrent()

        coVerify(exactly = 0) { chatMessageRepository.reparseMessagesOfUsers(any()) }
    }
}
