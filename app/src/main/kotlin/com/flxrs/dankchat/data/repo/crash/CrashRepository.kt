package com.flxrs.dankchat.data.repo.crash

import com.flxrs.dankchat.utils.datastore.safeData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class CrashRepository(
    crashHandler: CrashHandler,
) {
    private val dataStore = crashHandler.dataStore

    private fun currentData(): CrashData = runBlocking { dataStore.safeData(CrashData()).first() }

    fun hasUnshownCrash(): Boolean = currentData().hasUnshownCrash

    fun markCrashShown() {
        update { it.copy(hasUnshownCrash = false) }
    }

    fun getRecentCrashes(): List<CrashEntry> = currentData().crashes

    fun getMostRecentCrash(): CrashEntry? = getRecentCrashes().firstOrNull()

    fun getCrash(id: Long): CrashEntry? = getRecentCrashes().firstOrNull { it.id == id }

    fun deleteCrash(id: Long) {
        update { current ->
            val removed = current.crashes.firstOrNull { it.id == id }
            val updated = current.crashes.filter { it.id != id }
            when {
                updated.isEmpty() -> CrashData(lastProcessExitTimestamp = current.lastProcessExitTimestamp)

                else -> current.copy(
                    crashes = updated,
                    knownFingerprints = when (removed) {
                        null -> current.knownFingerprints
                        else -> current.knownFingerprints - removed.fingerprint
                    },
                )
            }
        }
    }

    fun deleteAllCrashes() {
        // The system keeps its exit history, deleted exits must not come back from it
        update { CrashData(lastProcessExitTimestamp = it.lastProcessExitTimestamp) }
    }

    private fun update(transform: suspend (CrashData) -> CrashData) {
        runBlocking {
            runCatching { dataStore.updateData(transform) }
        }
    }

    fun buildEmailBody(
        entry: CrashEntry,
        userName: String?,
        userId: String?,
    ): String = buildString {
        appendLine("LemChat Crash Report")
        appendLine("=====================")
        appendLine("Version: ${entry.version}")
        appendLine("Android: ${entry.androidInfo}")
        appendLine("Device: ${entry.device}")
        appendLine("Thread: ${entry.threadName}")
        appendLine("Time: ${entry.timestamp}")
        if (userName != null) {
            appendLine("User: $userName (ID: ${userId.orEmpty()})")
        }
        appendLine()
        appendLine("Stack Trace:")
        appendLine(entry.stackTrace)
    }
}
