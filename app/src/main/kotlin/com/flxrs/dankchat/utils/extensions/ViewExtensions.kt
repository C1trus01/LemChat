package com.flxrs.dankchat.utils.extensions

import android.graphics.drawable.LayerDrawable

inline fun <reified T : Any> LayerDrawable.forEachLayer(action: (T) -> Unit) {
    for (i in 0..<numberOfLayers) {
        val drawable = getDrawable(i)
        if (drawable is T) {
            action(drawable)
        }
    }
}
