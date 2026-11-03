package com.flxrs.dankchat.preferences.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.repo.update.AppUpdateState
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceCategoryTitle
import com.flxrs.dankchat.preferences.components.PreferenceCategoryWithSummary
import com.flxrs.dankchat.preferences.components.PreferenceItem
import com.flxrs.dankchat.preferences.components.PreferenceListDialog
import com.flxrs.dankchat.preferences.components.PreferenceSummary
import com.flxrs.dankchat.preferences.language.AppLanguage
import com.flxrs.dankchat.ui.theme.DankChatTheme
import com.flxrs.dankchat.ui.update.AppUpdateViewModel
import com.flxrs.dankchat.ui.update.appUpdateSummary
import com.flxrs.dankchat.utils.compose.buildClickableAnnotation
import com.flxrs.dankchat.utils.compose.buildLinkAnnotation
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

private const val LEMCHAT_URL = "https://github.com/C1trus01/LemChat"
private const val DANKCHAT_URL = "https://github.com/flex3r/dankchat"
private const val TWITCH_TOS_URL = "https://www.twitch.tv/p/terms-of-service"

sealed interface SettingsNavigation {
    data object Appearance : SettingsNavigation

    data object Notifications : SettingsNavigation

    data object Chat : SettingsNavigation

    data object Streams : SettingsNavigation

    data object Battery : SettingsNavigation

    data object Tools : SettingsNavigation

    data object Developer : SettingsNavigation

    data object DankChatAccount : SettingsNavigation

    data object Changelog : SettingsNavigation

    data object About : SettingsNavigation
}

@Composable
fun OverviewSettingsScreen(
    isLoggedIn: Boolean,
    hasChangelog: Boolean,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (SettingsNavigation) -> Unit,
) {
    val viewModel = koinViewModel<OverviewSettingsViewModel>()
    val showDankChatAccount by viewModel.showDankChatAccount.collectAsStateWithLifecycle()
    val updateViewModel = koinViewModel<AppUpdateViewModel>()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

    OverviewSettingsScreen(
        isLoggedIn = isLoggedIn,
        hasChangelog = hasChangelog,
        showDankChatAccount = showDankChatAccount,
        updateState = updateState,
        onUpdateClick = updateViewModel::advance,
        onBack = onBack,
        onLogout = onLogout,
        onNavigate = onNavigate,
    )
}

