package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.repo.chat.ChannelMessageRateTracker.Companion.WINDOW_MINUTES
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ChannelMessageRateTrackerTest {
    @Test
    fun `empty window has zero rate`() {
        val window = ChannelMessageRateTracker.Window()
        assertEquals(0, window.ratePerMinute(minute = 100))
    }

    @Test
    fun `rate is averaged over the full window`() {
        val window = ChannelMessageRateTracker.Window()
        repeat(500) { window.increment(minute = 100) }
        assertEquals(500 / WINDOW_MINUTES, window.ratePerMinute(minute = 100))
    }

    @Test
    fun `sustained rate across all buckets reports full average`() {
        val window = ChannelMessageRateTracker.Window()
        for (minute in 100L until 100L + WINDOW_MINUTES) {
            repeat(200) { window.increment(minute) }
        }
        assertEquals(200, window.ratePerMinute(minute = 100L + WINDOW_MINUTES - 1))
    }

    @Test
    fun `a single burst minute stays below the sustained rate`() {
        val window = ChannelMessageRateTracker.Window()
        repeat(300) { window.increment(minute = 100) }
        assertEquals(60, window.ratePerMinute(minute = 100))
    }

    @Test
    fun `stale buckets are cleared as minutes advance`() {
        val window = ChannelMessageRateTracker.Window()
        repeat(500) { window.increment(minute = 100) }
        assertEquals(100, window.ratePerMinute(minute = 100))
        assertEquals(100, window.ratePerMinute(minute = 102))
        assertEquals(0, window.ratePerMinute(minute = 100L + WINDOW_MINUTES))
    }

    @Test
    fun `advancing more than the window clears everything`() {
        val window = ChannelMessageRateTracker.Window()
        for (minute in 100L until 100L + WINDOW_MINUTES) {
            repeat(100) { window.increment(minute) }
        }
        assertEquals(0, window.ratePerMinute(minute = 1000))
    }

    @Test
    fun `rate queries in the past do not clear newer buckets`() {
        val window = ChannelMessageRateTracker.Window()
        repeat(500) { window.increment(minute = 100) }
        window.ratePerMinute(minute = 50)
        assertEquals(100, window.ratePerMinute(minute = 100))
    }
}
