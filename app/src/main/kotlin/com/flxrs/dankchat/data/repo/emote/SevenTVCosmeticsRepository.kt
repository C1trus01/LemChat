package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.seventv.SevenTVApiClient
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventApiClient
import com.flxrs.dankchat.data.api.seventv.eventapi.SevenTVEventMessage
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger("SevenTVCosmeticsRepository")

/**
 * Paints and personal emotes of 7TV subscribers. 7TV announces them per channel, once a client of the subscriber reported
 * being in that channel with a presence, which this client also does for the logged in user when a message is sent.
 */
@Single
class SevenTVCosmeticsRepository(
    private val sevenTVEventApiClient: SevenTVEventApiClient,
    private val sevenTVApiClient: SevenTVApiClient,
    private val emoteRepository: EmoteRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val authDataStore: AuthDataStore,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val channels = ConcurrentHashMap<UserName, UserId>()
    private val paints = ConcurrentHashMap<String, SevenTVPaint>()
    private val userPaintIds = ConcurrentHashMap<UserId, String>()

    // Mentions only know the name of a user
    private val userNamePaintIds = ConcurrentHashMap<UserName, String>()

    // Twitch user id to 7TV user id, NO_SEVEN_TV_USER for users without an account
    private val sevenTVUserIds = ConcurrentHashMap<UserId, String>()
    private val lastPresences = ConcurrentHashMap<UserId, TimeMark>()

    private val usersToReparse = ConcurrentHashMap.newKeySet<UserId>()
    private val usersToRemap = ConcurrentHashMap.newKeySet<UserId>()
    private val mentionsToRemap = ConcurrentHashMap.newKeySet<UserName>()
    private val updateRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        scope.launch {
            sevenTVEventApiClient.messages.collect { handleEvent(it) }
        }
        // Busy channels announce cosmetics constantly, a debounce would never settle
        scope.launch {
            updateRequests.sample(UPDATE_INTERVAL).collect {
                val reparsedUsers = usersToReparse.drain()
                if (reparsedUsers.isNotEmpty()) {
                    chatMessageRepository.reparseMessagesOfUsers(reparsedUsers)
                }
                val remappedUsers = usersToRemap.drain()
                val remappedMentions = mentionsToRemap.drain()
                if (remappedUsers.isNotEmpty() || remappedMentions.isNotEmpty()) {
                    chatMessageRepository.remapMessagesOfUsers(remappedUsers, remappedMentions)
                }
            }
        }
        scope.launch {
            chatSettingsDataStore.settings
                .map { it.isSevenTVCosmeticsEnabled }
                .distinctUntilChanged()
                .drop(1)
                .collect { enabled ->
                    channels.values.forEach { channelId ->
                        when {
                            enabled -> sevenTVEventApiClient.subscribeChannelCosmetics(channelId.value)
                            else -> sevenTVEventApiClient.unsubscribeChannelCosmetics(channelId.value)
                        }
                    }
                }
        }
        scope.launch {
            chatSettingsDataStore.settings
                .map { it.sevenTVPersonalEmotes }
                .distinctUntilChanged()
                .drop(1)
                .collect { chatMessageRepository.reparseAllEmotesAndBadges() }
        }
    }

    fun paintFor(userId: UserId): SevenTVPaint? = userPaintIds[userId]?.let { paints[it] }

    fun paintForName(userName: UserName): SevenTVPaint? = userNamePaintIds[userName.lowercase()]?.let { paints[it] }

    suspend fun subscribeChannel(
        channel: UserName,
        channelId: UserId,
    ) {
        channels[channel] = channelId
        if (chatSettingsDataStore.current().isSevenTVCosmeticsEnabled) {
            sevenTVEventApiClient.subscribeChannelCosmetics(channelId.value)
        }
    }

    suspend fun unsubscribeChannel(channel: UserName) {
        val channelId = channels.remove(channel) ?: return
        sevenTVEventApiClient.unsubscribeChannelCosmetics(channelId.value)
    }

    fun onMessageSent(channel: UserName) {
        if (!chatSettingsDataStore.current().sevenTVSendPresence) {
            return
        }

        val userId = authDataStore.userIdString ?: return
        val channelId = channels[channel] ?: return
        val lastPresence = lastPresences[channelId]
        if (lastPresence != null && lastPresence.elapsedNow() < PRESENCE_INTERVAL) {
            return
        }

        lastPresences[channelId] = TimeSource.Monotonic.markNow()
        scope.launch {
            val sevenTVUserId = sevenTVUserIdOf(userId) ?: return@launch
            sevenTVApiClient
                .sendChannelPresence(sevenTVUserId, channelId)
                .onFailure { logger.debug(it) { "Failed to send presence" } }
        }
    }

    private suspend fun sevenTVUserIdOf(userId: UserId): String? {
        val known = sevenTVUserIds[userId]
        if (known != null) {
            return known.takeUnless { it == NO_SEVEN_TV_USER }
        }

        val user =
            sevenTVApiClient.getSevenTVChannelEmotes(userId).getOrElse {
                logger.debug(it) { "Failed to look up 7TV user" }
                return null
            }
        sevenTVUserIds[userId] = user?.user?.id ?: NO_SEVEN_TV_USER
        return user?.user?.id
    }

    private fun handleEvent(event: SevenTVEventMessage) {
        when (event) {
            is SevenTVEventMessage.PersonalEmoteSetCreated -> {
                emoteRepository.createPersonalEmoteSet(event.emoteSetId)
            }

            is SevenTVEventMessage.PersonalEmoteSetAssigned -> {
                requestReparse(emoteRepository.assignPersonalEmoteSet(event.emoteSetId, event.userIds))
            }

            is SevenTVEventMessage.PersonalEmoteSetUpdated -> {
                requestReparse(emoteRepository.updatePersonalEmoteSet(event))
            }

            is SevenTVEventMessage.PersonalEmoteSetRemoved -> {
                emoteRepository.removePersonalEmoteSetEntitlement(event.emoteSetId, event.userIds)
            }

            is SevenTVEventMessage.BadgeCreated -> {
                requestReparse(emoteRepository.setSevenTVBadge(event.badgeId, event.title, event.url))
            }

            is SevenTVEventMessage.BadgeAssigned -> {
                requestReparse(emoteRepository.assignSevenTVBadge(event.badgeId, event.userIds))
            }

            is SevenTVEventMessage.BadgeRemoved -> {
                requestReparse(emoteRepository.removeSevenTVBadge(event.badgeId, event.userIds))
            }

            is SevenTVEventMessage.PaintCreated -> {
                val paint = event.paint.toPaint() ?: return
                if (paints.put(paint.id, paint) != paint) {
                    requestRemap(userPaintIds.filterValues { it == paint.id }.keys, userNamePaintIds.filterValues { it == paint.id }.keys)
                }
            }

            is SevenTVEventMessage.PaintAssigned -> {
                val changed = event.userIds.filterTo(mutableSetOf()) { userPaintIds.put(it, event.paintId) != event.paintId }
                val changedNames = event.userNames.filterTo(mutableSetOf()) { userNamePaintIds.put(it, event.paintId) != event.paintId }
                requestRemap(changed, changedNames)
            }

            is SevenTVEventMessage.PaintRemoved -> {
                val removed = event.userIds.filterTo(mutableSetOf()) { userPaintIds.remove(it, event.paintId) }
                val removedNames = event.userNames.filterTo(mutableSetOf()) { userNamePaintIds.remove(it, event.paintId) }
                requestRemap(removed, removedNames)
            }

            is SevenTVEventMessage.EmoteSetUpdated,
            is SevenTVEventMessage.UserUpdated,
            -> Unit
        }
    }

    private fun requestReparse(userIds: Collection<UserId>) {
        if (userIds.isNotEmpty()) {
            usersToReparse += userIds
            updateRequests.tryEmit(Unit)
        }
    }

    private fun requestRemap(
        userIds: Collection<UserId>,
        mentionedNames: Collection<UserName>,
    ) {
        if (userIds.isNotEmpty() || mentionedNames.isNotEmpty()) {
            usersToRemap += userIds
            mentionsToRemap += mentionedNames
            updateRequests.tryEmit(Unit)
        }
    }

    private fun <T> MutableSet<T>.drain(): Set<T> = toSet().also { removeAll(it) }

    companion object {
        private const val NO_SEVEN_TV_USER = ""
        private val UPDATE_INTERVAL = 2.seconds
        private val PRESENCE_INTERVAL = 60.seconds
    }
}
