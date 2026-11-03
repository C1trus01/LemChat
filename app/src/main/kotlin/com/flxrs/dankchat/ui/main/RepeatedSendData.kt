package com.flxrs.dankchat.ui.main

import androidx.compose.runtime.Immutable

@Immutable
data class RepeatedSendData(
    val enabled: Boolean,
    val message: String,
)
