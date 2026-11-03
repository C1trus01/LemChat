package com.flxrs.dankchat.data.auth

import android.webkit.CookieManager
import android.webkit.WebStorage
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.api.ApiException
import com.flxrs.dankchat.data.api.auth.AuthApiClient
import com.flxrs.dankchat.data.repo.IgnoresRepository
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.chat.ChatEventProcessor
import com.flxrs.dankchat.data.repo.chat.ChatNotificationRepository
import com.flxrs.dankchat.data.repo.chat.SendWaitRepository
import com.flxrs.dankchat.data.repo.chat.UserStateRepository
import com.flxrs.dankchat.data.repo.chat.UsersRepository
import com.flxrs.dankchat.data.repo.emote.EmoteRepository
import com.flxrs.dankchat.data.repo.emote.EmoteUsageRepository
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.domain.ChannelDataCoordinator
import com.flxrs.dankchat.utils.extensions.withoutOAuthPrefix
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

sealed interface AuthEvent {
    data class LoggedIn(
        val userName: UserName,
    ) : AuthEvent

    data class ScopesOutdated(
        val userName: UserName,
    ) : AuthEvent

    data object TokenInvalid : AuthEvent

    data object ValidationFailed : AuthEvent

    data class AccountExpired(
        val userName: UserName,
    ) : AuthEvent
}

private val logger = KotlinLogging.logger("AuthStateCoordinator")

@Single
class AuthStateCoordinator(
    private val authDataStore: AuthDataStore,
    private val chatChannelProvider: ChatChannelProvider,
    private val chatConnector: ChatConnector,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val emoteRepository: EmoteRepository,
    private val authApiClient: AuthApiClient,
    private val ignoresRepository: IgnoresRepository,
    private val userStateRepository: UserStateRepository,
    private val emoteUsageRepository: EmoteUsageRepository,
    private val startupValidationHolder: StartupValidationHolder,
    private val chatEventProcessor: ChatEventProcessor,
    private val chatNotificationRepository: ChatNotificationRepository,
    private val sendWaitRepository: SendWaitRepository,
    private val usersRepository: UsersRepository,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // React to login state changes — handles both login and logout.
        // distinctUntilChangedBy on isLoggedIn+oAuthKey solves re-login (new token = new oAuthKey).
        // The initial emission is skipped (startup connection handled by ConnectionCoordinator).
        // Observes currentSettings since the reconnect reads the credentials from it, they must not be stale.
        scope.launch {
            var previousUserId: String? = null
            authDataStore.currentSettings
                .distinctUntilChangedBy { it.isLoggedIn to it.oAuthKey }
                .collectIndexed { index, settings ->
                    val accountChanged = settings.userId != previousUserId
                    previousUserId = settings.userId
                    if (index == 0) {
                        return@collectIndexed
                    }

                    when {
                        settings.isLoggedIn -> {
                            if (accountChanged) {
                                emoteRepository.clearTwitchEmotes()
                                ignoresRepository.clearIgnores()
                                clearUserData()
                            }
                            startupValidationHolder.update(StartupValidation.Validated)
                            chatConnector.closeAndReconnect(chatChannelProvider.channels.value.orEmpty())
                            channelDataCoordinator.reloadGlobalData()
                            settings.userName?.let { name ->
                                _events.send(AuthEvent.LoggedIn(UserName(name)))
                            }
                        }

                        else -> {
                            channelDataCoordinator.cancelGlobalLoading()
                            emoteRepository.clearTwitchEmotes()
                            clearUserData()
                            chatConnector.closeAndReconnect(chatChannelProvider.channels.value.orEmpty())
                        }
                    }
                }
        }
    }

    // Chat state tied to the previous account must not carry over to the next one
    private fun clearUserData() {
        userStateRepository.clear()
        chatEventProcessor.clearUserData()
        chatNotificationRepository.clearUserData()
        sendWaitRepository.clearAll()
        usersRepository.clearGlobalUsers()
    }

    suspend fun validateOnStartup(): AuthEvent? {
        if (!authDataStore.isLoggedIn) {
            startupValidationHolder.update(StartupValidation.Validated)
            return null
        }

        val token = authDataStore.oAuthKey?.withoutOAuthPrefix ?: return null
        val result =
            authApiClient.validateUser(token).fold(
                onSuccess = { validateDto ->
                    // Update username from validation response
                    authDataStore.update { it.copy(userName = validateDto.login.value) }
                    when {
                        authApiClient.validateScopes(validateDto.scopes.orEmpty()) -> AuthEvent.LoggedIn(validateDto.login)
                        else -> AuthEvent.ScopesOutdated(validateDto.login)
                    }
                },
                onFailure = { throwable ->
                    when {
                        throwable is ApiException && throwable.status == HttpStatusCode.Unauthorized -> {
                            AuthEvent.TokenInvalid
                        }

                        else -> {
                            logger.error { "Failed to validate token: ${throwable.message}" }
                            AuthEvent.ValidationFailed
                        }
                    }
                },
            )

        startupValidationHolder.update(
            when (result) {
                is AuthEvent.LoggedIn,
                is AuthEvent.ValidationFailed,
                is AuthEvent.AccountExpired,
                -> StartupValidation.Validated

                is AuthEvent.ScopesOutdated -> StartupValidation.ScopesOutdated(result.userName)

                AuthEvent.TokenInvalid -> StartupValidation.TokenInvalid
            },
        )

        // Only send snackbar-worthy events through the channel
        when (result) {
            is AuthEvent.LoggedIn,
            is AuthEvent.ValidationFailed,
            -> _events.send(result)

            else -> Unit
        }

        return result
    }

    // A saved token can expire while unused, so it is checked before leaving the working account
    fun switchAccount(userId: String) {
        scope.launch {
            val settings = authDataStore.current()
            if (settings.isLoggedIn && settings.userId == userId) {
                return@launch
            }

            val account = settings.allAccounts.find { it.userId == userId } ?: return@launch
            authApiClient.validateUser(account.oAuthKey.withoutOAuthPrefix).fold(
                onSuccess = { authDataStore.switchAccount(account) },
                onFailure = { throwable ->
                    logger.error { "Failed to validate token of saved account: ${throwable.message}" }
                    val event =
                        when {
                            throwable is ApiException && throwable.status == HttpStatusCode.Unauthorized -> AuthEvent.AccountExpired(UserName(account.userName))
                            else -> AuthEvent.ValidationFailed
                        }
                    _events.send(event)
                },
            )
        }
    }

    fun removeAccount(userId: String) {
        if (authDataStore.current().userId == userId) {
            logout()
            return
        }

        scope.launch { authDataStore.removeAccount(userId) }
    }

    // Twitch would otherwise skip the sign in form and authorize the account of the current web session again
    fun clearWebSession(onCleared: () -> Unit) {
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies { onCleared() }
    }

    fun logout() {
        scope.launch {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()

            userStateRepository.clear()
            ignoresRepository.clearIgnores()
            emoteUsageRepository.clearUsages()

            // Setting isLoggedIn = false triggers the settings observer which handles
            // clearing twitch emotes and reconnecting anonymously.
            authDataStore.clearLogin()
        }
    }
}
