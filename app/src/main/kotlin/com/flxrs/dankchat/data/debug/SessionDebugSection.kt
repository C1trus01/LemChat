package com.flxrs.dankchat.data.debug

import android.os.Process
import android.os.SystemClock
import com.flxrs.dankchat.data.repo.chat.ChatChannelProvider
import com.flxrs.dankchat.data.repo.chat.ChatMessageRepository
import com.flxrs.dankchat.preferences.developer.DeveloperSettingsDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

@Single
class SessionDebugSection(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatChannelProvider: ChatChannelProvider,
    private val developerSettingsDataStore: DeveloperSettingsDataStore,
) : DebugSection {
    override val order = 1
    override val baseTitle = "Session"

    override fun entries(): Flow<DebugSectionSnapshot> {
        val ticker =
            flow {
                while (true) {
                    emit(Unit)
                    delay(1_000)
                }
            }
        return combine(ticker, chatChannelProvider.channels) { _, channels ->
            // Measured from process start, so lazy creation of this section doesn't shift the timer
            val elapsed = (SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()).milliseconds
            val hours = elapsed.inWholeHours
            val minutes = elapsed.inWholeMinutes % 60
            val seconds = elapsed.inWholeSeconds % 60
            val uptime =
                buildString {
                    if (hours > 0) append("${hours}h ")
                    if (minutes > 0 || hours > 0) append("${minutes}m ")
                    append("${seconds}s")
                }

            DebugSectionSnapshot(
                title = baseTitle,
                entries =
                    listOf(
                        DebugEntry("Uptime", uptime),
                        DebugEntry("Send protocol", developerSettingsDataStore.current().chatSendProtocol.name),
                        DebugEntry("Total messages received", "${chatMessageRepository.sessionMessageCount}"),
                        DebugEntry("Messages sent (IRC)", "${chatMessageRepository.ircSentCount}"),
                        DebugEntry("Messages sent (Helix)", "${chatMessageRepository.helixSentCount}"),
                        DebugEntry("Send failures", "${chatMessageRepository.sendFailureCount}"),
                        DebugEntry("Active channels", "${channels?.size ?: 0}"),
                    ),
            )
        }
    }
}
