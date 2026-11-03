package com.flxrs.dankchat.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.repo.update.AppUpdate
import com.flxrs.dankchat.data.repo.update.AppUpdateRepository
import com.flxrs.dankchat.data.repo.update.AppUpdateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class AppUpdateViewModel(
    private val repository: AppUpdateRepository,
) : ViewModel() {
    val state: StateFlow<AppUpdateState> = repository.state

    val prompt: StateFlow<AppUpdateState?> =
        combine(repository.state, repository.dismissedVersion) { state, dismissedVersion ->
            state.takeIf { it.update?.version.let { version -> version != null && version != dismissedVersion } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun checkIfDue() = repository.checkIfDue()

    fun dismissPrompt(version: String) = repository.dismissPrompt(version)

    /** The one thing that moves the update forward from where it is. */
    fun advance() {
        when (repository.state.value) {
            is AppUpdateState.Available, is AppUpdateState.DownloadFailed -> repository.download()
            is AppUpdateState.ReadyToInstall -> repository.install()
            AppUpdateState.Checking, is AppUpdateState.Downloading -> Unit
            AppUpdateState.Idle, AppUpdateState.UpToDate, AppUpdateState.CheckFailed, is AppUpdateState.NotInstallable -> repository.check()
        }
    }
}

internal val AppUpdateState.update: AppUpdate?
    get() = when (this) {
        is AppUpdateState.Available -> update
        is AppUpdateState.Downloading -> update
        is AppUpdateState.ReadyToInstall -> update
        is AppUpdateState.DownloadFailed -> update
        is AppUpdateState.NotInstallable -> update
        AppUpdateState.Idle, AppUpdateState.Checking, AppUpdateState.UpToDate, AppUpdateState.CheckFailed -> null
    }
