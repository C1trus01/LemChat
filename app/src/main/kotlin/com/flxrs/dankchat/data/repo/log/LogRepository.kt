package com.flxrs.dankchat.data.repo.log

import android.content.Context
import org.koin.core.annotation.Single
import java.io.File

@Single
class LogRepository(
    context: Context,
) {
    private val logDir = File(context.filesDir, "logs")

    fun getLogFiles(): List<File> {
        if (!logDir.exists()) return emptyList()
        return logDir
            .listFiles()
            ?.filter { it.isFile && it.name.endsWith(".log") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    fun readLogFile(fileName: String): List<String> {
        val file = File(logDir, fileName)
        if (!file.exists()) return emptyList()
        return file.readLines()
    }

    fun getLogFile(fileName: String): File? {
        val file = File(logDir, fileName)
        return file.takeIf { it.exists() }
    }

    fun getLatestLogFile(): File? = getLogFiles().firstOrNull()

    fun deleteAllLogs() {
        if (logDir.exists()) {
            logDir.listFiles()?.forEach { it.delete() }
        }
    }
}
