package com.flxrs.dankchat.data.repo.crash

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.utils.datastore.createDataStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

private val logger = KotlinLogging.logger("CrashHandler")

@Single(createdAtStart = true)
class CrashHandler(
    context: Context,
) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Suppress("InjectDispatcher")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val dataStore: DataStore<CrashData> = createDataStore(
        fileName = DATA_STORE_FILE,
        context = context,
        defaultValue = CrashData(),
        serializer = CrashData.serializer(),
        scope = scope,
    )

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
        scope.launch {
            try {
                importProcessExits(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to import process exits" }
            }
        }
    }

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        // Always log to file via logback
        logger.error(throwable) { "Uncaught exception on ${thread.name}" }

        // Reports stay on the device until they are shared by hand
        try {
            persistCrash(thread, throwable)
        } catch (_: Throwable) {
            // Best effort — must never throw from the crash handler
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    // Freezes, native crashes and kills by the system never reach the exception handler, the system reports them on the next start
    private suspend fun importProcessExits(context: Context) {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
        val lastImported = dataStore.data.first().lastProcessExitTimestamp
        val exits = activityManager
            .getHistoricalProcessExitReasons(null, 0, MAX_PROCESS_EXITS)
            .filter { it.timestamp > lastImported }
        if (exits.isEmpty()) {
            return
        }

        val entries = exits.filter { it.isReported }.sortedByDescending { it.timestamp }.map { it.toCrashEntry() }
        dataStore.updateData { current ->
            current.copy(
                crashes = (entries + current.crashes).sortedByDescending { it.id }.take(MAX_CRASHES),
                lastProcessExitTimestamp = exits.maxOf { it.timestamp },
            )
        }
    }

    private val ApplicationExitInfo.isReported: Boolean
        get() = when (reason) {
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
            -> true

            // Being killed in the background is normal, only a visible app disappearing is a problem
            ApplicationExitInfo.REASON_LOW_MEMORY,
            ApplicationExitInfo.REASON_SIGNALED,
            -> importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE

            else -> false
        }

    private fun ApplicationExitInfo.toCrashEntry(): CrashEntry {
        val header = listOfNotNull(reasonName(reason), description?.takeIf { it.isNotBlank() }).joinToString(": ")
        val details = buildString {
            appendLine("Importance: $importance, status: $status")
            appendLine("Memory: pss=${pss}kB rss=${rss}kB")
            appendLine()
            append(readTrace())
        }
        return CrashEntry(
            id = timestamp,
            fingerprint = "exit-$timestamp",
            timestamp = Instant.ofEpochMilli(timestamp).toString(),
            version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            androidInfo = "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            threadName = processName,
            exceptionHeader = header,
            stackTrace = details,
        )
    }

    private fun ApplicationExitInfo.readTrace(): String = try {
        val trace = traceInputStream
        when {
            trace == null -> ""
            reason == ApplicationExitInfo.REASON_ANR -> mainThreadFirst(trace.use { it.readBytes() }.decodeToString())
            reason == ApplicationExitInfo.REASON_CRASH_NATIVE -> nativeTraceSummary(trace.use { it.readBytes() })
            else -> ""
        }
    } catch (e: IOException) {
        "Failed to read the trace: ${e.message}"
    }

    private fun persistCrash(
        thread: Thread,
        throwable: Throwable,
    ) {
        val timestamp = System.currentTimeMillis()
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

        var rootCause: Throwable = throwable
        while (rootCause.cause != null) {
            rootCause = rootCause.cause ?: break
        }
        val exceptionHeader = "${rootCause.javaClass.name}: ${rootCause.message.orEmpty()}"

        val fingerprint = computeFingerprint(throwable)
        val entry = CrashEntry(
            id = timestamp,
            fingerprint = fingerprint,
            timestamp = Instant.ofEpochMilli(timestamp).toString(),
            version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            androidInfo = "${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            threadName = thread.name,
            exceptionHeader = exceptionHeader,
            stackTrace = stackTrace,
        )

        runBlocking {
            dataStore.updateData { current ->
                val isNew = fingerprint !in current.knownFingerprints
                current.copy(
                    crashes = (listOf(entry) + current.crashes).take(MAX_CRASHES),
                    hasUnshownCrash = isNew,
                    knownFingerprints = current.knownFingerprints + fingerprint,
                )
            }
        }
    }

    private fun computeFingerprint(throwable: Throwable): String {
        val frames = throwable.stackTrace.take(FINGERPRINT_FRAME_COUNT).joinToString("|") { it.toString() }
        val key = "${throwable.javaClass.name}:${throwable.message}|$frames"
        return key.hashCode().toString()
    }

    companion object {
        private const val DATA_STORE_FILE = "crash_data"
        private const val FINGERPRINT_FRAME_COUNT = 5
        private const val MAX_CRASHES = 10
        private const val MAX_PROCESS_EXITS = 16
    }
}

private fun reasonName(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_ANR -> "App froze (ANR)"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Killed for excessive resource usage"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Initialization failure"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "Killed by low memory"
    ApplicationExitInfo.REASON_SIGNALED -> "Killed by a signal"
    else -> "Exit reason $reason"
}

/** Freeze traces list every thread, the main thread is what froze and the size of a report is limited. */
internal fun mainThreadFirst(trace: String): String {
    val mainThread = trace.indexOf(MAIN_THREAD_MARKER)
    val start =
        when {
            mainThread >= 0 -> mainThread
            else -> 0
        }
    return trace.substring(start).take(MAX_TRACE_LENGTH)
}

/**
 * Native traces are binary protobuf tombstones that also hold memory dumps of the process.
 * Only the names of signals, libraries and functions are kept, the memory could contain chat data or tokens.
 */
internal fun nativeTraceSummary(tombstone: ByteArray): String = NATIVE_TRACE_STRING
    .findAll(tombstone.toString(Charsets.ISO_8859_1))
    .map { it.value }
    .filter { text -> NATIVE_TRACE_KEEP.any { text.contains(it) } || text.startsWith("_Z") }
    .joinToString(separator = "\n")
    .take(MAX_TRACE_LENGTH)

private const val MAIN_THREAD_MARKER = "\"main\""
private const val MAX_TRACE_LENGTH = 30_000
private val NATIVE_TRACE_STRING = Regex("[\\x21-\\x7E]{5,}")
private val NATIVE_TRACE_KEEP = listOf(".so", ".apk", ".dex", ".odex", ".oat", "SIG")
