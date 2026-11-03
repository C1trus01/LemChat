package com.flxrs.dankchat.data.auth

import com.flxrs.dankchat.data.UserName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

sealed interface StartupValidation {
    data object Pending : StartupValidation

    data object Validated : StartupValidation

    data class ScopesOutdated(
        val userName: UserName,
    ) : StartupValidation

    data object TokenInvalid : StartupValidation
}

@Single
class StartupValidationHolder {
    private val _state = MutableStateFlow<StartupValidation>(StartupValidation.Pending)
    val state: StateFlow<StartupValidation> = _state.asStateFlow()

    val isAuthAvailable: Boolean
        get() {
            val current = _state.value
            return current is StartupValidation.Validated || current is StartupValidation.ScopesOutdated
        }

    fun update(validation: StartupValidation) {
        _state.value = validation
    }

    fun acknowledge() {
        _state.value = StartupValidation.Validated
    }

    suspend fun awaitResolved() {
        _state.first { it !is StartupValidation.Pending }
    }
}
