package com.flxrs.dankchat.utils.extensions

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity

fun ComponentActivity.keepScreenOn(keep: Boolean) {
    if (keep) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

val ComponentActivity.isInSupportedPictureInPictureMode: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isInPictureInPictureMode
