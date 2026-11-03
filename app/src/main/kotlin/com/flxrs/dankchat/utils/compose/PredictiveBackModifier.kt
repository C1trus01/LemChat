package com.flxrs.dankchat.utils.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.predictiveBackScale(progress: Float): Modifier = graphicsLayer {
    val scale = 1f - (progress * 0.1f)
    scaleX = scale
    scaleY = scale
    alpha = 1f - (progress * 0.3f)
}
