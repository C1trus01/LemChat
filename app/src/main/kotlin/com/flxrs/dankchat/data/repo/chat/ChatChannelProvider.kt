package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class ChatChannelProvider(
    preferenceStore: DankChatPreferenceStore,
    private val channelSelectionDataStore: ChannelSelectionDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(dispatchersProvider.io + SupervisorJob())
    private val _activeChannel = MutableStateFlow<UserName?>(null)
    private val _channels = MutableStateFlow(preferenceStore.channels.takeIf { it.isNotEmpty() }?.toImmutableList())

    val activeChannel: StateFlow<UserName?> = _activeChannel.asStateFlow()
    val channels: StateFlow<ImmutableList<UserName>?> = _channels.asStateFlow()

    fun setActiveChannel(channel: UserName?) {
        _activeChannel.value = channel
        if (channel != null) {
            scope.launch {
                channelSelectionDataStore.update { it.copy(activeChannel = channel.value) }
            }
        }
    }

    fun setChannels(channels: List<UserName>) {
        _channels.value = channels.toImmutableList()
    }
}
