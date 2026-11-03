package com.flxrs.dankchat.data.state

import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.chat.ChatLoadingFailure
import com.flxrs.dankchat.data.repo.data.DataLoadingFailure

sealed interface ChannelLoadingState {
    data object Idle : ChannelLoadingState

    data object Loading : ChannelLoadingState

    data object Loaded : ChannelLoadingState

    data class Failed(
        val failures: List<ChannelLoadingFailure>,
    ) : ChannelLoadingState
}

sealed interface ChannelLoadingFailure {
    val channel: UserName
    val error: Throwable

    data class Badges(
        override val channel: UserName,
        val channelId: UserId,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class BTTVEmotes(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class FFZEmotes(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class SevenTVEmotes(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class Cheermotes(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class FollowerEmotes(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure

    data class RecentMessages(
        override val channel: UserName,
        override val error: Throwable,
    ) : ChannelLoadingFailure
}

sealed interface GlobalLoadingState {
    data object Idle : GlobalLoadingState

    data object Loading : GlobalLoadingState

    data object Loaded : GlobalLoadingState

    data class Failed(
        val failures: Set<DataLoadingFailure> = emptySet(),
        val chatFailures: Set<ChatLoadingFailure> = emptySet(),
    ) : GlobalLoadingState
}
