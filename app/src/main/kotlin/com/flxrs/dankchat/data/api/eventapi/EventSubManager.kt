package com.flxrs.dankchat.data.api.eventapi

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class EventSubManager(
    private val eventSubClient: EventSubClient,
    private val channelRepository: ChannelRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val userStateRepository: UserStateRepository,
    private val authDataStore: AuthDataStore,
    developerSettingsDataStore: DeveloperSettingsDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val isEnabled = developerSettingsDataStore.current().shouldUseEventSub
    private var debugOutput = developerSettingsDataStore.current().eventSubDebugOutput

    val events =
        eventSubClient.events
            .filter { it !is SystemMessage || debugOutput }

    init {
        scope.launch {
            if (!isEnabled) {
                return@launch
            }

            userStateRepository.userState.map { it.moderationChannels }.collect {
                subscribeModerationTopics(it)
            }
        }

        scope.launch {
            if (!isEnabled) {
                return@launch
            }

            chatChannelProvider.channels.filterNotNull().collect { channels ->
                subscribeUserMessageTopics(channels)
            }
        }

        scope.launch {
            if (!isEnabled) {
                return@launch
            }

            developerSettingsDataStore.settings.collect {
                debugOutput = it.eventSubDebugOutput
            }
        }
    }

    private suspend fun subscribeModerationTopics(moderationChannels: Set<UserName>) {
        val userId = authDataStore.userIdString ?: return
        val channels = channelRepository.getChannels(moderationChannels)
        channels.forEach { channel ->
            eventSubClient.subscribe(EventSubTopic.ChannelModerate(channel = channel.name, broadcasterId = channel.id, moderatorId = userId))
            eventSubClient.subscribe(EventSubTopic.AutomodMessageHold(channel = channel.name, broadcasterId = channel.id, moderatorId = userId))
            eventSubClient.subscribe(EventSubTopic.AutomodMessageUpdate(channel = channel.name, broadcasterId = channel.id, moderatorId = userId))
        }
    }

    private suspend fun subscribeUserMessageTopics(channelNames: List<UserName>) {
        val userId = authDataStore.userIdString ?: return
        val resolved = channelRepository.getChannels(channelNames)
        resolved.forEach {
            eventSubClient.subscribe(EventSubTopic.UserMessageHold(channel = it.name, broadcasterId = it.id, userId = userId))
            eventSubClient.subscribe(EventSubTopic.UserMessageUpdate(channel = it.name, broadcasterId = it.id, userId = userId))
        }
    }

    suspend fun pause() {
        if (!isEnabled) {
            return
        }

        eventSubClient.closeAndClearTopics()
    }

    fun resume() {
        if (!isEnabled) {
            return
        }

        scope.launch {
            subscribeModerationTopics(userStateRepository.userState.value.moderationChannels)
            chatChannelProvider.channels.value?.let { subscribeUserMessageTopics(it) }
        }
    }

    fun connectedAndHasModerateTopic(channel: UserName): Boolean {
        val topics = eventSubClient.topics.value
        return eventSubClient.connected && topics.isNotEmpty() &&
            topics.any {
                it.topic is EventSubTopic.ChannelModerate && it.topic.channel == channel
            }
    }

    val connectedAndHasUserMessageTopic: Boolean
        get() {
            val topics = eventSubClient.topics.value
            return eventSubClient.connected && topics.any { it.topic is EventSubTopic.UserMessageHold }
        }

    fun removeChannel(channel: UserName) {
        if (!isEnabled) {
            return
        }

        scope.launch {
            val topics =
                eventSubClient.topics.value.filter { subscribedTopic ->
                    when (val topic = subscribedTopic.topic) {
                        is EventSubTopic.ChannelModerate -> topic.channel == channel
                        is EventSubTopic.AutomodMessageHold -> topic.channel == channel
                        is EventSubTopic.AutomodMessageUpdate -> topic.channel == channel
                        is EventSubTopic.UserMessageHold -> topic.channel == channel
                        is EventSubTopic.UserMessageUpdate -> topic.channel == channel
                    }
                }
            topics.forEach { eventSubClient.unsubscribe(it) }
        }
    }

    fun reconnect() {
        if (!isEnabled) {
            return
        }

        eventSubClient.reconnect()
    }

    fun reconnectIfNecessary() {
        if (!isEnabled) {
            return
        }

        eventSubClient.reconnectIfNecessary()
    }

    suspend fun close() {
        if (!isEnabled) {
            return
        }

        eventSubClient.closeAndClearTopics()
    }
}
