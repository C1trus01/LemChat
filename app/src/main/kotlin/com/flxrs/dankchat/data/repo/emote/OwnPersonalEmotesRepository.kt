package com.flxrs.dankchat.data.repo.emote

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.api.seventv.SevenTVApiClient
import com.flxrs.dankchat.data.api.seventv.dto.SevenTVEmoteSetDto
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.data.toUserId
import com.flxrs.dankchat.data.twitch.emote.GenericEmote
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.AppLifecycleListener
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger("OwnPersonalEmotesRepository")

/**
 * The 7TV personal emotes of the logged in user. They are looked up instead of waiting for an event, which
 * 7TV only sends after a presence in a channel. Looking again while the app is open is what makes them go
 * away soon after a subscription ends.
 */
@Single
class OwnPersonalEmotesRepository(
    private val sevenTVApiClient: SevenTVApiClient,
    private val emoteRepository: EmoteRepository,
    private val chatMessageRepository: ChatMessageRepository,
    authDataStore: AuthDataStore,
    chatSettingsDataStore: ChatSettingsDataStore,
    appLifecycleListener: AppLifecycleListener,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private var lastLookup: Lookup? = null

    val emotes: StateFlow<List<GenericEmote>> = emoteRepository.ownPersonalEmotes

    init {
        val ownUserId =
            authDataStore.settings
                .map { auth -> auth.userId?.takeIf { auth.isLoggedIn }?.toUserId() }
                .distinctUntilChanged()
        val isEnabled =
            chatSettingsDataStore.settings
                .map { it.sevenTVPersonalEmotes }
                .distinctUntilChanged()
        val isForeground =
            appLifecycleListener.appState
                .map { it == AppLifecycleListener.AppLifecycle.Foreground }
                .distinctUntilChanged()

        scope.launch {
            combine(ownUserId, isEnabled, isForeground, ::LookupRequest).collectLatest { request ->
                when {
                    request.userId == null || !request.isEnabled -> {
                        lastLookup = null
                        apply(userId = null, set = null)
                    }

                    request.isForeground -> {
                        lookUpRepeatedly(request.userId)
                    }
                }
            }
        }
    }

    private suspend fun lookUpRepeatedly(userId: UserId) {
        // After switching accounts the emotes of the previous one must not stay until the answer arrives
        if (lastLookup?.userId != userId) {
            apply(userId = null, set = null)
        }
        while (true) {
            val last = lastLookup
            if (last != null && last.userId == userId) {
                val remaining = LOOKUP_INTERVAL - last.time.elapsedNow()
                if (remaining.isPositive()) {
                    delay(remaining)
                }
            }
            lastLookup = Lookup(userId, TimeSource.Monotonic.markNow())
            lookUp(userId)
        }
    }

    // A failed request keeps what is shown, only an answer can take the emotes away
    private suspend fun lookUp(userId: UserId) {
        val sevenTVUser =
            sevenTVApiClient.getSevenTVChannelEmotes(userId).getOrElse {
                logger.debug(it) { "Failed to look up 7TV user" }
                return
            }
        if (sevenTVUser == null) {
            apply(userId, set = null)
            return
        }

        val user =
            sevenTVApiClient.getSevenTVUser(sevenTVUser.user.id).getOrElse {
                logger.debug(it) { "Failed to load 7TV user" }
                return
            }
        val setId = user.activePersonalEmoteSetId
        if (setId == null) {
            apply(userId, set = null)
            return
        }

        val set =
            sevenTVApiClient.getSevenTVEmoteSet(setId).getOrElse {
                logger.debug(it) { "Failed to load personal emote set" }
                return
            }
        apply(userId, set)
    }

    private suspend fun apply(
        userId: UserId?,
        set: SevenTVEmoteSetDto?,
    ) {
        val changedUsers = emoteRepository.setOwnPersonalEmotes(userId, set)
        if (changedUsers.isNotEmpty()) {
            chatMessageRepository.reparseMessagesOfUsers(changedUsers)
        }
    }

    private data class LookupRequest(
        val userId: UserId?,
        val isEnabled: Boolean,
        val isForeground: Boolean,
    )

    private data class Lookup(
        val userId: UserId,
        val time: TimeMark,
    )

    private companion object {
        val LOOKUP_INTERVAL = 15.minutes
    }
}
