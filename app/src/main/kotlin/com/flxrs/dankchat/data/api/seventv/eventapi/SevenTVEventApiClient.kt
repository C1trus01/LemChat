package com.flxrs.dankchat.data.api.seventv.eventapi

import com.flxrs.dankchat.data.api.seventv.eventapi.dto.AckMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.CosmeticCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DataMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.DispatchMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetChangeField
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EmoteSetDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EndOfStreamMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EntitlementCreateDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.EntitlementDeleteDispatchData
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.HeartbeatMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.HelloMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.ReconnectMessage
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVBadgeDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SubscribeRequest
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.UnsubscribeRequest
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.UserDispatchData
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.preferences.battery.BatterySettingsDataStore
import com.flxrs.dankchat.preferences.chat.ChatSettingsDataStore
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle.Background
import com.flxrs.dankchat.utils.ForegroundServiceState
import com.flxrs.dankchat.utils.extensions.timer
import com.flxrs.dankchat.utils.webSocketCoroutineExceptionHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.util.collections.ConcurrentSet
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.annotation.Single
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger("SevenTVEventApiClient")

@OptIn(DelicateCoroutinesApi::class)
@Single
class SevenTVEventApiClient(
    httpClient: HttpClient,
    private val chatSettingsDataStore: ChatSettingsDataStore,
    private val batterySettingsDataStore: BatterySettingsDataStore,
    private val appLifecycleListener: AppLifecycleListener,
    private val foregroundServiceState: ForegroundServiceState,
    defaultJson: Json,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val client =
        httpClient.config {
            install(WebSockets)
        }

    private val json =
        Json(defaultJson) {
            encodeDefaults = true
        }

    @Volatile
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null

    @Volatile
    private var lastHeartBeat: TimeMark = TimeSource.Monotonic.markNow()

    private val subscriptions = ConcurrentSet<SubscribeRequest>()
    private val _messages = MutableSharedFlow<SevenTVEventMessage>(extraBufferCapacity = 16)

    private val connected: Boolean
        get() = session?.isActive == true && session?.incoming?.isClosedForReceive == false

    init {
        scope.launch {
            chatSettingsDataStore.debouncedSevenTvLiveEmoteUpdates
                .collectLatest { enabled ->
                    when {
                        enabled -> start()
                        else -> close()
                    }
                    if (enabled) {
                        appLifecycleListener.appState
                            .debounce(FLOW_DEBOUNCE)
                            .collectLatest { state ->
                                if (state == Background) {
                                    val batterySettings = batterySettingsDataStore.settings.first()
                                    if (!batterySettings.pauseSevenTvLiveUpdates) {
                                        return@collectLatest
                                    }

                                    val timeout = batterySettings.backgroundDelay.duration
                                    logger.debug { "[7TV Event-Api] Sleeping for $timeout until connection is closed" }
                                    delay(timeout)
                                    close()
                                }
                            }
                    }
                }
        }
    }

    val messages = _messages.asSharedFlow()

    suspend fun subscribeUser(userId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        val request = SubscribeRequest.userUpdates(userId)
        addSubscription(request)
    }

    suspend fun subscribeEmoteSet(emoteSetId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        val request = SubscribeRequest.emoteSetUpdates(emoteSetId)
        addSubscription(request)
    }

    suspend fun unsubscribeUser(userId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        val request = UnsubscribeRequest.userUpdates(userId)
        removeSubscription(request)
    }

    suspend fun unsubscribeEmoteSet(emoteSetId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        val request = UnsubscribeRequest.emoteSetUpdates(emoteSetId)
        removeSubscription(request)
    }

    suspend fun subscribeChannelCosmetics(channelId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        SubscribeRequest.channelCosmetics(channelId).forEach(::addSubscription)
    }

    suspend fun unsubscribeChannelCosmetics(channelId: String) {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        UnsubscribeRequest.channelCosmetics(channelId).forEach(::removeSubscription)
    }

    suspend fun reconnect() {
        if (!chatSettingsDataStore.settings.first().sevenTVLiveEmoteUpdates) {
            return
        }

        close()
        start()
    }

    suspend fun reconnectIfNecessary() {
        if (connected || connectionJob?.isActive == true) {
            return
        }

        reconnect()
    }

    fun close() {
        val currentSession = session
        session = null
        connectionJob?.cancel()
        scope.launch {
            runCatching {
                currentSession?.close()
                currentSession?.cancel()
            }
        }
    }

    private fun addSubscription(request: SubscribeRequest) {
        if (subscriptions.add(request) && connected) {
            val encoded = request.encodeOrNull() ?: return
            val currentSession = session
            scope.launch { runCatching { currentSession?.send(Frame.Text(encoded)) } }
        }
    }

    private fun removeSubscription(request: UnsubscribeRequest) {
        val subscription = subscriptions.find { it.d == request.d } ?: return
        if (subscriptions.remove(subscription) && connected) {
            val encoded = request.encodeOrNull() ?: return
            val currentSession = session
            scope.launch { runCatching { currentSession?.send(Frame.Text(encoded)) } }
        }
    }

    private fun start() {
        if (connected || connectionJob?.isActive == true) {
            return
        }

        logger.info { "[7TV Event-Api] connecting" }
        connectionJob =
            scope.launch(webSocketCoroutineExceptionHandler("7TV Event-Api")) {
                var retryCount = 1
                while (isActive) {
                    var serverRequestedReconnect = false
                    try {
                        client.webSocket(EVENT_API_URL) {
                            session = this
                            retryCount = 1
                            logger.info { "[7TV Event-Api] connected" }

                            var heartBeatJob: Job? = null
                            try {
                                while (isActive) {
                                    val result = incoming.receiveCatching()
                                    val text =
                                        when (val frame = result.getOrNull()) {
                                            null -> {
                                                val cause = result.exceptionOrNull() ?: return@webSocket
                                                throw cause
                                            }

                                            else -> {
                                                (frame as? Frame.Text)?.readText() ?: continue
                                            }
                                        }

                                    val message =
                                        runCatching { json.decodeFromString<DataMessage>(text) }.getOrElse {
                                            logger.debug(it) { "Failed to parse incoming message" }
                                            continue
                                        }

                                    when (message) {
                                        is HelloMessage -> {
                                            lastHeartBeat = TimeSource.Monotonic.markNow()
                                            heartBeatJob?.cancel()
                                            heartBeatJob = setupHeartBeatWatchdog(message.d.heartBeatInterval.milliseconds)

                                            subscriptions.forEach { subscription ->
                                                val encoded = subscription.encodeOrNull() ?: return@forEach
                                                send(Frame.Text(encoded))
                                            }
                                        }

                                        is HeartbeatMessage -> {
                                            lastHeartBeat = TimeSource.Monotonic.markNow()
                                        }

                                        is DispatchMessage -> {
                                            message.handleMessage()
                                        }

                                        is ReconnectMessage -> {
                                            serverRequestedReconnect = true
                                            return@webSocket
                                        }

                                        is EndOfStreamMessage -> Unit

                                        is AckMessage -> Unit
                                    }
                                }
                            } finally {
                                heartBeatJob?.cancel()
                            }
                        }

                        session = null
                        if (!foregroundServiceState.active.value) {
                            logger.info { "[7TV Event-Api] connection closed, foreground service inactive, not reconnecting" }
                            return@launch
                        }

                        when {
                            serverRequestedReconnect -> logger.info { "[7TV Event-Api] reconnecting after server request" }

                            else -> {
                                // Graceful server close (close frame/END_OF_STREAM). Reconnecting is allowed here because
                                // the background limit collector cancels this loop via close() once the limit expires.
                                logger.info { "[7TV Event-Api] connection closed by server, reconnecting" }
                                delay(RECONNECT_BASE_DELAY + randomJitter())
                            }
                        }
                    } catch (t: CancellationException) {
                        throw t
                    } catch (t: Throwable) {
                        logger.error { "[7TV Event-Api] connection failed: $t" }
                        session = null

                        if (!foregroundServiceState.active.value) {
                            logger.info { "[7TV Event-Api] foreground service inactive, not retrying" }
                            return@launch
                        }

                        logger.info { "[7TV Event-Api] attempting to reconnect #$retryCount.." }
                        val reconnectDelay = RECONNECT_BASE_DELAY * (1 shl (retryCount - 1))
                        delay(reconnectDelay + randomJitter())
                        retryCount = (retryCount + 1).coerceAtMost(RECONNECT_MAX_ATTEMPTS)
                    }
                }
            }
    }

    private fun setupHeartBeatWatchdog(interval: Duration): Job = scope.timer(interval) {
        if (lastHeartBeat.elapsedNow() > interval * 3) {
            logger.info { "[7TV Event-Api] missed heartbeats, reconnecting" }
            cancel()
            reconnect()
        }
    }

    private fun randomJitter() = Random.nextLong(range = 0L..MAX_JITTER).milliseconds

    private suspend fun DispatchMessage.handleMessage() {
        when (d) {
            is EmoteSetDispatchData -> {
                with(d.body) {
                    val emoteSetId = id
                    val added =
                        pushed?.mapNotNull {
                            it.value ?: return@mapNotNull null
                        }
                    val removed =
                        pulled?.mapNotNull {
                            val removedData = it.oldValue ?: return@mapNotNull null
                            SevenTVEventMessage.EmoteSetUpdated.RemovedEmote(removedData.id, removedData.name)
                        }
                    val updated =
                        updated?.mapNotNull {
                            val newData = it.value ?: return@mapNotNull null
                            val oldData = it.oldValue ?: return@mapNotNull null
                            SevenTVEventMessage.EmoteSetUpdated.UpdatedEmote(newData.id, newData.name, oldData.name)
                        }
                    val actorName = actor?.displayName
                    _messages.emit(
                        when (actorName) {
                            null -> {
                                SevenTVEventMessage.PersonalEmoteSetUpdated(
                                    emoteSetId = emoteSetId,
                                    added = added.orEmpty(),
                                    removed = removed.orEmpty(),
                                    updated = updated.orEmpty(),
                                )
                            }

                            else -> {
                                SevenTVEventMessage.EmoteSetUpdated(
                                    emoteSetId = emoteSetId,
                                    actorName = actorName,
                                    added = added.orEmpty(),
                                    removed = removed.orEmpty(),
                                    updated = updated.orEmpty(),
                                )
                            }
                        },
                    )
                }
            }

            is UserDispatchData -> {
                with(d.body) {
                    val actorName = actor.displayName
                    updated?.forEach { change ->
                        val index = change.index
                        val emoteSetChange = change.value?.filterIsInstance<EmoteSetChangeField>()?.firstOrNull() ?: return@forEach
                        _messages.emit(
                            SevenTVEventMessage.UserUpdated(
                                actorName = actorName,
                                connectionIndex = index,
                                emoteSetId = emoteSetChange.value.id,
                                oldEmoteSetId = emoteSetChange.oldValue.id,
                            ),
                        )
                    }
                }
            }

            is EmoteSetCreateDispatchData -> {
                val emoteSet = d.body.emoteSet
                if (emoteSet.isPersonalOrCommercial) {
                    _messages.emit(SevenTVEventMessage.PersonalEmoteSetCreated(emoteSet.id))
                }
            }

            is CosmeticCreateDispatchData -> {
                val cosmetic = d.body.cosmetic
                when (cosmetic.kind) {
                    COSMETIC_KIND_PAINT -> {
                        val paint =
                            runCatching { json.decodeFromJsonElement<SevenTVPaintDto>(cosmetic.data) }.getOrElse {
                                logger.debug(it) { "Failed to parse paint" }
                                return
                            }
                        _messages.emit(SevenTVEventMessage.PaintCreated(paint))
                    }

                    COSMETIC_KIND_BADGE -> {
                        val badge =
                            runCatching { json.decodeFromJsonElement<SevenTVBadgeDto>(cosmetic.data) }.getOrElse {
                                logger.debug(it) { "Failed to parse badge" }
                                return
                            }
                        val url = badge.imageUrl ?: return
                        _messages.emit(SevenTVEventMessage.BadgeCreated(badge.id, badge.tooltip, url))
                    }
                }
            }

            is EntitlementCreateDispatchData -> {
                val entitlement = d.body.entitlement
                when (entitlement.kind) {
                    COSMETIC_KIND_PAINT -> _messages.emit(SevenTVEventMessage.PaintAssigned(entitlement.refId, entitlement.twitchUserIds, entitlement.twitchUserNames))
                    COSMETIC_KIND_EMOTE_SET -> _messages.emit(SevenTVEventMessage.PersonalEmoteSetAssigned(entitlement.refId, entitlement.twitchUserIds))
                    COSMETIC_KIND_BADGE -> _messages.emit(SevenTVEventMessage.BadgeAssigned(entitlement.refId, entitlement.twitchUserIds))
                }
            }

            is EntitlementDeleteDispatchData -> {
                val entitlement = d.body.entitlement
                when (entitlement.kind) {
                    COSMETIC_KIND_PAINT -> _messages.emit(SevenTVEventMessage.PaintRemoved(entitlement.refId, entitlement.twitchUserIds, entitlement.twitchUserNames))
                    COSMETIC_KIND_EMOTE_SET -> _messages.emit(SevenTVEventMessage.PersonalEmoteSetRemoved(entitlement.refId, entitlement.twitchUserIds))
                    COSMETIC_KIND_BADGE -> _messages.emit(SevenTVEventMessage.BadgeRemoved(entitlement.refId, entitlement.twitchUserIds))
                }
            }
        }
    }

    private inline fun <reified T> T.encodeOrNull(): String? = runCatching { json.encodeToString(this) }.getOrNull()

    data class Status(
        val connected: Boolean,
        val subscriptionCount: Int,
    )

    fun status(): Status = Status(connected = connected, subscriptionCount = subscriptions.size)

    companion object {
        private const val EVENT_API_URL = "wss://events.7tv.io/v3"
        private const val COSMETIC_KIND_PAINT = "PAINT"
        private const val COSMETIC_KIND_EMOTE_SET = "EMOTE_SET"
        private const val COSMETIC_KIND_BADGE = "BADGE"
        private const val MAX_JITTER = 250L
        private val RECONNECT_BASE_DELAY = 1.seconds
        private const val RECONNECT_MAX_ATTEMPTS = 6
        private val FLOW_DEBOUNCE = 2.seconds
    }
}
