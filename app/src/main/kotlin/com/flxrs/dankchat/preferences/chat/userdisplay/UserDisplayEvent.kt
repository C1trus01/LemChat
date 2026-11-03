package com.flxrs.dankchat.preferences.chat.userdisplay

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow

@Immutable
sealed interface UserDisplayEvent {
    data class ItemRemoved(
        val item: UserDisplayItem,
        val position: Int,
    ) : UserDisplayEvent

    data class ItemAdded(
        val position: Int,
        val isLast: Boolean,
    ) : UserDisplayEvent
}

@Stable
data class UserDisplayEventsWrapper(
    val events: Flow<UserDisplayEvent>,
)
