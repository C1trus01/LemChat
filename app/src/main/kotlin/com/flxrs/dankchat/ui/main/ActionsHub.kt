package com.flxrs.dankchat.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.appearance.HubAction
import com.flxrs.dankchat.utils.compose.rememberModalSheetState

enum class HubPage { Main, Channel, Upload }

@Immutable
data class HubTile(
    val action: HubAction,
    @get:StringRes val labelRes: Int,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val isActive: Boolean = false,
)

@Immutable
data class HubUiState(
    val hasChannel: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isStreamActive: Boolean = false,
    val isAudioOnly: Boolean = false,
    val hasStreamData: Boolean = false,
    val isFullscreen: Boolean = false,
    val isTheaterMode: Boolean = false,
    val isModerator: Boolean = false,
    val debugMode: Boolean = false,
    val inputEnabled: Boolean = false,
    val hasLastMessage: Boolean = false,
    val hasPinnedMessage: Boolean = false,
    val isPinnedMessageShown: Boolean = false,
) {
    /** The tile of an action, or null while the action does not exist in the current state. */
    fun tileFor(action: HubAction): HubTile? = when (action) {
        HubAction.Stream -> HubTile(
            action = action,
            labelRes = if (isStreamActive) R.string.hub_stream_hide else R.string.hub_stream,
            icon = if (isStreamActive) Icons.Outlined.VideocamOff else Icons.Outlined.Videocam,
            enabled = hasStreamData || isStreamActive,
            isActive = isStreamActive,
        )

        HubAction.AudioOnly -> when {
            isStreamActive -> HubTile(
                action = action,
                labelRes = if (isAudioOnly) R.string.hub_audio_exit else R.string.hub_audio,
                icon = if (isAudioOnly) Icons.Outlined.Videocam else Icons.Default.Headphones,
                isActive = isAudioOnly,
            )

            else -> null
        }

        HubAction.Search -> HubTile(action, R.string.hub_search, Icons.Default.Search, enabled = hasChannel)

        HubAction.ModActions -> when {
            isModerator -> HubTile(action, R.string.hub_mod_actions, Icons.Outlined.Shield, enabled = hasChannel)
            else -> null
        }

        HubAction.ReloadEmotes -> HubTile(action, R.string.reload_emotes, Icons.Default.EmojiEmotions, enabled = hasChannel)

        HubAction.PinnedMessage -> when {
            hasPinnedMessage -> HubTile(
                action = action,
                labelRes = if (isPinnedMessageShown) R.string.pinned_message_collapse else R.string.pinned_message_show,
                icon = Icons.Default.PushPin,
                isActive = isPinnedMessageShown,
            )

            else -> null
        }

        // Stays available without a channel, adding the first one starts here too
        HubAction.Channel -> HubTile(action, R.string.hub_channels, Icons.Default.Tag)

        HubAction.Upload -> HubTile(action, R.string.upload_media, Icons.Default.CloudUpload)

        HubAction.LastMessage -> HubTile(action, R.string.input_action_last_message, Icons.Default.History, enabled = inputEnabled && hasLastMessage)

        HubAction.Fullscreen -> when {
            isTheaterMode -> null

            else -> HubTile(
                action = action,
                labelRes = if (isFullscreen) R.string.hub_fullscreen_exit else R.string.menu_fullscreen,
                icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                isActive = isFullscreen,
            )
        }

        HubAction.HideInput -> HubTile(action, R.string.menu_hide_input, Icons.Default.VisibilityOff)

        HubAction.Reconnect -> HubTile(action, R.string.hub_reconnect, Icons.Default.Autorenew)

        HubAction.Settings -> HubTile(action, R.string.settings, Icons.Default.Settings)

        HubAction.Debug -> when {
            debugMode -> HubTile(action, R.string.input_action_debug, Icons.Default.BugReport)
            else -> null
        }
    }
}

private val CHANNEL_ACTIONS = listOf(
    HubAction.Stream,
    HubAction.AudioOnly,
    HubAction.Search,
    HubAction.PinnedMessage,
    HubAction.ModActions,
    HubAction.ReloadEmotes,
    HubAction.Channel,
)
private val OWN_ACTIONS = listOf(HubAction.Upload, HubAction.LastMessage)
private val APP_ACTIONS = listOf(
    HubAction.Fullscreen,
    HubAction.HideInput,
    HubAction.Reconnect,
    HubAction.Settings,
    HubAction.Debug,
)

private val MIN_TILE_WIDTH = 76.dp
private val TILE_GAP = 6.dp

/** Every action that is not needed all the time, new features belong here instead of on the chat screen. */
@Composable
fun ActionsHubSheet(
    page: HubPage,
    state: HubUiState,
    accountName: String?,
    channelInfo: String?,
    pinnedAction: HubAction,
    channelNotificationsEnabled: Boolean,
    onPageChange: (HubPage) -> Unit,
    onHubAction: (HubAction) -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
    onPinAction: (HubAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            when (page) {
                HubPage.Main -> HubMainPage(
                    state = state,
                    accountName = accountName,
                    channelInfo = channelInfo,
                    pinnedAction = pinnedAction,
                    onHubAction = onHubAction,
                    onToolbarAction = onToolbarAction,
                    onPinAction = onPinAction,
                )

                HubPage.Channel -> HubChannelPage(
                    hasChannel = state.hasChannel,
                    isLoggedIn = state.isLoggedIn,
                    notificationsEnabled = channelNotificationsEnabled,
                    onBack = { onPageChange(HubPage.Main) },
                    onToolbarAction = onToolbarAction,
                )

                HubPage.Upload -> HubUploadPage(
                    onBack = { onPageChange(HubPage.Main) },
                    onToolbarAction = onToolbarAction,
                )
            }
        }
    }
}

