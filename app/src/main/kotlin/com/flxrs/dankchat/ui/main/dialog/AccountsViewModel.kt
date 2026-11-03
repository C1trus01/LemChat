package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.auth.AuthDataStore
import com.flxrs.dankchat.data.auth.AuthSettings
import com.flxrs.dankchat.data.auth.AuthStateCoordinator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@Immutable
data class AccountItem(
    val userId: String,
    val name: String,
    val isActive: Boolean,
)

@KoinViewModel
class AccountsViewModel(
    authDataStore: AuthDataStore,
    private val authStateCoordinator: AuthStateCoordinator,
) : ViewModel() {
    val accounts: StateFlow<ImmutableList<AccountItem>> =
        authDataStore.settings
            .map { it.toAccountItems() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authDataStore.current().toAccountItems())

    fun switchAccount(userId: String) = authStateCoordinator.switchAccount(userId)

    fun removeAccount(userId: String) = authStateCoordinator.removeAccount(userId)

    fun prepareAddAccount(onReady: () -> Unit) = authStateCoordinator.clearWebSession(onReady)

    private fun AuthSettings.toAccountItems(): ImmutableList<AccountItem> = allAccounts
        .map { account ->
            AccountItem(
                userId = account.userId,
                name = account.displayName ?: account.userName,
                isActive = isLoggedIn && account.userId == userId,
            )
        }.toImmutableList()
}
