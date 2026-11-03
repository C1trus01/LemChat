package com.flxrs.dankchat.ui.crash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.flxrs.dankchat.data.repo.crash.CrashEntry
import com.flxrs.dankchat.data.repo.crash.CrashRepository
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import com.flxrs.dankchat.ui.main.CrashViewer
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class CrashViewerViewModel(
    savedStateHandle: SavedStateHandle,
    private val crashRepository: CrashRepository,
    private val preferenceStore: DankChatPreferenceStore,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CrashViewer>()

    val crashEntry: CrashEntry? = when {
        route.crashId != 0L -> crashRepository.getCrash(route.crashId)
        else -> crashRepository.getMostRecentCrash()
    }

    val userName: String? get() = preferenceStore.userName?.value
    val userId: String? get() = preferenceStore.userIdString?.value

    fun buildEmailBody(): String? {
        val entry = crashEntry ?: return null
        return crashRepository.buildEmailBody(entry, userName, userId)
    }

    fun deleteCrash() {
        val entry = crashEntry ?: return
        crashRepository.deleteCrash(entry.id)
    }
}
