package com.flxrs.dankchat.utils.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/** [SheetState] for modal sheets that always expand fully, without a partially expanded stop. */
@ExperimentalMaterial3Api
@Composable
fun rememberModalSheetState(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)
