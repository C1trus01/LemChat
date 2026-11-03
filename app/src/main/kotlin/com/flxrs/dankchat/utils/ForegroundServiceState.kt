package com.flxrs.dankchat.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class ForegroundServiceState {
    private val _active = MutableStateFlow(true)
    val active = _active.asStateFlow()

    fun setActive(active: Boolean) {
        _active.value = active
    }
}
