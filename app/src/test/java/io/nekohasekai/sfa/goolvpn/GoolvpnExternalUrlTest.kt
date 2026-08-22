package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertEquals
import org.junit.Test

class GoolvpnExternalUrlTest {
    @Test
    fun `allows only https GOOLVPN web hosts`() {
        assertEquals(
            "https://goolv.site/connect.html",
            trustedGoolvpnUrl("https://goolv.site/connect.html", "https://goolv.site/fallback"),
        )
        assertEquals(
            "https://www.goolv.site/support",
            trustedGoolvpnUrl("https://www.goolv.site/support", "https://goolv.site/fallback"),
        )
    }

    @Test
    fun `rejects foreign hosts credentials and non-https schemes`() {
        val fallback = "https://goolv.site/fallback"
        assertEquals(fallback, trustedGoolvpnUrl("javascript:alert(1)", fallback))
        assertEquals(fallback, trustedGoolvpnUrl("https://evil.example/path", fallback))
        assertEquals(fallback, trustedGoolvpnUrl("https://goolv.site@evil.example/path", fallback))
        assertEquals(fallback, trustedGoolvpnUrl("http://goolv.site/path", fallback))
    }
}
