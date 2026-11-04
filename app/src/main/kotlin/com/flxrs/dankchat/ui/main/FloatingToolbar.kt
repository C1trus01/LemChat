package com.flxrs.dankchat.ui.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.ui.main.channel.ChannelTabUiState
import com.flxrs.dankchat.ui.main.stream.AudioOnlyBar
import com.flxrs.dankchat.utils.compose.PagerTabIndicator
import com.flxrs.dankchat.utils.compose.PagerTabIndicatorState
import com.flxrs.dankchat.utils.compose.rememberPagerTabIndicatorState
import com.flxrs.dankchat.utils.compose.reportPosition

internal const val RESTING_SCROLLBAR_ALPHA = 0.6f

private val TABS_ROW_HEIGHT = 44.dp
private val ACTIONS_ROW_HEIGHT = 40.dp
private val DIVIDER_HEIGHT = 1.dp

// Landscape is too short for a second row, there the icons sit at the end of the tabs
@Composable
@ReadOnlyComposable
private fun isTopBarSingleRow(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/** Height of the bar without the status bar gap, messages start right below it. */
@Composable
@ReadOnlyComposable
internal fun topBarHeight(): Dp = when {
    isTopBarSingleRow() -> TABS_ROW_HEIGHT + DIVIDER_HEIGHT
    else -> ACTIONS_ROW_HEIGHT + TABS_ROW_HEIGHT + DIVIDER_HEIGHT
}

/** The channel tabs get the whole width, with the mentions and the hub in a row of their own above them. */
@Composable
fun FloatingToolbar(
    tabState: ChannelTabUiState,
    composePagerState: PagerState,
    showAppBar: Boolean,
    isFullscreen: Boolean,
    currentStream: UserName?,
    isAudioOnly: Boolean,
    streamHeightDp: Dp,
    hasMentions: Boolean,
    onOpenMentions: (() -> Unit)?,
    onOpenChannels: () -> Unit,
    onOpenHub: () -> Unit,
    onAction: (ToolbarAction) -> Unit,
    onAudioOnly: () -> Unit,
    onStreamClose: () -> Unit,
    modifier: Modifier = Modifier,
    onToolbarBottomChange: (Int) -> Unit = {},
    streamToolbarAlpha: () -> Float = { 1f },
) {
    val density = LocalDensity.current
    var barHeightPx by remember { mutableIntStateOf(0) }

    val hasStream = currentStream != null && streamHeightDp > 0.dp
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val toolbarBottomPx = with(density) {
        when {
            isFullscreen -> 0

            !showAppBar -> statusBarTopPx

            // Under a stream the bar sits below the video instead of the status bar
            hasStream -> streamHeightDp.roundToPx() + barHeightPx

            else -> barHeightPx
        }
    }
    SideEffect(toolbarBottomPx) { onToolbarBottomChange(toolbarBottomPx) }
    val isSingleRow = isTopBarSingleRow()

    AnimatedVisibility(
        visible = showAppBar && !isFullscreen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = if (hasStream) streamHeightDp else 0.dp)
                .graphicsLayer { alpha = streamToolbarAlpha() },
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height },
        ) {
            Column(
                modifier = Modifier.padding(
                    top = when {
                        hasStream -> 0.dp
                        else -> with(density) { statusBarTopPx.toDp() }
                    },
                ),
            ) {
                if (!isSingleRow) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ACTIONS_ROW_HEIGHT),
                    ) {
                        Text(
                            text = tabState.tabs.getOrNull(composePagerState.currentPage)?.displayName.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 11.dp),
                        )
                        TopBarActions(
                            hasMentions = hasMentions,
                            onOpenMentions = onOpenMentions,
                            onOpenChannels = onOpenChannels,
                            onOpenHub = onOpenHub,
                        )
                    }
                }
                if (currentStream != null && isAudioOnly) {
                    AudioOnlyBar(
                        channel = currentStream,
                        onExpandVideo = onAudioOnly,
                        onClose = onStreamClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(TABS_ROW_HEIGHT),
                ) {
                    ChannelTabs(
                        tabState = tabState,
                        composePagerState = composePagerState,
                        onAction = onAction,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                    )
                    if (isSingleRow) {
                        TopBarActions(
                            hasMentions = hasMentions,
                            onOpenMentions = onOpenMentions,
                            onOpenChannels = onOpenChannels,
                            onOpenHub = onOpenHub,
                        )
                    }
                }
                HorizontalDivider(thickness = DIVIDER_HEIGHT, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TopBarActions(
    hasMentions: Boolean,
    onOpenMentions: (() -> Unit)?,
    onOpenChannels: () -> Unit,
    onOpenHub: () -> Unit,
) {
    IconButton(
        onClick = onOpenChannels,
        modifier = Modifier.size(ACTIONS_ROW_HEIGHT),
    ) {
        Icon(
            imageVector = Icons.Default.Tag,
            contentDescription = stringResource(R.string.hub_channels),
        )
    }
    if (onOpenMentions != null) {
        IconButton(
            onClick = onOpenMentions,
            modifier = Modifier.size(ACTIONS_ROW_HEIGHT),
        ) {
            Icon(
                imageVector = when {
                    hasMentions -> Icons.Default.Notifications
                    else -> Icons.Outlined.Notifications
                },
                contentDescription = stringResource(R.string.mentions_title),
                tint = when {
                    hasMentions -> MaterialTheme.colorScheme.error
                    else -> LocalContentColor.current
                },
            )
        }
    }
    IconButton(
        onClick = onOpenHub,
        modifier = Modifier.size(ACTIONS_ROW_HEIGHT),
    ) {
        Icon(
            imageVector = Icons.Outlined.GridView,
            contentDescription = stringResource(R.string.hub_open),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelTabs(
    tabState: ChannelTabUiState,
    composePagerState: PagerState,
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = composePagerState.currentPage
    val scrollState = rememberScrollState()
    val tabLayoutState = rememberPagerTabIndicatorState(tabState.tabs.size)
    var viewportWidth by remember { mutableIntStateOf(0) }

    // Center selected tab when selection changes
    LaunchedEffect(selectedIndex, tabLayoutState.ready, viewportWidth) {
        if (!tabLayoutState.ready || selectedIndex !in tabLayoutState.offsets.indices || viewportWidth <= 0) {
            return@LaunchedEffect
        }
        val tabOffset = tabLayoutState.offsets[selectedIndex]
        val tabWidth = tabLayoutState.widths[selectedIndex]
        val centeredOffset = tabOffset - (viewportWidth / 2 - tabWidth / 2)
        val clampedOffset = centeredOffset.coerceIn(0, scrollState.maxValue)
        if (scrollState.value != clampedOffset) {
            scrollState.animateScrollTo(clampedOffset)
        }
    }

    val hasLeftMention by rememberHasMentionBeside(tabState, tabLayoutState, scrollState) { offset, width, scroll ->
        offset + width < scroll
    }
    val hasRightMention by rememberHasMentionBeside(tabState, tabLayoutState, scrollState) { offset, _, scroll ->
        offset > scroll + viewportWidth
    }
    val mentionGradientColor = MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .onSizeChanged { viewportWidth = it.width }
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    val gradientWidth = 24.dp.toPx()
                    if (hasLeftMention) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(mentionGradientColor.copy(alpha = 0.5f), mentionGradientColor.copy(alpha = 0f)),
                                endX = gradientWidth,
                            ),
                            size = Size(gradientWidth, size.height),
                        )
                    }
                    if (hasRightMention) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(mentionGradientColor.copy(alpha = 0f), mentionGradientColor.copy(alpha = 0.5f)),
                                startX = size.width - gradientWidth,
                                endX = size.width,
                            ),
                            topLeft = Offset(size.width - gradientWidth, 0f),
                            size = Size(gradientWidth, size.height),
                        )
                    }
                }.horizontalScroll(scrollState),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight(),
        ) {
            tabState.tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val hasActivity = tab.mentionCount > 0 || tab.hasUnread
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { onAction(ToolbarAction.SelectTab(index)) },
                                onLongClick = { onAction(ToolbarAction.LongClickTab) },
                            ).padding(horizontal = 11.dp)
                            .reportPosition(tabLayoutState, index),
                ) {
                    Text(
                        text = tab.displayName,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            hasActivity -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = when {
                            isSelected -> FontWeight.Bold
                            hasActivity -> FontWeight.SemiBold
                            else -> FontWeight.Normal
                        },
                    )
                    if (tab.mentionCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Badge()
                    }
                }
            }
        }

        PagerTabIndicator(
            pagerState = composePagerState,
            state = tabLayoutState,
            color = MaterialTheme.colorScheme.primary,
            height = 2.dp,
            bottomInset = 0.dp,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

// A tab with mentions scrolled out of view tints that edge of the row
@Composable
private fun rememberHasMentionBeside(
    tabState: ChannelTabUiState,
    tabLayoutState: PagerTabIndicatorState,
    scrollState: ScrollState,
    isOutOfView: (offset: Int, width: Int, scroll: Int) -> Boolean,
) = remember(tabState.tabs) {
    derivedStateOf {
        val scroll = scrollState.value
        tabState.tabs.indices.any { i ->
            i < tabLayoutState.offsets.size &&
                tabState.tabs[i].mentionCount > 0 &&
                isOutOfView(tabLayoutState.offsets[i], tabLayoutState.widths[i], scroll)
        }
    }
}