@Composable
private fun OverviewSettingsScreen(
    isLoggedIn: Boolean,
    hasChangelog: Boolean,
    showDankChatAccount: Boolean,
    updateState: AppUpdateState,
    onUpdateClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (SettingsNavigation) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            PreferenceItem(
                title = stringResource(R.string.preference_appearance_header),
                icon = Icons.Default.Palette,
                onClick = { onNavigate(SettingsNavigation.Appearance) },
            )
            LanguagePreference()
            PreferenceItem(
                title = stringResource(R.string.preference_highlights_ignores_header),
                icon = Icons.Default.NotificationsActive,
                onClick = { onNavigate(SettingsNavigation.Notifications) },
            )
            PreferenceItem(stringResource(R.string.preference_chat_header), Icons.Default.Forum, onClick = {
                onNavigate(SettingsNavigation.Chat)
            })
            PreferenceItem(stringResource(R.string.preference_streams_header), Icons.Default.PlayArrow, onClick = {
                onNavigate(SettingsNavigation.Streams)
            })
            PreferenceItem(stringResource(R.string.preference_battery_header), Icons.Default.BatterySaver, onClick = {
                onNavigate(SettingsNavigation.Battery)
            })
            PreferenceItem(stringResource(R.string.preference_tools_header), Icons.Default.Construction, onClick = {
                onNavigate(SettingsNavigation.Tools)
            })
            PreferenceItem(stringResource(R.string.preference_developer_header), Icons.Default.DeveloperMode, onClick = {
                onNavigate(SettingsNavigation.Developer)
            })

            AnimatedVisibility(hasChangelog) {
                PreferenceItem(stringResource(R.string.preference_whats_new_header), Icons.Default.FiberNew, onClick = {
                    onNavigate(SettingsNavigation.Changelog)
                })
            }

            AnimatedVisibility(showDankChatAccount) {
                PreferenceItem(stringResource(R.string.preference_dankchat_account_title), Icons.Default.ManageAccounts, onClick = {
                    onNavigate(SettingsNavigation.DankChatAccount)
                })
            }

            PreferenceItem(
                title = stringResource(R.string.app_update_title),
                icon = Icons.Default.SystemUpdate,
                summary = appUpdateSummary(updateState),
                onClick = onUpdateClick,
            )
            PreferenceItem(stringResource(R.string.logout), Icons.AutoMirrored.Default.ExitToApp, isEnabled = isLoggedIn, onClick = onLogout)
            SecretDankerModeTrigger {
                PreferenceCategoryWithSummary(
                    title = {
                        PreferenceCategoryTitle(
                            text = stringResource(R.string.preference_about_header),
                            modifier = Modifier.dankClickable(),
                        )
                    },
                ) {
                    val aboutSummary = stringResource(R.string.preference_about_summary, BuildConfig.VERSION_NAME)
                    val aboutBasedOn = stringResource(R.string.preference_about_based_on)
                    val aboutTos = stringResource(R.string.preference_about_tos)
                    val annotated =
                        buildAnnotatedString {
                            append(aboutSummary)
                            appendLine()
                            withLink(link = buildLinkAnnotation(LEMCHAT_URL)) {
                                append(LEMCHAT_URL)
                            }
                            appendLine()
                            appendLine()
                            append(aboutBasedOn)
                            appendLine()
                            withLink(link = buildLinkAnnotation(DANKCHAT_URL)) {
                                append(DANKCHAT_URL)
                            }
                            appendLine()
                            appendLine()
                            append(aboutTos)
                            appendLine()
                            withLink(link = buildLinkAnnotation(TWITCH_TOS_URL)) {
                                append(TWITCH_TOS_URL)
                            }
                            appendLine()
                            appendLine()
                            val licenseText = stringResource(R.string.open_source_licenses)
                            withLink(link = buildClickableAnnotation(text = licenseText, onClick = { onNavigate(SettingsNavigation.About) })) {
                                append(licenseText)
                            }
                        }
                    PreferenceSummary(annotated, Modifier.padding(top = 16.dp))
                }
            }
            NavigationBarSpacer()
        }
    }
}

@Composable
private fun LanguagePreference() {
    val context = LocalContext.current
    val systemName = stringResource(R.string.preference_language_system)
    var selected by remember { mutableStateOf(AppLanguage.current(context)) }
    val values = remember { (listOf(AppLanguage.SYSTEM) + AppLanguage.AVAILABLE).toImmutableList() }
    val entries =
        remember(systemName) {
            values.map { tag -> if (tag == AppLanguage.SYSTEM) systemName else AppLanguage.displayName(tag) }.toImmutableList()
        }
    PreferenceListDialog(
        title = stringResource(R.string.preference_language_title),
        values = values,
        entries = entries,
        selected = selected,
        summary = entries[values.indexOf(selected)],
        icon = Icons.Default.Language,
        onChange = {
            selected = it
            AppLanguage.set(context, it)
        },
    )
}

@Suppress("UnusedPrivateFunction")
@Composable
@PreviewDynamicColors
@PreviewLightDark
private fun OverviewSettingsPreview() {
    DankChatTheme {
        OverviewSettingsScreen(
            isLoggedIn = false,
            hasChangelog = true,
            showDankChatAccount = false,
            updateState = AppUpdateState.Idle,
            onUpdateClick = { },
            onBack = { },
            onLogout = { },
            onNavigate = { },
        )
    }
}
