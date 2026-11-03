package com.flxrs.dankchat.ui.chat.user

import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.twitch.badge.Badge

data class UserPopupStateParams(
    val targetUserId: UserId?,
    val targetUserName: UserName,
    val targetDisplayName: DisplayName,
    val channel: UserName?,
    val badges: List<Badge> = emptyList(),
)
