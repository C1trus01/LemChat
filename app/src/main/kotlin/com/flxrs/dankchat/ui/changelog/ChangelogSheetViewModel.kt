package com.flxrs.dankchat.ui.changelog

import androidx.lifecycle.ViewModel
import com.flxrs.dankchat.preferences.DankChatPreferenceStore
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ChangelogSheetViewModel(
    dankChatPreferenceStore: DankChatPreferenceStore,
) : ViewModel() {
    init {
        dankChatPreferenceStore.setCurrentInstalledVersionCode()
    }

    val state: ChangelogState? =
        DankChatVersion.LATEST_CHANGELOG?.let {
            ChangelogState(it.version.copy(patch = 0).formattedString(), it.string)
        }
}
