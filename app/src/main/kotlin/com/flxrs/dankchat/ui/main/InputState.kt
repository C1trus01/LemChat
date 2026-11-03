package com.flxrs.dankchat.ui.main

import androidx.compose.runtime.Stable

@Stable
sealed interface InputState {
    object Default : InputState

    object Replying : InputState

    object Announcing : InputState

    object Whispering : InputState

    object NotLoggedIn : InputState

    object Disconnected : InputState
}
