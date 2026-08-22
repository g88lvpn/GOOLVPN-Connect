package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoolvpnAppPresetsTest {
    @Test
    fun `only explicitly reviewed installed packages are suggested`() {
        val banking = GoolvpnAppPresets.all[1]

        val installed = setOf("ru.sberbankmobile", "ru.unknown.application")
        val suggested = GoolvpnAppPresets.installedPackageNames(banking, installed)

        assertEquals(setOf("ru.sberbankmobile"), suggested)
        assertFalse(suggested.contains("ru.unknown.application"))
    }

    @Test
    fun `catalog keeps every package in exactly one group`() {
        val packageNames = GoolvpnAppPresets.all.flatMap { it.packageNames }

        assertEquals(packageNames.size, packageNames.toSet().size)
        assertTrue(packageNames.contains("com.idamob.tinkoff.android"))
        assertTrue(packageNames.contains("com.wildberries.ru"))
    }
}
