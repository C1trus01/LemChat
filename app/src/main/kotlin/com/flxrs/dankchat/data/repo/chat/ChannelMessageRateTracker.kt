package com.flxrs.dankchat.data.repo.chat

import com.flxrs.dankchat.data.UserName
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class ChannelMessageRateTracker {
    private val windows = ConcurrentHashMap<UserName, Window>()

    fun onMessage(channel: UserName) {
        windows.getOrPut(channel) { Window() }.increment(currentMinute())
    }

    fun ratePerMinute(channel: UserName): Int = windows[channel]?.ratePerMinute(currentMinute()) ?: 0

    fun removeChannel(channel: UserName) {
        windows.remove(channel)
    }

    private fun currentMinute(): Long = System.currentTimeMillis() / 60_000

    internal class Window {
        private val buckets = IntArray(WINDOW_MINUTES)
        private var latestMinute = 0L

        @Synchronized
        fun increment(minute: Long) {
            advanceTo(minute)
            buckets[(minute % WINDOW_MINUTES).toInt()]++
        }

        @Synchronized
        fun ratePerMinute(minute: Long): Int {
            advanceTo(minute)
            return buckets.sum() / WINDOW_MINUTES
        }

        private fun advanceTo(minute: Long) {
            val delta = minute - latestMinute
            when {
                delta <= 0L -> return

                delta >= WINDOW_MINUTES -> buckets.fill(0)

                else -> {
                    for (stale in (latestMinute + 1)..minute) {
                        buckets[(stale % WINDOW_MINUTES).toInt()] = 0
                    }
                }
            }
            latestMinute = minute
        }
    }

    companion object {
        internal const val WINDOW_MINUTES = 5
    }
}
