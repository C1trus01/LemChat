package com.flxrs.dankchat.ui.chat.messages.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Selects the appropriate background color based on current theme.
 * Semi-transparent colors (e.g. checkered backgrounds) are composited over the
 * theme background to produce an opaque result suitable for contrast calculations.
 */
@Composable
fun rememberBackgroundColor(
    lightColor: Color,
    darkColor: Color,
): Color {
    val raw = if (isSystemInDarkTheme()) darkColor else lightColor
    val background = MaterialTheme.colorScheme.background
    return remember(raw, background) {
        when {
            raw == Color.Transparent -> Color.Transparent
            raw.alpha < 1f -> raw.compositeOver(background)
            else -> raw
        }
    }
}
