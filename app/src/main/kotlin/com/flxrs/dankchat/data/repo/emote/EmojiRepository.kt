package com.flxrs.dankchat.data.repo.emote

import android.content.Context
import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.R
import com.flxrs.dankchat.di.DispatchersProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Immutable
@Serializable
data class EmojiData(
    val code: String,
    val unicode: String,
)

@Single
class EmojiRepository(
    private val context: Context,
    private val json: Json,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.default)
    private val _emojis = MutableStateFlow<List<EmojiData>>(emptyList())
    val emojis: StateFlow<List<EmojiData>> = _emojis.asStateFlow()

    init {
        scope.launch {
            runCatching {
                val input = context.resources.openRawResource(R.raw.emoji_data)
                val text = input.bufferedReader().use { it.readText() }
                _emojis.value = json.decodeFromString<List<EmojiData>>(text)
            }
        }
    }
}
