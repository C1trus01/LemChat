package com.flxrs.dankchat.ui.crash

import android.content.ClipData
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.ConfirmationBottomSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CrashViewerSheet(onDismiss: () -> Unit) {
    val viewModel: CrashViewerViewModel = koinViewModel()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val entry = viewModel.crashEntry
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val sheetBackgroundColor =
        lerp(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            fraction = 0.75f,
        )

    var backProgress by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val toolbarTopPadding = statusBarHeight + 8.dp + 48.dp + 16.dp

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            onDismiss()
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(sheetBackgroundColor)
                .graphicsLayer {
                    val scale = 1f - (backProgress * 0.1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - backProgress
                    translationY = backProgress * 100f
                },
    ) {
        SelectionContainer {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = toolbarTopPadding)
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                if (entry != null) {
                    Text(
                        text = "Version: ${entry.version}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Android: ${entry.androidInfo} | ${entry.device}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Thread: ${entry.threadName} | ${entry.timestamp}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text(
                        text = entry.stackTrace,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Floating toolbar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                0f to sheetBackgroundColor.copy(alpha = 0.7f),
                                0.75f to sheetBackgroundColor.copy(alpha = 0.7f),
                                1f to sheetBackgroundColor.copy(alpha = 0f),
                            ),
                    ).padding(top = statusBarHeight + 8.dp)
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = stringResource(R.string.crash_viewer_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    IconButton(
                        onClick = {
                            val fullReport = viewModel.buildEmailBody() ?: return@IconButton
                            scope.launch {
                                clipboardManager.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("crash_report", fullReport)),
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.crash_report_copy),
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.crash_viewer_delete),
                        )
                    }
                }
            }
        }

        // Status bar fill when toolbar scrolled
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(statusBarHeight)
                    .background(sheetBackgroundColor.copy(alpha = 0.7f)),
        )
    }

    if (showDeleteConfirmation && entry != null) {
        ConfirmationBottomSheet(
            title = stringResource(R.string.crash_viewer_delete_confirm),
            confirmText = stringResource(R.string.crash_viewer_delete),
            confirmColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onConfirm = {
                viewModel.deleteCrash()
                showDeleteConfirmation = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}
