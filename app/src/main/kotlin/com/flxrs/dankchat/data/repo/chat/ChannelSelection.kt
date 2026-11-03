package com.flxrs.dankchat.data.repo.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChannelSelection(
    val activeChannel: String? = null,
)
