package com.flxrs.dankchat.preferences.battery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.R
import com.flxrs.dankchat.preferences.components.NavigationBarSpacer
import com.flxrs.dankchat.preferences.components.PreferenceListDialog
import com.flxrs.dankchat.preferences.components.SwitchPreferenceItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BatterySettingsScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<BatterySettingsViewModel>()
    val state = viewModel.state.collectAsStateWithLifecycle().value

    BatterySettingsContent(
        state = state,
        onInteraction = { viewModel.onInteraction(it) },
        onBack = onBack,
    )
}

@Composable
private fun BatterySettingsContent(
    state: BatterySettingsState,
    onInteraction: (BatterySettingsInteraction) -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.preference_battery_header)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) },
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
                    .verticalScroll(rememberScrollState()),
        ) {
            SwitchPreferenceItem(
                title = stringResource(R.string.preference_battery_part_busy_title),
                summary = stringResource(R.string.preference_battery_part_busy_summary),
                isChecked = settings.partBusyChannels,
                onClick = { onInteraction(BatterySettingsInteraction.PartBusyChannels(it)) },
            )

            val thresholdEntries =
                persistentListOf(
                    stringResource(R.string.battery_busy_threshold_entry_100),
                    stringResource(R.string.battery_busy_threshold_entry_200),
                    stringResource(R.string.battery_busy_threshold_entry_400),
                )
            PreferenceListDialog(
                isEnabled = settings.partBusyChannels,
                title = stringResource(R.string.preference_battery_busy_threshold_title),
                summary = stringResource(R.string.preference_battery_busy_threshold_summary, thresholdEntries[settings.busyThreshold.ordinal]),
                values = BusyThreshold.entries.toImmutableList(),
                entries = thresholdEntries,
                selected = settings.busyThreshold,
                onChange = { onInteraction(BatterySettingsInteraction.Threshold(it)) },
            )

            SwitchPreferenceItem(
                title = stringResource(R.string.preference_battery_pause_events_title),
                summary = stringResource(R.string.preference_battery_pause_events_summary),
                isChecked = settings.pauseEventConnections,
                onClick = { onInteraction(BatterySettingsInteraction.PauseEventConnections(it)) },
            )
            SwitchPreferenceItem(
                title = stringResource(R.string.preference_battery_pause_7tv_title),
                summary = stringResource(R.string.preference_battery_pause_7tv_summary),
                isChecked = settings.pauseSevenTvLiveUpdates,
                isEnabled = state.sevenTvLiveUpdatesConfigurable,
                onClick = { onInteraction(BatterySettingsInteraction.PauseSevenTvLiveUpdates(it)) },
            )

            val delayEntries =
                persistentListOf(
                    stringResource(R.string.battery_delay_entry_five_minutes),
                    stringResource(R.string.battery_delay_entry_ten_minutes),
                    stringResource(R.string.battery_delay_entry_thirty_minutes),
                )
            PreferenceListDialog(
                isEnabled = settings.partBusyChannels || settings.pauseEventConnections || settings.pauseSevenTvLiveUpdates,
                title = stringResource(R.string.preference_battery_delay_title),
                summary = stringResource(R.string.preference_battery_delay_summary, delayEntries[settings.backgroundDelay.ordinal]),
                values = BatterySaverDelay.entries.toImmutableList(),
                entries = delayEntries,
                selected = settings.backgroundDelay,
                onChange = { onInteraction(BatterySettingsInteraction.Delay(it)) },
            )
            NavigationBarSpacer()
        }
    }
}
