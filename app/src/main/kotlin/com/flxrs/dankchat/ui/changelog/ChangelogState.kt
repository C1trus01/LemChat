package com.flxrs.dankchat.ui.changelog

import androidx.compose.runtime.Immutable

@Immutable
data class ChangelogState(
    val version: String,
    val changelog: String,
)
