package com.flxrs.dankchat.data.repo.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.data.api.github.GitHubApiClient
import com.flxrs.dankchat.di.DispatchersProvider
import com.flxrs.dankchat.di.UPDATE_OKHTTP_CLIENT
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger("AppUpdateRepository")

/** Offers the newest GitHub release of the fork as an update. Android itself still asks before installing. */
@Single
class AppUpdateRepository(
    private val application: Application,
    private val gitHubApiClient: GitHubApiClient,
    @Named(UPDATE_OKHTTP_CLIENT) private val httpClient: OkHttpClient,
    dispatchersProvider: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchersProvider.io)
    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val updatesDir = File(application.cacheDir, UPDATES_DIR)

    private var checkJob: Job? = null
    private var lastCheck: TimeMark? = null

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private val _dismissedVersion = MutableStateFlow(preferences.getString(KEY_DISMISSED_VERSION, null))
    val dismissedVersion: StateFlow<String?> = _dismissedVersion.asStateFlow()

    init {
        // What an earlier run downloaded is either installed by now or out of date
        scope.launch { updatesDir.deleteRecursively() }
    }

    fun checkIfDue() {
        val last = lastCheck
        if (last == null || last.elapsedNow() >= CHECK_INTERVAL) {
            check(isManual = false)
        }
    }

    fun check(isManual: Boolean = true) {
        val previous = _state.value
        if (checkJob?.isActive == true || previous is AppUpdateState.Downloading || previous is AppUpdateState.ReadyToInstall) {
            return
        }
        lastCheck = TimeSource.Monotonic.markNow()
        if (isManual) {
            _state.value = AppUpdateState.Checking
        }
        checkJob =
            scope.launch {
                _state.value =
                    gitHubApiClient.getLatestRelease(UPDATE_REPOSITORY).fold(
                        onSuccess = { release ->
                            when (val update = release?.toAppUpdate(BuildConfig.VERSION_NAME)) {
                                null -> AppUpdateState.UpToDate
                                else -> AppUpdateState.Available(update)
                            }
                        },
                        onFailure = {
                            logger.debug(it) { "Failed to check for updates" }
                            if (isManual) AppUpdateState.CheckFailed else previous
                        },
                    )
            }
    }

    fun download() {
        val update =
            when (val state = _state.value) {
                is AppUpdateState.Available -> state.update
                is AppUpdateState.DownloadFailed -> state.update
                else -> return
            }
        _state.value = AppUpdateState.Downloading(update, progress = null)
        scope.launch {
            val apk = runCatching { downloadApk(update) }.getOrElse {
                logger.debug(it) { "Failed to download update" }
                _state.value = AppUpdateState.DownloadFailed(update)
                return@launch
            }
            if (!isInstallable(apk)) {
                apk.delete()
                _state.value = AppUpdateState.NotInstallable(update)
                return@launch
            }
            _state.value = AppUpdateState.ReadyToInstall(update)
            install()
        }
    }

    /** Hands the file to the system installer. It stays available when that is cancelled, or blocked while the app is in the background. */
    fun install() {
        val apk = File(updatesDir, APK_FILE)
        if (!apk.exists()) {
            return
        }
        val uri = FileProvider.getUriForFile(application, "${application.packageName}.fileprovider", apk)
        // Viewing the file would also offer any other app that can open archives, like office suites and app stores
        @Suppress("DEPRECATION")
        val intent =
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setData(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { application.startActivity(intent) }
            .onFailure { logger.debug(it) { "Failed to open the installer" } }
    }

    /** Asked once per version, afterwards the update waits in the settings. */
    fun dismissPrompt(version: String) {
        preferences.edit { putString(KEY_DISMISSED_VERSION, version) }
        _dismissedVersion.value = version
    }

    // The name in the release is not used for the path, it comes from outside of the app
    private fun downloadApk(update: AppUpdate): File {
        updatesDir.mkdirs()
        val partial = File(updatesDir, "$APK_FILE.part")
        val request = Request.Builder().url(update.apkUrl).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response ${response.code}")
            }
            val total = response.body.contentLength().takeIf { it > 0 } ?: update.apkSize.takeIf { it > 0 }
            response.body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        written += read
                        val percent = total?.let { (written * 100 / it).toInt() } ?: continue
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.value = AppUpdateState.Downloading(update, percent.coerceAtMost(100) / 100f)
                        }
                    }
                    if (update.apkSize > 0 && written != update.apkSize) {
                        throw IOException("Expected ${update.apkSize} bytes, got $written")
                    }
                }
            }
        }
        val apk = File(updatesDir, APK_FILE)
        if (!partial.renameTo(apk)) {
            throw IOException("Could not move the downloaded file")
        }
        return apk
    }

    private fun isInstallable(apk: File): Boolean {
        val packageManager = application.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: return false
        val installed = packageManager.getPackageInfo(application.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        return archive.packageName == application.packageName &&
            archive.longVersionCode > installed.longVersionCode &&
            signersOf(archive).any { it in signersOf(installed) }
    }

    private fun signersOf(info: PackageInfo): List<Signature> {
        val signingInfo = info.signingInfo ?: return emptyList()
        val signers =
            when {
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        return signers?.toList().orEmpty()
    }

    private companion object {
        const val UPDATE_REPOSITORY = "C1trus01/LemChat"
        const val PREFS_NAME = "app_update"
        const val KEY_DISMISSED_VERSION = "dismissed_version"
        const val UPDATES_DIR = "updates"
        const val APK_FILE = "update.apk"
        val CHECK_INTERVAL = 6.hours
    }
}
