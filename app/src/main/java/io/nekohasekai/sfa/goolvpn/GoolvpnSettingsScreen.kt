package io.nekohasekai.sfa.goolvpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.constant.Status

private data class GoolvpnSettingItem(
    val icon: ImageVector,
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoolvpnSettingsScreen(
    uiState: GoolvpnUiState,
    serviceStatus: Status,
    themeMode: GoolvpnThemeMode,
    onThemeModeChange: (GoolvpnThemeMode) -> Unit,
    onSmartBypassChange: (Boolean) -> Unit,
    onSmartBypassGroupChange: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenAppRouting: () -> Unit,
    onOpenDevices: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenUpdates: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onSubmitDiagnostics: () -> Unit,
    onDismissDiagnostics: () -> Unit,
    onAdvanceOnboarding: () -> Unit,
    onDismissOnboarding: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val connected = serviceStatus != Status.Stopped
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showDiagnosticSendConfirmation by remember { mutableStateOf(false) }
    var showDiagnosticDetails by remember { mutableStateOf(false) }
    var smartBypassGroupsExpanded by rememberSaveable { mutableStateOf(false) }
    val onboardingStep = uiState.onboardingStep
    val onboardingTargetTitle = when (onboardingStep) {
        5 -> stringResource(R.string.goolvpn_excluded_apps)
        6 -> stringResource(R.string.goolvpn_diagnostics)
        else -> null
    }
    val items = listOf(
        GoolvpnSettingItem(
            Icons.Default.Info,
            stringResource(R.string.goolvpn_onboarding_reopen),
            stringResource(R.string.goolvpn_onboarding_reopen_description),
            onClick = onRestartOnboarding,
        ),
        GoolvpnSettingItem(
            Icons.Default.Apps,
            stringResource(R.string.goolvpn_excluded_apps),
            stringResource(R.string.goolvpn_excluded_apps_description),
            onClick = onOpenAppRouting,
        ),
        GoolvpnSettingItem(
            Icons.Default.MonitorHeart,
            stringResource(R.string.goolvpn_devices),
            stringResource(R.string.goolvpn_devices_settings_description),
            onClick = onOpenDevices,
        ),
        GoolvpnSettingItem(
            Icons.Default.Refresh,
            stringResource(R.string.goolvpn_refresh),
            stringResource(R.string.goolvpn_refresh_description),
            enabled = !uiState.loading,
            onClick = onRefresh,
        ),
        GoolvpnSettingItem(
            Icons.Default.AccountCircle,
            stringResource(R.string.goolvpn_account),
            onClick = onOpenAccount,
        ),
        GoolvpnSettingItem(
            Icons.Default.MonitorHeart,
            stringResource(R.string.goolvpn_diagnostics),
            stringResource(R.string.goolvpn_diagnostics_description),
            enabled = !uiState.diagnosticsLoading,
            onClick = onRunDiagnostics,
        ),
        GoolvpnSettingItem(
            Icons.Default.SupportAgent,
            stringResource(R.string.goolvpn_support),
            onClick = onOpenSupport,
        ),
        GoolvpnSettingItem(
            Icons.Default.Info,
            stringResource(R.string.goolvpn_feedback),
            stringResource(R.string.goolvpn_feedback_description),
            onClick = onOpenFeedback,
        ),
        GoolvpnSettingItem(
            Icons.Default.Notifications,
            stringResource(R.string.notification_settings),
            stringResource(R.string.service_notification_description),
            onClick = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                    )
                }
                context.startActivity(intent)
            },
        ),
        GoolvpnSettingItem(
            Icons.Default.SystemUpdate,
            stringResource(R.string.goolvpn_updates),
            stringResource(R.string.goolvpn_version, BuildConfig.VERSION_NAME),
            onClick = onOpenUpdates,
        ),
        GoolvpnSettingItem(
            Icons.Default.Logout,
            stringResource(R.string.goolvpn_deactivate),
            if (connected) stringResource(R.string.goolvpn_deactivate_connected) else null,
            enabled = !connected && !uiState.loading,
            onClick = { showDeactivateDialog = true },
        ),
    )
    val listState = rememberLazyListState()
    var onboardingTargetBounds by remember { mutableStateOf<Rect?>(null) }
    val onboardingTargetIndex = items.indexOfFirst { it.title == onboardingTargetTitle }
    val smartBypassListItemCount = if (uiState.smartBypassGroups.isEmpty()) {
        0
    } else {
        2 + if (smartBypassGroupsExpanded) uiState.smartBypassGroups.size else 0
    }

    LaunchedEffect(onboardingStep, onboardingTargetIndex, smartBypassListItemCount) {
        if (onboardingStep == 4 && uiState.smartBypassGroups.isNotEmpty()) {
            smartBypassGroupsExpanded = true
            listState.animateScrollToItem(1)
        }
        if (onboardingTargetIndex >= 0) {
            listState.animateScrollToItem(onboardingTargetIndex + 1 + smartBypassListItemCount)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goolvpn_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                },
            )
        },
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                item {
                    ThemeSelector(
                        selectedMode = themeMode,
                        onModeChange = onThemeModeChange,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (uiState.smartBypassGroups.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.goolvpn_smart_bypass)) },
                            supportingContent = {
                                Column {
                                    Text(
                                        if (uiState.smartBypassEnabled) {
                                            stringResource(R.string.goolvpn_smart_bypass_on, uiState.smartBypassEnabledGroupIds.size)
                                        } else {
                                            stringResource(R.string.goolvpn_smart_bypass_off)
                                        },
                                    )
                                    Text(
                                        stringResource(R.string.goolvpn_smart_bypass_recommended),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            leadingContent = { Icon(Icons.Default.Apps, contentDescription = null) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = uiState.smartBypassEnabled,
                                        onCheckedChange = onSmartBypassChange,
                                    )
                                    IconButton(onClick = {
                                        smartBypassGroupsExpanded = !smartBypassGroupsExpanded
                                    }) {
                                        Icon(
                                            imageVector = if (smartBypassGroupsExpanded) {
                                                Icons.Default.ExpandLess
                                            } else {
                                                Icons.Default.ExpandMore
                                            },
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .onGloballyPositioned {
                                    if (onboardingStep == 4) onboardingTargetBounds = it.boundsInWindow()
                                }
                                .clickable {
                                    smartBypassGroupsExpanded = !smartBypassGroupsExpanded
                                },
                        )
                        HorizontalDivider()
                    }
                    if (smartBypassGroupsExpanded) {
                        items(uiState.smartBypassGroups) { group ->
                            ListItem(
                                headlineContent = { Text(group.title) },
                                supportingContent = { Text(group.examples) },
                                modifier = Modifier.padding(start = 16.dp),
                                trailingContent = {
                                    Switch(
                                        checked = group.id in uiState.smartBypassEnabledGroupIds,
                                        onCheckedChange = { onSmartBypassGroupChange(group.id, it) },
                                    )
                                },
                            )
                        }
                    }
                    item { HorizontalDivider() }
                }
                items(items) { item ->
                    val isOnboardingTarget = item.title == onboardingTargetTitle
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = item.description?.let { description ->
                            { Text(description) }
                        },
                        leadingContent = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = if (item.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isOnboardingTarget) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier
                            .padding(vertical = if (isOnboardingTarget) 4.dp else 0.dp)
                            .onGloballyPositioned {
                                if (isOnboardingTarget) onboardingTargetBounds = it.boundsInWindow()
                            }
                            .clickable(enabled = item.enabled, onClick = item.onClick),
                    )
                    HorizontalDivider()
                }
            }
        }

        if (onboardingStep in 4..6) {
            GoolvpnOnboardingOverlay(
                targetBounds = onboardingTargetBounds,
                title = stringResource(
                    when (onboardingStep) {
                        4 -> R.string.goolvpn_onboarding_smart_bypass_title
                        5 -> R.string.goolvpn_onboarding_apps_title
                        else -> R.string.goolvpn_onboarding_diagnostics_title
                    },
                ),
                message = stringResource(
                    when (onboardingStep) {
                        4 -> R.string.goolvpn_onboarding_smart_bypass_text
                        5 -> R.string.goolvpn_onboarding_apps_text
                        else -> R.string.goolvpn_onboarding_diagnostics_text
                    },
                ),
                step = onboardingStep,
                position = if (onboardingStep in 4..5) {
                    GoolvpnCoachmarkPosition.Bottom
                } else {
                    GoolvpnCoachmarkPosition.Top
                },
                onNext = onAdvanceOnboarding,
                onDismiss = onDismissOnboarding,
            )
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text(stringResource(R.string.goolvpn_deactivate_confirm_title)) },
            text = { Text(stringResource(R.string.goolvpn_deactivate_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeactivateDialog = false
                        onDeactivate()
                    },
                ) {
                    Text(stringResource(R.string.goolvpn_deactivate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (uiState.diagnosticsLoading || uiState.diagnosticsReport != null) {
        val report = uiState.diagnosticsReport
        val snapshot = uiState.diagnosticsSnapshot
        AlertDialog(
            onDismissRequest = {
                if (!uiState.diagnosticsLoading) onDismissDiagnostics()
            },
            title = { Text(stringResource(R.string.goolvpn_diagnostics_title)) },
            text = {
                if (uiState.diagnosticsLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                        Text(stringResource(R.string.goolvpn_diagnostics_running))
                    }
                } else {
                    Column(modifier = Modifier.widthIn(max = 440.dp)) {
                        Text(
                            text = stringResource(
                                if (snapshot?.isHealthy == true) {
                                    R.string.goolvpn_diagnostics_healthy
                                } else {
                                    R.string.goolvpn_diagnostics_attention
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.goolvpn_diagnostics_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        snapshot?.let {
                            DiagnosticCheck(
                                stringResource(R.string.goolvpn_diagnostics_network),
                                it.networkValidated,
                            )
                            DiagnosticCheck(
                                stringResource(R.string.goolvpn_diagnostics_server),
                                it.backendReachable,
                            )
                            DiagnosticCheck(
                                stringResource(R.string.goolvpn_diagnostics_access),
                                it.activated && it.accessActive && it.profileReady,
                            )
                            Text(
                                text = stringResource(
                                    R.string.goolvpn_diagnostics_route_strategy,
                                    stringResource(it.routeStrategyStringRes()),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        Text(
                            text = stringResource(
                                if (showDiagnosticDetails) {
                                    R.string.goolvpn_diagnostics_hide_details
                                } else {
                                    R.string.goolvpn_diagnostics_show_details
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 8.dp)
                                .clickable { showDiagnosticDetails = !showDiagnosticDetails },
                        )
                        if (showDiagnosticDetails) {
                            Text(
                                text = report.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        uiState.diagnosticsTicketId?.let { ticketId ->
                            Text(
                                text = stringResource(R.string.goolvpn_diagnostics_sent, ticketId),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        uiState.diagnosticsSendError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (report != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            enabled = !uiState.diagnosticsSending && uiState.diagnosticsTicketId == null,
                            onClick = { showDiagnosticSendConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.diagnosticsSending) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                            }
                            Text(stringResource(R.string.goolvpn_diagnostics_send_support))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("GOOLVPN diagnostics", report))
                                    Toast.makeText(context, R.string.goolvpn_diagnostics_copied, Toast.LENGTH_SHORT).show()
                                },
                            ) { Text(stringResource(R.string.goolvpn_diagnostics_copy)) }
                        }
                    }
                }
            },
        )
    }

    if (showDiagnosticSendConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiagnosticSendConfirmation = false },
            title = { Text(stringResource(R.string.goolvpn_diagnostics_send_confirm_title)) },
            text = { Text(stringResource(R.string.goolvpn_diagnostics_send_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiagnosticSendConfirmation = false
                        onSubmitDiagnostics()
                    },
                ) {
                    Text(stringResource(R.string.goolvpn_diagnostics_send_support))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticSendConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DiagnosticCheck(label: String, passed: Boolean) {
    val status = stringResource(
        if (passed) R.string.goolvpn_diagnostics_status_ok
        else R.string.goolvpn_diagnostics_status_problem,
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (passed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (passed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun GoolvpnDiagnosticSnapshot.routeStrategyStringRes(): Int = when (routeStrategy) {
    "automatic_urltest" -> R.string.goolvpn_diagnostics_route_auto
    "hysteria_first_urltest" -> R.string.goolvpn_diagnostics_route_fast
    "compatible_urltest" -> R.string.goolvpn_diagnostics_route_stable
    else -> R.string.goolvpn_diagnostics_route_unknown
}

@Composable
private fun ThemeSelector(
    selectedMode: GoolvpnThemeMode,
    onModeChange: (GoolvpnThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.goolvpn_theme),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.goolvpn_theme_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            val modes = GoolvpnThemeMode.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(
                            stringResource(
                                when (mode) {
                                    GoolvpnThemeMode.System -> R.string.goolvpn_theme_system
                                    GoolvpnThemeMode.Light -> R.string.goolvpn_theme_light
                                    GoolvpnThemeMode.Dark -> R.string.goolvpn_theme_dark
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}
