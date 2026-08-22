package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoolvpnDashboardLayoutTest {
    @Test
    fun `regular screens keep the compact connect control`() {
        val spec = dashboardLayoutSpec(400, 1f)

        assertEquals(18, spec.panelHorizontalPaddingDp)
        assertEquals(132, spec.connectButtonDiameterDp)
        assertEquals(8, spec.connectButtonContentPaddingDp)
    }

    @Test
    fun `narrow screens give action labels more usable width`() {
        val spec = dashboardLayoutSpec(320, 1f)

        assertEquals(12, spec.panelHorizontalPaddingDp)
        assertEquals(148, spec.connectButtonDiameterDp)
        assertEquals(4, spec.connectButtonContentPaddingDp)
    }

    @Test
    fun `large system text expands the connect control`() {
        val spec = dashboardLayoutSpec(400, 1.6f)

        assertEquals(164, spec.connectButtonDiameterDp)
        assertEquals(4, spec.connectButtonContentPaddingDp)
    }

    @Test
    fun `connect control never exceeds the panel content width`() {
        val spec = dashboardLayoutSpec(180, 2f)
        val availableContentWidth = 180 - (spec.panelHorizontalPaddingDp * 2)

        assertTrue(spec.connectButtonDiameterDp <= availableContentWidth)
        assertTrue(spec.connectButtonDiameterDp >= 96)
    }

    @Test
    fun `overflow shrinks a label by one bounded step`() {
        assertEquals(15.5f, nextFittingFontSizeSp(16f, 10f, 0.5f, didOverflow = true))
        assertEquals(10f, nextFittingFontSizeSp(10f, 10f, 0.5f, didOverflow = true))
    }

    @Test
    fun `fitting label keeps its current size`() {
        assertEquals(16f, nextFittingFontSizeSp(16f, 10f, 0.5f, didOverflow = false))
    }
}
