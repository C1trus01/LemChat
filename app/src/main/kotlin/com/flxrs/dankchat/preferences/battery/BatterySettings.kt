package com.flxrs.dankchat.preferences.battery

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BatterySettings(
    val partBusyChannels: Boolean = true,
    val busyThreshold: BusyThreshold = BusyThreshold.MessagesPerMinute100,
    val backgroundDelay: BatterySaverDelay = BatterySaverDelay.TenMinutes,
    val pauseEventConnections: Boolean = true,
    val pauseSevenTvLiveUpdates: Boolean = true,
    val sevenTvBehaviorMigrated: Boolean = false,
)

enum class BusyThreshold(
    val messagesPerMinute: Int,
) {
    MessagesPerMinute100(100),
    MessagesPerMinute200(200),
    MessagesPerMinute400(400),
}

enum class BatterySaverDelay(
    val duration: Duration,
) {
    FiveMinutes(5.minutes),
    TenMinutes(10.minutes),
    ThirtyMinutes(30.minutes),
}
