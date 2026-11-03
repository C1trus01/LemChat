package com.flxrs.dankchat.ui.chat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Observes scroll direction and fires [onHide]/[onShow] when the accumulated
 * scroll delta exceeds [hideThresholdPx]/[showThresholdPx].
 *
 * With `reverseLayout = true` the nested scroll deltas are inverted:
 * `available.y > 0` = finger up = reading old messages = hide toolbar;
 * `available.y < 0` = finger down = toward new messages = show toolbar.
 *
 * Returns [Offset.Zero] — scroll is observed, never consumed.
 */
class ScrollDirectionTracker(
    private val hideThresholdPx: Float,
    private val showThresholdPx: Float,
    private val onHide: () -> Unit,
    private val onShow: () -> Unit,
) : NestedScrollConnection {
    private var accumulated = 0f

    @Suppress("SameReturnValue")
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        val delta = consumed.y
        if (delta == 0f) return Offset.Zero
        // Reset accumulator on direction change to avoid stale buildup
        when {
            accumulated > 0f && delta < 0f -> accumulated = 0f
            accumulated < 0f && delta > 0f -> accumulated = 0f
        }
        accumulated += delta
        when {
            accumulated > hideThresholdPx -> {
                onHide()
                accumulated = 0f
            }

            accumulated < -showThresholdPx -> {
                onShow()
                accumulated = 0f
            }
        }
        return Offset.Zero
    }
}