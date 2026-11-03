package com.flxrs.dankchat.ui.main

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

@Single
class MainEventBus {
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun emitEvent(event: MainEvent) {
        _events.send(event)
    }
}
