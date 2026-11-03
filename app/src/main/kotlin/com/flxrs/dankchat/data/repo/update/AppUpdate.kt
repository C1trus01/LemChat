package com.flxrs.dankchat.data.repo.update

import com.flxrs.dankchat.data.api.github.dto.GitHubReleaseDto

data class AppUpdate(
    val version: String,
    val notes: String?,
    val apkUrl: String,
    val apkSize: Long,
)

sealed interface AppUpdateState {
    data object Idle : AppUpdateState

    data object Checking : AppUpdateState

    data object UpToDate : AppUpdateState

    data object CheckFailed : AppUpdateState

    data class Available(
        val update: AppUpdate,
    ) : AppUpdateState

    data class Downloading(
        val update: AppUpdate,
        val progress: Float?,
    ) : AppUpdateState

    data class ReadyToInstall(
        val update: AppUpdate,
    ) : AppUpdateState

    data class DownloadFailed(
        val update: AppUpdate,
    ) : AppUpdateState

    // The file is not this app, not newer or signed with another key, so Android would refuse it
    data class NotInstallable(
        val update: AppUpdate,
    ) : AppUpdateState
}

/** The update this release offers, null when it is not newer or has nothing to install. */
internal fun GitHubReleaseDto.toAppUpdate(currentVersion: String): AppUpdate? {
    val version = tagName.removePrefix("v")
    if (!isNewerVersion(version, currentVersion)) {
        return null
    }
    val apk = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.downloadUrl.startsWith("https://") } ?: return null
    return AppUpdate(version = version, notes = body?.trim()?.ifEmpty { null }, apkUrl = apk.downloadUrl, apkSize = apk.size)
}

/** Compares versions like 4.0.47, one that can't be read is never treated as newer. */
internal fun isNewerVersion(
    candidate: String,
    current: String,
): Boolean {
    val candidateParts = candidate.toVersionParts() ?: return false
    val currentParts = current.toVersionParts() ?: return false
    for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
        val difference = candidateParts.getOrElse(index) { 0 } - currentParts.getOrElse(index) { 0 }
        if (difference != 0) {
            return difference > 0
        }
    }
    return false
}

private fun String.toVersionParts(): List<Int>? {
    val parts = removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() }
    return parts.filterNotNull().takeIf { it.size == parts.size }
}
