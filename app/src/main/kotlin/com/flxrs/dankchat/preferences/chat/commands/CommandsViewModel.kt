package com.flxrs.dankchat.preferences.chat.commands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.repo.command.CommandRepository
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.preferences.chat.CustomCommand
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class CommandsViewModel(
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val commandRepository: CommandRepository,
) : ViewModel() {
    val commands =
        chatSettingsDataStore.settings
            .map { it.customCommands.toImmutableList() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = chatSettingsDataStore.current().customCommands.toImmutableList(),
            )

    val reservedTriggers: Set<String> get() = commandRepository.getReservedTriggers()

    fun save(commands: List<CustomCommand>) = viewModelScope.launch {
        val reserved = commandRepository.getReservedTriggers()
        val filtered = commands
            .filter { it.trigger.isNotBlank() && it.command.isNotBlank() }
            .filter { it.trigger !in reserved }
            .distinctBy { it.trigger }
        chatSettingsDataStore.update { it.copy(customCommands = filtered) }
    }
}
