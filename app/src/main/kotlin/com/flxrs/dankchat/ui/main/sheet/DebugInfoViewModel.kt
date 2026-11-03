package com.flxrs.dankchat.ui.main.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flxrs.dankchat.data.debug.DebugSectionRegistry
import com.flxrs.dankchat.data.debug.DebugSectionSnapshot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DebugInfoViewModel(
    debugSectionRegistry: DebugSectionRegistry,
) : ViewModel() {
    val sections: StateFlow<ImmutableList<DebugSectionSnapshot>> =
        debugSectionRegistry
            .allSections()
            .map { it.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())
}
