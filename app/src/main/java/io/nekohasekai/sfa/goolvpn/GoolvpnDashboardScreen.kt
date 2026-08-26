package io.nekohasekai.sfa.goolvpn

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.theme.GoolvpnConnected
import io.nekohasekai.sfa.compose.theme.GoolvpnInactive
import io.nekohasekai.sfa.compose.theme.GoolvpnReady
import io.nekohasekai.sfa.constant.Status
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun GoolvpnDashboardScreen(
    uiState: GoolvpnUiState,
    serviceStatus: Status,
    onActivate: (String) -> Unit,
    onToggleConnection: () -> Unit,
    onGetCode: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onConnectionModeChange: (GoolvpnConnectionMode) -> Unit,
    onSmartBypassChange: (Boolean) -> Unit,
    onContinueOnboarding: () -> Unit,
    onDismissOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.activated) {
        ActivationContent(
            loading = uiState.loading,
            error = uiState.error,
            onActivate = onActivate,
            onGetCode = onGetCode,
            modifier = modifier,
        )
        return
    }

    val connected = serviceStatus == Status.Started
    val transitioning = serviceStatus == Status.Starting || serviceStatus == Status.Stopping
    val connectionReady = uiState.active && uiState.profileReady
    val statusText = when {
        !connectionReady -> stringResource(R.string.goolvpn_status_inactive)
        serviceStatus == Status.Started -> stringResource(R.string.goolvpn_status_protected)
        serviceStatus == Status.Starting -> stringResource(R.string.goolvpn_status_connecting)
        serviceStatus == Status.Stopping -> stringResource(R.string.goolvpn_status_disconnecting)
        else -> stringResource(R.string.goolvpn_status_disconnected)
    }

    val validityText = if (uiState.isLifetime) {
        stringResource(R.string.goolvpn_lifetime)
    } else {
        stringResource(R.string.goolvpn_valid_until, formatExpiry(uiState.expiresAt))
    }

    var modeBounds by remember { mutableStateOf<Map<GoolvpnConnectionMode, Rect>>(emptyMap()) }
    val onboardingMode = when (uiState.onboardingStep) {
        1 -> GoolvpnConnectionMode.Automatic
        2 -> GoolvpnConnectionMode.Fast
        3 -> GoolvpnConnectionMode.Stable
        else -> null
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusPanel(
                    planTitle = uiState.planTitle,
                    statusText = statusText,
                    validityText = validityText,
                    connected = connected,
                    connectionReady = connectionReady,
                    transitioning = transitioning || uiState.loading,
                    onToggleConnection = if (connectionReady) onToggleConnection else onOpenAccount,
                    onOpenSettings = onOpenSettings,
                )
            }

            if (uiState.error != null || uiState.message != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.error != null) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = uiState.error ?: uiState.message.orEmpty(),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    ConnectionModePicker(
                        selectedMode = uiState.connectionMode,
                        onModeChange = onConnectionModeChange,
                        onModeBoundsChange = { mode, bounds ->
                            modeBounds = modeBounds + (mode to bounds)
                        },
                    )
                }
            }

            if (uiState.smartBypassGroups.isNotEmpty()) {
                item {
                    SmartBypassCard(
                        enabled = uiState.smartBypassEnabled,
                        enabledGroupCount = uiState.smartBypassEnabledGroupIds.size,
                        onEnabledChange = onSmartBypassChange,
                    )
                }
            }
        }

        onboardingMode?.let { mode ->
            val title = stringResource(
                when (mode) {
                    GoolvpnConnectionMode.Automatic -> R.string.goolvpn_mode_automatic
                    GoolvpnConnectionMode.Fast -> R.string.goolvpn_mode_fast
                    GoolvpnConnectionMode.Stable -> R.string.goolvpn_mode_stable
                },
            )
            val description = stringResource(
                when (mode) {
                    GoolvpnConnectionMode.Automatic -> R.string.goolvpn_onboarding_auto_text
                    GoolvpnConnectionMode.Fast -> R.string.goolvpn_onboarding_fast_text
                    GoolvpnConnectionMode.Stable -> R.string.goolvpn_onboarding_stable_text
                },
            )
            GoolvpnOnboardingOverlay(
                targetBounds = modeBounds[mode],
                title = title,
                message = description,
                step = uiState.onboardingStep,
                position = GoolvpnCoachmarkPosition.Bottom,
                onNext = onContinueOnboarding,
                onDismiss = onDismissOnboarding,
            )
        }
    }
}

