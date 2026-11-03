package com.flxrs.dankchat.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.repo.update.AppUpdateState
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.hours

/** Looks for updates while the app is open and asks once per version whether to install one. */
@Composable
fun AppUpdatePrompt(viewModel: AppUpdateViewModel = koinViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.checkIfDue()
                delay(1.hours)
            }
        }
    }

    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val state = prompt ?: return
    val update = state.update ?: return
    val onLater = { viewModel.dismissPrompt(update.version) }

    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.app_update_dialog_title, update.version)) },
        text = {
            Column {
                when (state) {
                    is AppUpdateState.Downloading -> {
                        Text(stringResource(R.string.app_update_summary_downloading, update.version))
                        Spacer(Modifier.height(12.dp))
                        when (val progress = state.progress) {
                            null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            else -> LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    is AppUpdateState.DownloadFailed -> {
                        Text(stringResource(R.string.app_update_summary_download_failed_short))
                    }

                    is AppUpdateState.NotInstallable -> {
                        Text(stringResource(R.string.app_update_dialog_not_installable))
                    }

                    else -> {
                        update.notes?.let { notes ->
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier =
                                    Modifier
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(stringResource(R.string.app_update_dialog_later_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is AppUpdateState.Available, is AppUpdateState.ReadyToInstall -> {
                    TextButton(onClick = viewModel::advance) { Text(stringResource(R.string.app_update_dialog_install)) }
                }

                is AppUpdateState.DownloadFailed -> {
                    TextButton(onClick = viewModel::advance) { Text(stringResource(R.string.app_update_dialog_retry)) }
                }

                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.app_update_dialog_later)) }
        },
    )
}

@Composable
fun appUpdateSummary(state: AppUpdateState): String {
    val current = BuildConfig.VERSION_NAME
    return when (state) {
        AppUpdateState.Idle -> stringResource(R.string.app_update_summary_idle, current)
        AppUpdateState.Checking -> stringResource(R.string.app_update_summary_checking)
        AppUpdateState.UpToDate -> stringResource(R.string.app_update_summary_up_to_date, current)
        AppUpdateState.CheckFailed -> stringResource(R.string.app_update_summary_check_failed)
        is AppUpdateState.Available -> stringResource(R.string.app_update_summary_available, state.update.version)
        is AppUpdateState.Downloading -> stringResource(R.string.app_update_summary_downloading, state.update.version)
        is AppUpdateState.ReadyToInstall -> stringResource(R.string.app_update_summary_ready, state.update.version)
        is AppUpdateState.DownloadFailed -> stringResource(R.string.app_update_summary_download_failed, state.update.version)
        is AppUpdateState.NotInstallable -> stringResource(R.string.app_update_summary_not_installable, state.update.version)
    }
}
