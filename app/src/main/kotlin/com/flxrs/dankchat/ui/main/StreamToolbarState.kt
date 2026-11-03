package com.flxrs.dankchat.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.data.UserName

@Stable
internal class StreamToolbarState(
    val alpha: Animatable<Float, AnimationVector1D>,
) {
    var heightDp by mutableStateOf(0.dp)
    private var prevHasVisibleStream by mutableStateOf(false)

    val hasVisibleStream: Boolean
        get() = heightDp > 0.dp

    val effectiveAlpha: Float
        get() = alpha.value

    suspend fun updateAnimation(hasVisibleStream: Boolean) {
        when {
            hasVisibleStream && !prevHasVisibleStream -> {
                prevHasVisibleStream = true
                // Only fade in from 0 if toolbar isn't already visible
                // (e.g. first stream open). When returning from keyboard/emote menu
                // the toolbar is already at 1.0, so skip the fade-in animation.
                if (alpha.value < 0.1f) {
                    alpha.snapTo(0f)
                    alpha.animateTo(1f, tween(durationMillis = 350))
                }
            }

            !hasVisibleStream && prevHasVisibleStream -> {
                prevHasVisibleStream = false
                // Stream is hiding — animate toolbar to fully visible so it stays
                // visible while the stream is gone (keyboard/emote menu open).
                alpha.animateTo(1f, tween(durationMillis = 150))
            }
        }
    }
}

@Composable
internal fun rememberStreamToolbarState(currentStream: UserName?): StreamToolbarState {
    val state = remember { StreamToolbarState(alpha = Animatable(1f)) }

    val hasVisibleStream = currentStream != null && state.heightDp > 0.dp

    LaunchedEffect(hasVisibleStream) {
        state.updateAnimation(hasVisibleStream)
    }
    LaunchedEffect(currentStream) {
        if (currentStream == null) {
            state.heightDp = 0.dp
            state.alpha.snapTo(1f)
        }
    }

    return state
}
