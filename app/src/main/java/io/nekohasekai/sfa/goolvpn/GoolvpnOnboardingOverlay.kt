package io.nekohasekai.sfa.goolvpn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R

enum class GoolvpnCoachmarkPosition {
    Top,
    Bottom,
}

@Composable
fun GoolvpnOnboardingOverlay(
    targetBounds: Rect?,
    title: String,
    message: String,
    step: Int,
    position: GoolvpnCoachmarkPosition,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlayBounds by remember { mutableStateOf<Rect?>(null) }
    val highlightPadding = 10.dp
    val highlightCorner = 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayBounds = it.boundsInWindow() },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            drawRect(Color.Black.copy(alpha = 0.58f))
            val rootBounds = overlayBounds ?: return@Canvas
            val target = targetBounds ?: return@Canvas
            val padding = highlightPadding.toPx()
            val corner = highlightCorner.toPx()
            val left = target.left - rootBounds.left - padding
            val top = target.top - rootBounds.top - padding
            val highlightSize = Size(target.width + padding * 2, target.height + padding * 2)
            val highlightOffset = Offset(left, top)
            val cornerRadius = CornerRadius(corner, corner)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = highlightOffset,
                size = highlightSize,
                cornerRadius = cornerRadius,
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = highlightOffset,
                size = highlightSize,
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Surface(
            modifier = Modifier
                .align(
                    if (position == GoolvpnCoachmarkPosition.Top) {
                        Alignment.TopCenter
                    } else {
                        Alignment.BottomCenter
                    },
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .heightIn(max = 232.dp)
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.goolvpn_onboarding_progress, step, 5),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.goolvpn_onboarding_skip))
                        }
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                stringResource(
                                    if (step == 5) R.string.goolvpn_onboarding_done
                                    else R.string.goolvpn_onboarding_next,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
