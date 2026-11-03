package com.flxrs.dankchat.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline profile via `:app:generateBaselineProfile`.
 *
 * The journey covers cold start, skipping onboarding without a login, joining a channel
 * anonymously, and letting chat with message history render, so startup, the chat connection,
 * message parsing and chat rendering are all compiled ahead of time.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = BuildConfig.TARGET_APP_ID, maxIterations = 5) {
            pressHome()
            startActivityAndWait()

            device.completeOnboardingIfShown()
            device.joinChannelIfMissing()

            // Let message history, emotes and live messages arrive and render
            device.awaitChatContent()
            device.scrollChat()
        }
    }
}
