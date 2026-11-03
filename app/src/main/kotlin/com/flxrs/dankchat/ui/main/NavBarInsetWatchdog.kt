package com.flxrs.dankchat.ui.main

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

private val CHECK_INTERVAL = 3.seconds
private const val LOG_FILE_NAME = "navbar_insets.log"
private const val LOG_MAX_BYTES = 64 * 1024L

/**
 * The input sometimes ends up behind the navigation buttons, seen after the app sat in the background for hours.
 * The trigger is still unknown, so this notices the state, tries to leave it and records what it saw.
 * Remove it once the cause is found and fixed.
 */
@Composable
internal fun NavBarInsetWatchdog(expectBarsVisible: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val navBars = WindowInsets.navigationBars
    val lifecycleOwner = LocalLifecycleOwner.current
    val expectVisible by rememberUpdatedState(expectBarsVisible)

    LaunchedEffect(view, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var wasWrong = false
            var isReported = false
            while (isActive) {
                delay(CHECK_INTERVAL)
                val insets = ViewCompat.getRootWindowInsets(view) ?: continue
                val type = WindowInsetsCompat.Type.navigationBars()
                val applied = navBars.getBottom(density)
                val window = insets.getInsets(type).bottom
                val full = insets.getInsetsIgnoringVisibility(type).bottom
                val isHidden = expectVisible && window < full
                val isWrong = applied != window || isHidden

                when {
                    // One odd reading can be an inset animation in progress
                    isWrong && wasWrong -> {
                        if (!isReported) {
                            isReported = true
                            appendLog(context, "wrong applied=$applied window=$window full=$full expectVisible=$expectVisible")
                        }
                        ViewCompat.requestApplyInsets(view)
                        if (isHidden) {
                            (context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view).show(type) }
                        }
                    }

                    !isWrong && isReported -> {
                        isReported = false
                        appendLog(context, "recovered applied=$applied window=$window full=$full")
                    }
                }
                wasWrong = isWrong
            }
        }
    }
}

private suspend fun appendLog(context: Context, message: String) = withContext(Dispatchers.IO) {
    val file = File(context.getExternalFilesDir(null) ?: return@withContext, LOG_FILE_NAME)
    if (file.length() < LOG_MAX_BYTES) {
        file.appendText("${LocalDateTime.now()} $message\n")
    }
}
