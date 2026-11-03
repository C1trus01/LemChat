package com.flxrs.dankchat.data.debug

import android.os.Debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import java.util.Locale

@Single
class AppDebugSection : DebugSection {
    override val order = 11
    override val baseTitle = "App"

    override fun entries(): Flow<DebugSectionSnapshot> {
        val ticker =
            flow {
                while (true) {
                    emit(Unit)
                    delay(2_000)
                }
            }
        return ticker.map {
            val runtime = Runtime.getRuntime()
            val heapUsed = runtime.totalMemory() - runtime.freeMemory()
            val heapMax = runtime.maxMemory()
            val nativeAllocated = Debug.getNativeHeapAllocatedSize()
            val nativeTotal = Debug.getNativeHeapSize()

            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val graphicsKb = memInfo.getMemoryStat("summary.graphics")?.toLongOrNull() ?: 0L
            val codeKb = memInfo.getMemoryStat("summary.code")?.toLongOrNull() ?: 0L
            val stackKb = memInfo.getMemoryStat("summary.stack")?.toLongOrNull() ?: 0L
            val privateOtherKb = memInfo.getMemoryStat("summary.private-other")?.toLongOrNull() ?: 0L
            val totalPssKb = memInfo.getMemoryStat("summary.total-pss")?.toLongOrNull() ?: 0L

            val graphicsBytes = graphicsKb * 1024L
            val totalAppMemory = heapUsed + nativeAllocated + graphicsBytes

            DebugSectionSnapshot(
                title = baseTitle,
                entries =
                    buildList {
                        add(DebugEntry("Total app memory", formatBytes(totalAppMemory)))
                        add(DebugEntry("JVM heap", "${formatBytes(heapUsed)} / ${formatBytes(heapMax)}"))
                        add(DebugEntry("Native heap", "${formatBytes(nativeAllocated)} / ${formatBytes(nativeTotal)}"))
                        add(DebugEntry("Graphics", formatBytes(graphicsBytes)))
                        add(DebugEntry("Code", formatKb(codeKb)))
                        add(DebugEntry("Stack", formatKb(stackKb)))
                        add(DebugEntry("Other", formatKb(privateOtherKb)))
                        add(DebugEntry("Total PSS", formatKb(totalPssKb)))
                        add(DebugEntry("Threads", "${Thread.activeCount()}"))
                    },
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return "%.1f MB".format(Locale.ROOT, mb)
    }

    private fun formatKb(kb: Long): String {
        val mb = kb / 1024.0
        return "%.1f MB".format(Locale.ROOT, mb)
    }
}
