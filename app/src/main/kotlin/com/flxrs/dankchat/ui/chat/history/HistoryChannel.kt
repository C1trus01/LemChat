package com.flxrs.dankchat.ui.chat.history

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName

@Immutable
sealed interface HistoryChannel {
    data object Global : HistoryChannel

    data class Channel(
        val name: UserName,
    ) : HistoryChannel
}
