package com.flxrs.dankchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.data.DataRepository
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class DankChatViewModel(
    private val authDataStore: AuthDataStore,
    dataRepository: DataRepository,
    chatChannelProvider: ChatChannelProvider,
    private val authStateCoordinator: AuthStateCoordinator,
    appearanceSettingsDataStore: AppearanceSettingsDataStore,
) : ViewModel() {
    val serviceEvents = dataRepository.serviceEvents
    val activeChannel = chatChannelProvider.activeChannel
    val isLoggedIn: Flow<Boolean> =
        authDataStore.settings
            .map { it.isLoggedIn }
            .distinctUntilChanged()

    val keepScreenOn =
        appearanceSettingsDataStore.settings
            .map { it.keepScreenOn }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = appearanceSettingsDataStore.current().keepScreenOn,
            )

    fun checkLogin() {
        if (authDataStore.isLoggedIn && authDataStore.oAuthKey.isNullOrBlank()) {
            authStateCoordinator.logout()
        }
    }

    fun clearDataForLogout() {
        authStateCoordinator.logout()
    }
}
