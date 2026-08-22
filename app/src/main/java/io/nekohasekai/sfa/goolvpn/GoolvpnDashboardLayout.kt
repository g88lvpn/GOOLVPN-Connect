package io.nekohasekai.sfa.goolvpn

internal data class GoolvpnDashboardLayoutSpec(
    val panelHorizontalPaddingDp: Int,
    val connectButtonDiameterDp: Int,
    val connectButtonContentPaddingDp: Int,
)

internal fun dashboardLayoutSpec(
    availableWidthDp: Int,
    fontScale: Float,
): GoolvpnDashboardLayoutSpec {
    val narrowScreen = availableWidthDp < 360
    val largeText = fontScale >= 1.3f
    val panelPadding = if (narrowScreen) 12 else 18
    val targetButtonDiameter = when {
        fontScale >= 1.6f -> 164
        narrowScreen || largeText -> 148
        else -> 132
    }
    val availableContentWidth = (availableWidthDp - panelPadding * 2).coerceAtLeast(96)
    val compactContent = narrowScreen || largeText

    return GoolvpnDashboardLayoutSpec(
        panelHorizontalPaddingDp = panelPadding,
        connectButtonDiameterDp = targetButtonDiameter.coerceAtMost(availableContentWidth),
        connectButtonContentPaddingDp = if (compactContent) 4 else 8,
    )
}

internal fun nextFittingFontSizeSp(
    currentSizeSp: Float,
    minSizeSp: Float,
    stepSizeSp: Float,
    didOverflow: Boolean,
): Float {
    if (!didOverflow) return currentSizeSp
    return (currentSizeSp - stepSizeSp).coerceAtLeast(minSizeSp)
}