@Composable
private fun HubMainPage(
    state: HubUiState,
    accountName: String?,
    channelInfo: String?,
    pinnedAction: HubAction,
    onHubAction: (HubAction) -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
    onPinAction: (HubAction) -> Unit,
) {
    var isPinMode by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    onToolbarAction(if (state.isLoggedIn) ToolbarAction.ManageAccounts else ToolbarAction.Login)
                }.heightIn(min = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                    .padding(6.dp),
            )
            Column {
                Text(
                    text = accountName ?: stringResource(R.string.login),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.isLoggedIn) {
                    Text(
                        text = stringResource(R.string.accounts),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { isPinMode = !isPinMode },
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = stringResource(R.string.hub_pin),
                color = when {
                    isPinMode -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }

    when {
        isPinMode -> Text(
            text = stringResource(R.string.hub_pin_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp),
        )

        !channelInfo.isNullOrEmpty() -> Text(
            text = channelInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = (maxWidth / (MIN_TILE_WIDTH + TILE_GAP)).toInt().coerceAtLeast(4)
        Column {
            listOf(
                R.string.channel to CHANNEL_ACTIONS,
                R.string.hub_section_own to OWN_ACTIONS,
                R.string.hub_section_app to APP_ACTIONS,
            ).forEach { (titleRes, actions) ->
                HubSection(
                    title = stringResource(titleRes),
                    tiles = actions.mapNotNull(state::tileFor),
                    columns = columns,
                    isPinMode = isPinMode,
                    pinnedAction = pinnedAction,
                    onTileClick = { action ->
                        when {
                            isPinMode -> onPinAction(action)
                            else -> onHubAction(action)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HubSection(
    title: String,
    tiles: List<HubTile>,
    columns: Int,
    isPinMode: Boolean,
    pinnedAction: HubAction,
    onTileClick: (HubAction) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                row.forEach { tile ->
                    HubTileButton(
                        tile = tile,
                        // Anything can be pinned, a tile that is unavailable right now just stays disabled next to the input
                        enabled = tile.enabled || isPinMode,
                        isPinned = tile.action == pinnedAction,
                        onClick = { onTileClick(tile.action) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HubTileButton(
    tile: HubTile,
    enabled: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val contentColor = when {
        tile.isActive || isPinned -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = modifier
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, if (isPinned) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            text = stringResource(tile.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HubChannelPage(
    hasChannel: Boolean,
    isLoggedIn: Boolean,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
) {
    HubPageHeader(title = stringResource(R.string.hub_channels), onBack = onBack)
    HubRow(
        text = stringResource(R.string.add_channel),
        icon = Icons.Default.Add,
        onClick = { onToolbarAction(ToolbarAction.AddChannel) },
    )
    HubRow(
        text = stringResource(R.string.manage_channels),
        icon = Icons.Default.EditNote,
        onClick = { onToolbarAction(ToolbarAction.ManageChannels) },
    )
    if (!hasChannel) {
        return
    }
    HubRow(
        text = stringResource(R.string.remove_channel),
        icon = Icons.Default.RemoveCircleOutline,
        onClick = { onToolbarAction(ToolbarAction.RemoveChannel) },
    )
    HubRow(
        text = stringResource(R.string.channel_background_notifications),
        icon = Icons.Default.Notifications,
        onClick = { onToolbarAction(ToolbarAction.ToggleChannelNotifications) },
        checked = notificationsEnabled,
    )
    HubRow(
        text = stringResource(R.string.open_channel),
        icon = Icons.Default.OpenInBrowser,
        onClick = { onToolbarAction(ToolbarAction.OpenChannel) },
    )
    HubRow(
        text = stringResource(R.string.report_channel),
        icon = Icons.Default.Flag,
        onClick = { onToolbarAction(ToolbarAction.ReportChannel) },
    )
    if (isLoggedIn) {
        HubRow(
            text = stringResource(R.string.block_channel),
            icon = Icons.Default.Block,
            onClick = { onToolbarAction(ToolbarAction.BlockChannel) },
        )
    }
}

@Composable
private fun HubUploadPage(
    onBack: () -> Unit,
    onToolbarAction: (ToolbarAction) -> Unit,
) {
    HubPageHeader(title = stringResource(R.string.upload_media), onBack = onBack)
    HubRow(
        text = stringResource(R.string.take_picture),
        icon = Icons.Default.CameraAlt,
        onClick = { onToolbarAction(ToolbarAction.CaptureImage) },
    )
    HubRow(
        text = stringResource(R.string.record_video),
        icon = Icons.Default.Videocam,
        onClick = { onToolbarAction(ToolbarAction.CaptureVideo) },
    )
    HubRow(
        text = stringResource(R.string.choose_media),
        icon = Icons.Default.Image,
        onClick = { onToolbarAction(ToolbarAction.ChooseMedia) },
    )
}

@Composable
private fun HubPageHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.hub_back),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun HubRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    checked: Boolean? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (checked != null) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}
