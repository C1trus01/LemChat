package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flxrs.dankchat.R
import com.flxrs.dankchat.utils.compose.ConfirmationBottomSheet
import com.flxrs.dankchat.utils.compose.rememberModalSheetState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AccountsSheet(
    accounts: ImmutableList<AccountItem>,
    onSwitchAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onRelogin: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
) {
    var accountToRemove by remember { mutableStateOf<AccountItem?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.accounts),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            accounts.forEach { account ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchAccount(account.userId)
                                onDismiss()
                            }.padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Icon(
                        imageVector =
                            when {
                                account.isActive -> Icons.Default.Check
                                else -> Icons.Default.Person
                            },
                        contentDescription = null,
                        tint =
                            when {
                                account.isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { accountToRemove = account }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.account_remove_description, account.name),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddAccount)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.account_add),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (accounts.any { it.isActive }) {
                HorizontalDivider()
                AccountAction(
                    text = stringResource(R.string.relogin),
                    icon = Icons.Default.Refresh,
                    onClick = onRelogin,
                )
                AccountAction(
                    text = stringResource(R.string.logout),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = onLogout,
                )
            }
        }
    }

    accountToRemove?.let { account ->
        ConfirmationBottomSheet(
            title = stringResource(R.string.account_remove_message, account.name),
            confirmText = stringResource(R.string.account_remove_button),
            confirmColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onConfirm = {
                onRemoveAccount(account.userId)
                accountToRemove = null
            },
            onDismiss = { accountToRemove = null },
        )
    }
}

@Composable
private fun AccountAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
