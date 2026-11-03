package com.flxrs.dankchat.preferences.developer

import kotlinx.serialization.Serializable

@Serializable
enum class ChatSendProtocol {
    IRC,
    Helix,
}
