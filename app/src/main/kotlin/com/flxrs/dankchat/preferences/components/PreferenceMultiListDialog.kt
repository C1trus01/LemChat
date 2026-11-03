package com.flxrs.dankchat.preferences.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flxrs.dankchat.utils.compose.rememberModalSheetState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun <T> PreferenceMultiListDialog(
    title: String,
    values: ImmutableList<T>,
    initialSelected: ImmutableList<T>,
    entries: ImmutableList<String>,
    onChange: (List<T>) -> Unit,
    isEnabled: Boolean = true,
    summary: String? = null,
    icon: ImageVector? = null,
    descriptions: ImmutableList<String>? = null,
) {
    var selected by remember(initialSelected) { mutableStateOf(values.map(initialSelected::contains).toPersistentList()) }
    ExpandablePreferenceItem(
        title = title,
        summary = summary,
        isEnabled = isEnabled,
        icon = icon,
    ) {
        val sheetState = rememberModalSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                dismiss()
                onChange(values.filterIndexed { idx, _ -> selected[idx] })
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            entries.forEachIndexed { idx, entry ->
                val interactionSource = remember { MutableInteractionSource() }
                val itemSelected = selected[idx]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = itemSelected,
                                onClick = { selected = selected.replacingAt(idx, !itemSelected) },
                                interactionSource = interactionSource,
                                indication = ripple(),
                            ).padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = itemSelected,
                        onCheckedChange = { selected = selected.replacingAt(idx, it) },
                        interactionSource = interactionSource,
                    )
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 18.sp,
                        )
                        val description = descriptions?.getOrNull(idx)
                        if (description != null) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
