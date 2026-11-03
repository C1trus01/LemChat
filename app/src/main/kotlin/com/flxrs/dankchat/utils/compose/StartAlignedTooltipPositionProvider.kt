package com.flxrs.dankchat.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

@Composable
fun rememberStartAlignedTooltipPositionProvider(spacingBetweenTooltipAndAnchor: Dp = 4.dp): PopupPositionProvider {
    val spacingPx = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(spacingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val startX = anchorBounds.left - popupContentSize.width - spacingPx
                return if (startX >= 0) {
                    val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                    IntOffset(
                        startX,
                        y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                    )
                } else {
                    val x =
                        (anchorBounds.right - popupContentSize.width)
                            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val y =
                        (anchorBounds.top - popupContentSize.height - spacingPx)
                            .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
                    IntOffset(x, y)
                }
            }
        }
    }
}