@Composable
private fun SmartBypassCard(
    enabled: Boolean,
    enabledGroupCount: Int,
    onEnabledChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.goolvpn_smart_bypass),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.goolvpn_smart_bypass_recommended),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (enabled) {
                        stringResource(R.string.goolvpn_smart_bypass_on, enabledGroupCount)
                    } else {
                        stringResource(R.string.goolvpn_smart_bypass_off)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun StatusPanel(
    planTitle: String,
    statusText: String,
    validityText: String,
    connected: Boolean,
    connectionReady: Boolean,
    transitioning: Boolean,
    onToggleConnection: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val accentColor = when {
        connected -> GoolvpnConnected
        connectionReady -> GoolvpnReady
        else -> GoolvpnInactive
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layoutSpec = dashboardLayoutSpec(
            availableWidthDp = maxWidth.value.roundToInt(),
            fontScale = LocalDensity.current.fontScale,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = accentColor,
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = layoutSpec.panelHorizontalPaddingDp.dp,
                    vertical = 16.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FittingSingleLineText(
                            text = stringResource(R.string.goolvpn_home_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            minFontSizeSp = 16f,
                            maxFontSizeSp = 24f,
                            stepSizeSp = 1f,
                        )
                        Text(
                            text = planTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(30.dp),
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.goolvpn_settings),
                            tint = Color.White,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onToggleConnection,
                    enabled = !transitioning,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.72f),
                        disabledContentColor = accentColor.copy(alpha = 0.7f),
                    ),
                    contentPadding = PaddingValues(layoutSpec.connectButtonContentPaddingDp.dp),
                    modifier = Modifier.size(layoutSpec.connectButtonDiameterDp.dp),
                ) {
                    if (transitioning) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp),
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(46.dp),
                            )
                            Spacer(Modifier.height(5.dp))
                            FittingSingleLineText(
                                text = if (connected) {
                                    stringResource(R.string.goolvpn_disconnect)
                                } else {
                                    stringResource(R.string.goolvpn_connect)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Clip,
                                minFontSizeSp = 10f,
                                maxFontSizeSp = 16f,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                FittingSingleLineText(
                    text = statusText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    minFontSizeSp = 16f,
                    maxFontSizeSp = 24f,
                    stepSizeSp = 1f,
                )
                Text(
                    text = validityText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

}

@Composable
private fun ConnectionModePicker(
    selectedMode: GoolvpnConnectionMode,
    onModeChange: (GoolvpnConnectionMode) -> Unit,
    onModeBoundsChange: (GoolvpnConnectionMode, Rect) -> Unit,
) {
    val modes = GoolvpnConnectionMode.entries
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.goolvpn_connection_mode),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    modifier = Modifier.onGloballyPositioned {
                        onModeBoundsChange(mode, it.boundsInWindow())
                    },
                    selected = selectedMode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    icon = {},
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedMode == mode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        FittingSingleLineText(
                            text = stringResource(
                                when (mode) {
                                    GoolvpnConnectionMode.Automatic -> R.string.goolvpn_mode_automatic
                                    GoolvpnConnectionMode.Fast -> R.string.goolvpn_mode_fast
                                    GoolvpnConnectionMode.Stable -> R.string.goolvpn_mode_stable
                                },
                            ),
                            overflow = TextOverflow.Clip,
                            minFontSizeSp = 9f,
                            maxFontSizeSp = 14f,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (selectedMode) {
                    GoolvpnConnectionMode.Automatic -> R.string.goolvpn_mode_automatic_description
                    GoolvpnConnectionMode.Fast -> R.string.goolvpn_mode_fast_description
                    GoolvpnConnectionMode.Stable -> R.string.goolvpn_mode_stable_description
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FittingSingleLineText(
    text: String,
    minFontSizeSp: Float,
    maxFontSizeSp: Float,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    stepSizeSp: Float = 0.5f,
) {
    val fontScale = LocalDensity.current.fontScale
    var fontSizeSp by remember(
        text,
        fontScale,
        minFontSizeSp,
        maxFontSizeSp,
        stepSizeSp,
    ) {
        mutableStateOf(maxFontSizeSp)
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSizeSp.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = overflow,
        onTextLayout = { result ->
            val nextSize = nextFittingFontSizeSp(
                currentSizeSp = fontSizeSp,
                minSizeSp = minFontSizeSp,
                stepSizeSp = stepSizeSp,
                didOverflow = result.didOverflowWidth,
            )
            if (nextSize != fontSizeSp) fontSizeSp = nextSize
        },
    )
}

@Composable
private fun ActivationContent(
    loading: Boolean,
    error: String?,
    onActivate: (String) -> Unit,
    onGetCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activationCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.goolvpn_launcher_icon),
            contentDescription = null,
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.goolvpn_activation_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.goolvpn_activation_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onGetCode,
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.goolvpn_get_code))
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = activationCode,
            onValueChange = { activationCode = it.trim() },
            label = { Text(stringResource(R.string.goolvpn_activation_code)) },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { onActivate(activationCode) },
            enabled = activationCode.isNotBlank() && !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(stringResource(R.string.goolvpn_activate))
            }
        }
    }
}

private fun formatExpiry(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val parsed = parser.parse(value.take(19)) ?: return@runCatching value.take(16).replace('T', ' ')
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(parsed)
    }.getOrElse {
        value.take(16).replace('T', ' ')
    }
}
