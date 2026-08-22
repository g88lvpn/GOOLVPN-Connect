package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoolvpnThemeModeTest {
    @Test
    fun `system mode follows device theme`() {
        assertTrue(GoolvpnThemeMode.System.resolveDarkMode(systemDark = true))
        assertFalse(GoolvpnThemeMode.System.resolveDarkMode(systemDark = false))
    }

    @Test
    fun `manual modes override device theme`() {
        assertFalse(GoolvpnThemeMode.Light.resolveDarkMode(systemDark = true))
        assertTrue(GoolvpnThemeMode.Dark.resolveDarkMode(systemDark = false))
    }

    @Test
    fun `stored values are parsed safely`() {
        assertEquals(GoolvpnThemeMode.System, GoolvpnThemeMode.fromPreference(null))
        assertEquals(GoolvpnThemeMode.System, GoolvpnThemeMode.fromPreference("unknown"))
        assertEquals(GoolvpnThemeMode.Light, GoolvpnThemeMode.fromPreference("light"))
        assertEquals(GoolvpnThemeMode.Dark, GoolvpnThemeMode.fromPreference("dark"))
    }
}
