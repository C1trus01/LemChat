package com.flxrs.dankchat.domain

import com.flxrs.dankchat.data.auth.AuthEvent
import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.auth.StartupValidationHolder
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatConnector
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.data.state.GlobalLoadingState
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.utils.AppLifecycleListener
import com.flxrs.dankchat.utils.AppLifecycleListener.AppLifecycle
import com.flxrs.dankchat.utils.ForegroundServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class ConnectionCoordinator(
    private val chatConnector: ChatConnector,
    private val dataRepository: DataRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val channelDataCoordinator: ChannelDataCoordinator,
    private val authStateCoordinator: AuthStateCoordinator,
    private val startupValidationHolder: StartupValidationHolder,
    private val appLifecycleListener: AppLifecycleListener,
    private val foregroundServiceState: ForegroundServiceState,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)

    fun initialize() {
        scope.launch {
            val result = authStateCoordinator.validateOnStartup()
            when (result) {
                is AuthEvent.TokenInvalid -> Unit
                else -> chatConnector.connectAndJoin(chatChannelProvider.channels.value.orEmpty())
            }
        }

        scope.launch {
            startupValidationHolder.awaitResolved()
            var wasInBackground = false
            appLifecycleListener.appState.collect { state ->
                when (state) {
                    is AppLifecycle.Background -> {
                        wasInBackground = true
                    }

                    is AppLifecycle.Foreground -> {
                        if (wasInBackground) {
                            wasInBackground = false
                            foregroundServiceState.setActive(true)
                            chatConnector.reconnectIfNecessary()
                            dataRepository.reconnectIfNecessary()

                            val loadingState = channelDataCoordinator.globalLoadingState.value
                            if (loadingState is GlobalLoadingState.Failed) {
                                channelDataCoordinator.retryDataLoading(loadingState)
                            }
                        }
                    }
                }
            }
        }
    }
}
