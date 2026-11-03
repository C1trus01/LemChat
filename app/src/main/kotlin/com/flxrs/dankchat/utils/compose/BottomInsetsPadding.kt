package com.flxrs.dankchat.utils.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

/**
 * Bottom padding driven by [insets], resolved in the layout phase so per-frame inset changes
 * (like the ime animation) only trigger remeasure instead of recomposition.
 * Unlike [androidx.compose.foundation.layout.windowInsetsPadding], the insets are not consumed
 * for descendants.
 */
fun Modifier.bottomInsetsPadding(insets: WindowInsets): Modifier = layout { measurable, constraints ->
    val bottom = insets.getBottom(this)
    val placeable = measurable.measure(constraints.offset(vertical = -bottom))
    val width = constraints.constrainWidth(placeable.width)
    val height = constraints.constrainHeight(placeable.height + bottom)
    layout(width, height) {
        placeable.place(0, 0)
    }
}
