package com.flxrs.dankchat.data.repo.chat

import android.content.res.Resources
import androidx.annotation.StringRes
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName

sealed interface ChatLoadingStep {
    @get:StringRes
    val displayNameRes: Int
    val channel: UserName?

    data class RecentMessages(
        override val channel: UserName,
    ) : ChatLoadingStep {
        override val displayNameRes = R.string.data_loading_step_recent_messages
    }
}

fun List<ChatLoadingStep>.toDisplayStrings(resources: Resources): List<String> {
    val recentMessages = filterIsInstance<ChatLoadingStep.RecentMessages>()

    return buildList {
        if (recentMessages.isNotEmpty()) {
            val channels = recentMessages.joinToString { it.channel.value }
            add(resources.getString(R.string.data_loading_step_with_channels, resources.getString(R.string.data_loading_step_recent_messages), channels))
        }
    }
}
