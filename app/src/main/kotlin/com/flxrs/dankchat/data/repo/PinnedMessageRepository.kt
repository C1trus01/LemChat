package com.flxrs.dankchat.data.repo

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.helix.HelixApiClient
import com.flxrs.dankchat.data.api.helix.dto.PinnedChatMessageDto
import com.flxrs.dankchat.data.api.shared.dto.toEmotesWithPositions
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.channel.ChannelRepository
import com.flxrs.dankchat.data.twitch.message.EmoteWithPositions
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface PinnedMessageState {
    data object None : PinnedMessageState

    data class Pinned(
        val message: PinnedMessage,
    ) : PinnedMessageState
}

data class PinnedMessage(
    val messageId: String,
    val channel: UserName,
    val text: String,
    val emotesWithPositions: List<EmoteWithPositions>,
    val senderId: UserId,
    val senderLogin: UserName,
    val senderName: DisplayName,
    val pinnedByName: DisplayName,
    val startsAt: Instant,
    val endsAt: Instant?,
)

@Single
class PinnedMessageRepository(
    private val helixApiClient: HelixApiClient,
    private val channelRepository: ChannelRepository,
    private val authDataStore: AuthDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val states = ConcurrentHashMap<UserName, MutableStateFlow<PinnedMessageState>>()

    // Incremented before each fetch and on clear, so stale responses can't override newer state
    private val requestIds = ConcurrentHashMap<UserName, AtomicLong>()
    private val expiryJobs = ConcurrentHashMap<UserName, Job>()

    fun getState(channel: UserName): StateFlow<PinnedMessageState> = stateFlowOf(channel)

    suspend fun fetch(channel: UserName) {
        val channelId = channelRepository.getChannel(channel)?.id ?: return
        val moderatorId = authDataStore.userIdString ?: return
        val requestId = requestIdOf(channel).incrementAndFetch()
        helixApiClient
            .getPinnedChatMessage(channelId, moderatorId)
            .onSuccess { dto ->
                if (requestIdOf(channel).load() != requestId) {
                    return@onSuccess
                }
                applyState(channel, dto)
            }
    }

    fun clear(channel: UserName) {
        requestIdOf(channel).incrementAndFetch()
        expiryJobs.remove(channel)?.cancel()
        stateFlowOf(channel).value = PinnedMessageState.None
    }

    suspend fun pin(
        channel: UserName,
        messageId: String,
        duration: Duration?,
    ): Result<Unit> {
        val channelId = channelRepository.getChannel(channel)?.id ?: return Result.failure(IllegalStateException("Unknown channel $channel"))
        val moderatorId = authDataStore.userIdString ?: return Result.failure(IllegalStateException("Not logged in"))
        return helixApiClient
            .pinChatMessage(channelId, moderatorId, messageId, durationSeconds = duration?.inWholeSeconds)
            .onSuccess { scope.launch { fetch(channel) } }
    }

    suspend fun unpin(channel: UserName): Result<Unit> {
        val pinned = stateFlowOf(channel).value as? PinnedMessageState.Pinned ?: return Result.success(Unit)
        val channelId = channelRepository.getChannel(channel)?.id ?: return Result.failure(IllegalStateException("Unknown channel $channel"))
        val moderatorId = authDataStore.userIdString ?: return Result.failure(IllegalStateException("Not logged in"))
        return helixApiClient
            .unpinChatMessage(channelId, moderatorId, pinned.message.messageId)
            .onSuccess { clear(channel) }
    }

    fun removeChannel(channel: UserName) {
        expiryJobs.remove(channel)?.cancel()
        requestIds.remove(channel)
        states.remove(channel)
    }

    private fun applyState(
        channel: UserName,
        dto: PinnedChatMessageDto?,
    ) {
        expiryJobs.remove(channel)?.cancel()
        val message = dto?.toPinnedMessage(channel)
        val endsAt = message?.endsAt
        val state =
            when {
                message == null -> PinnedMessageState.None
                endsAt != null && endsAt <= Clock.System.now() -> PinnedMessageState.None
                else -> PinnedMessageState.Pinned(message)
            }
        stateFlowOf(channel).value = state
        if (state is PinnedMessageState.Pinned && endsAt != null) {
            expiryJobs[channel] = scope.launch {
                delay(endsAt - Clock.System.now())
                clear(channel)
            }
        }
    }

    private fun stateFlowOf(channel: UserName): MutableStateFlow<PinnedMessageState> = states.getOrPut(channel) { MutableStateFlow(PinnedMessageState.None) }

    private fun requestIdOf(channel: UserName): AtomicLong = requestIds.getOrPut(channel) { AtomicLong(0L) }

    private fun PinnedChatMessageDto.toPinnedMessage(channel: UserName): PinnedMessage = PinnedMessage(
        messageId = messageId,
        channel = channel,
        text = message.text,
        emotesWithPositions = message.fragments.toEmotesWithPositions(),
        senderId = senderUserId,
        senderLogin = senderUserLogin,
        senderName = senderUserName,
        pinnedByName = pinnedByUserName,
        startsAt = startsAt,
        endsAt = endsAt?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() },
    )
}
