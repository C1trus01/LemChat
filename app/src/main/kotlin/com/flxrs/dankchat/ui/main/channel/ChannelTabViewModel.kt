package com.flxrs.dankchat.ui.main.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.data.state.ChannelLoadingState
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.data.twitch.message.WhisperMessage
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ChannelTabViewModel(
    private val chatChannelProvider: ChatChannelProvider,
    private val chatNotificationRepository: ChatNotificationRepository,
    channelDataCoordinator: ChannelDataCoordinator,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {
    val uiState: StateFlow<ChannelTabUiState> =
        preferenceStore
            .getChannelsWithRenamesFlow()
            .flatMapLatest { channels ->
                if (channels.isEmpty()) {
                    return@flatMapLatest flowOf(ChannelTabUiState(loading = false))
                }

                val loadingFlows =
                    channels.map {
                        channelDataCoordinator.getChannelLoadingState(it.channel)
                    }

                combine(
                    chatChannelProvider.activeChannel,
                    chatNotificationRepository.unreadMessagesMap,
                    chatNotificationRepository.channelMentionCount,
                    combine(loadingFlows) { it.toList() },
                    channelDataCoordinator.globalLoadingState,
                ) { active, unread, mentions, loadingStates, globalState ->
                    val tabs =
                        channels.mapIndexed { index, channelWithRename ->
                            ChannelTabItem(
                                channel = channelWithRename.channel,
                                displayName =
                                    channelWithRename.rename?.value
                                        ?: channelWithRename.channel.value,
                                isSelected = channelWithRename.channel == active,
                                hasUnread = unread[channelWithRename.channel] ?: false,
                                mentionCount = mentions[channelWithRename.channel] ?: 0,
                                loadingState = loadingStates[index],
                            )
                        }
                    ChannelTabUiState(
                        tabs = tabs.toImmutableList(),
                        selectedIndex =
                            channels
                                .indexOfFirst { it.channel == active }
                                .coerceAtLeast(0),
                        loading =
                            globalState == GlobalLoadingState.Loading ||
                                tabs.any { it.loadingState == ChannelLoadingState.Loading },
                        whisperMentionCount = mentions[WhisperMessage.WHISPER_CHANNEL] ?: 0,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelTabUiState())

    fun selectTab(index: Int) {
        val channels = preferenceStore.channels
        if (index in channels.indices) {
            val channel = channels[index]
            chatChannelProvider.setActiveChannel(channel)
            chatNotificationRepository.clearUnreadMessage(channel)
            chatNotificationRepository.clearMentionCount(channel)
        }
    }

    fun clearAllMentionCounts() {
        chatNotificationRepository.clearMentionCounts()
    }
}
