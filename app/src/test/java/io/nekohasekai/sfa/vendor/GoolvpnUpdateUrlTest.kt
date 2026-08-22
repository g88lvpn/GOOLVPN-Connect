package io.nekohasekai.sfa.vendor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GoolvpnUpdateUrlTest {
    @Test
    fun `accepts only GOOLVPN release hosts over https`() {
        assertEquals(
            "https://goolv.site/downloads/app.apk",
            requireGoolvpnUpdateUrl("https://goolv.site/downloads/app.apk", "download_url"),
        )
        assertEquals(
            "https://www.goolv.site/connect.html",
            requireGoolvpnUpdateUrl("https://www.goolv.site/connect.html", "release_url"),
        )
    }

    @Test
    fun `rejects foreign hosts credentials ports fragments and non-https schemes`() {
        listOf(
            "https://evil.example/app.apk",
            "https://goolv.site@evil.example/app.apk",
            "https://goolv.site:443/app.apk",
            "https://goolv.site/app.apk#fragment",
            "http://goolv.site/app.apk",
        ).forEach { value ->
            assertThrows(Exception::class.java) {
                requireGoolvpnUpdateUrl(value, "download_url")
            }
        }
    }
}
