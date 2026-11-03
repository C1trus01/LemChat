package com.flxrs.dankchat.data.repo

import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ShieldModeRepository(
    private val helixApiClient: HelixApiClient,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
) {
    private val states = ConcurrentHashMap<UserName, MutableStateFlow<Boolean?>>()

    fun getState(channel: UserName): StateFlow<Boolean?> = states.getOrPut(channel) { MutableStateFlow(null) }

    fun setState(
        channel: UserName,
        active: Boolean,
    ) {
        states.getOrPut(channel) { MutableStateFlow(null) }.value = active
    }

    suspend fun fetch(channel: UserName) {
        val channelId = channelRepository.getChannel(channel)?.id ?: return
        val moderatorId = authDataStore.userIdString ?: return
        helixApiClient
            .getShieldMode(channelId, moderatorId)
            .onSuccess { setState(channel, it.isActive) }
    }
}
