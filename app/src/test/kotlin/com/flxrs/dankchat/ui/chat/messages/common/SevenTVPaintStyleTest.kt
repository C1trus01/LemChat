package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.ui.graphics.Color
import com.flxrs.dankchat.data.repo.emote.SevenTVPaint
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SevenTVPaintStyleTest {
    private fun stops(vararg positions: Float) = positions.map { SevenTVPaint.Stop(position = it, color = OPAQUE_WHITE) }

    @Test
    fun `repeating gradients stretch their stops to the full range`() {
        val positions = stops(0.1f, 0.2f, 0.2f, 0.3f).toColorStops(repeat = true, userColor = Color.Red)?.map { it.first }

        assertEquals(listOf(0f, 0.5f, 0.5f, 1f), positions?.map { (it * 1000).roundToInt() / 1000f })
    }

    @Test
    fun `other gradients keep their stops`() {
        val positions = stops(0.2f, 0.8f).toColorStops(repeat = false, userColor = Color.Red)?.map { it.first }

        assertEquals(listOf(0.2f, 0.8f), positions)
    }

    @Test
    fun `stops stay ascending and in range`() {
        val positions = stops(0.5f, 0.2f, 1.5f).toColorStops(repeat = false, userColor = Color.Red)?.map { it.first }

        assertEquals(listOf(0.5f, 0.5f, 1f), positions)
    }

    @Test
    fun `a repeat without a range does not divide by zero`() {
        val positions = stops(0.5f, 0.5f).toColorStops(repeat = true, userColor = Color.Red)?.map { it.first }

        assertEquals(listOf(0.5f, 0.5f), positions)
    }

    @Test
    fun `translucent stops show the color of the user`() {
        val stops = listOf(SevenTVPaint.Stop(position = 0f, color = 0x00FFFFFF), SevenTVPaint.Stop(position = 1f, color = OPAQUE_WHITE))

        val colors = stops.toColorStops(repeat = false, userColor = Color.Red)?.map { it.second }

        assertEquals(listOf(Color.Red, Color.White), colors)
    }

    @Test
    fun `paints without stops have no colors`() {
        assertNull(emptyList<SevenTVPaint.Stop>().toColorStops(repeat = false, userColor = Color.Red))
    }

    private companion object {
        const val OPAQUE_WHITE = -1
    }
}
