package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.ConfirmationBottomSheet

@Composable
fun ConfirmationDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColors: ButtonColors? = null,
    dismissText: String = stringResource(R.string.dialog_cancel),
) {
    ConfirmationBottomSheet(
        title = title,
        confirmText = confirmText,
        confirmColors = confirmColors,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
