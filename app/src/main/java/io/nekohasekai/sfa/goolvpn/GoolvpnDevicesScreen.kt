package io.nekohasekai.sfa.goolvpn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoolvpnDevicesScreen(
    uiState: GoolvpnUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRevoke: (Int) -> Unit,
) {
    var revokeTarget by remember { mutableStateOf<GoolvpnDevice?>(null) }
    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goolvpn_devices)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(enabled = !uiState.devicesLoading, onClick = onRefresh) {
                        Text(stringResource(R.string.goolvpn_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.goolvpn_devices_limit, uiState.deviceUsed, uiState.deviceLimit),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.goolvpn_devices_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
            if (uiState.devicesLoading && uiState.devices.isEmpty()) {
                item { CircularProgressIndicator() }
            }
            uiState.devicesError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            items(uiState.devices, key = { it.id }) { device ->
                ListItem(
                    headlineContent = { Text(device.name) },
                    supportingContent = {
                        Column {
                            device.appVersion?.let { Text(stringResource(R.string.goolvpn_devices_version, it)) }
                            device.lastSeenAt?.let { Text(stringResource(R.string.goolvpn_devices_last_seen, it)) }
                            if (device.isCurrent) Text(stringResource(R.string.goolvpn_devices_current))
                        }
                    },
                    trailingContent = {
                        if (!device.isCurrent) {
                            TextButton(enabled = !uiState.devicesLoading, onClick = { revokeTarget = device }) {
                                Text(stringResource(R.string.goolvpn_devices_revoke))
                            }
                        }
                    },
                )
            }
        }
    }

    revokeTarget?.let { device ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text(stringResource(R.string.goolvpn_devices_revoke_title)) },
            text = { Text(stringResource(R.string.goolvpn_devices_revoke_text, device.name)) },
            confirmButton = {
                TextButton(onClick = { revokeTarget = null; onRevoke(device.id) }) {
                    Text(stringResource(R.string.goolvpn_devices_revoke))
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}
