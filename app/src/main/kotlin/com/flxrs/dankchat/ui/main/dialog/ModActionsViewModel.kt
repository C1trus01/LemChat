package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.ShieldModeRepository
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.RoomState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@Immutable
data class ModActionsUiState(
    val roomState: RoomState?,
    val shieldModeActive: Boolean?,
    val isBroadcaster: Boolean,
)

@KoinViewModel
class ModActionsViewModel(
    private val shieldModeRepository: ShieldModeRepository,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ModActionsUiState?>(null)
    val state: StateFlow<ModActionsUiState?> = _state.asStateFlow()
    val isActive = _state.map { it != null }

    private var collectJob: Job? = null

    fun show(channel: UserName) {
        collectJob?.cancel()
        val initialRoomState = channelRepository.getRoomState(channel)
        val isBroadcaster = authDataStore.userIdString == initialRoomState?.channelId

        collectJob = viewModelScope.launch {
            shieldModeRepository.fetch(channel)

            combine(
                channelRepository.getRoomStateFlow(channel),
                shieldModeRepository.getState(channel),
            ) { roomState, shieldModeActive ->
                ModActionsUiState(
                    roomState = roomState,
                    shieldModeActive = shieldModeActive,
                    isBroadcaster = isBroadcaster,
                )
            }.collect { _state.value = it }
        }

        // Emit immediately with initial state so the sheet opens without delay
        _state.value = ModActionsUiState(
            roomState = initialRoomState,
            shieldModeActive = null,
            isBroadcaster = isBroadcaster,
        )
    }

    fun dismiss() {
        collectJob?.cancel()
        _state.value = null
    }
}
