package com.flxrs.dankchat.ui.chat.emote

import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmoteInfoViewModel : ViewModel() {
    private val _items = MutableStateFlow<ImmutableList<EmoteInfoItem>>(persistentListOf())
    val items: StateFlow<ImmutableList<EmoteInfoItem>> = _items.asStateFlow()
    val isActive = _items.map { it.isNotEmpty() }

    fun show(emotes: List<EmoteSheetData>) {
        _items.value = emotes.map { it.toBasicEmoteInfoItem() }.toImmutableList()
    }

    fun dismiss() {
        _items.value = persistentListOf()
    }
}
