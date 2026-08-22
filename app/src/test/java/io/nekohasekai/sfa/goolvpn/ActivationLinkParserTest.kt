package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivationLinkParserTest {
    @Test
    fun `parses token parameter from activation link`() {
        assertEquals("activation-token", ActivationLinkParser.parse("goolvpn://activate?token=activation-token"))
    }

    @Test
    fun `parses and decodes code parameter from activation link`() {
        assertEquals("code+with spaces", ActivationLinkParser.parse("goolvpn://activate?code=code%2Bwith%20spaces"))
    }

    @Test
    fun `rejects empty links and activation values`() {
        assertNull(ActivationLinkParser.parse(""))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?token="))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?code=%20%20"))
    }

    @Test
    fun `rejects foreign schemes and hosts`() {
        assertNull(ActivationLinkParser.parse("https://activate?token=activation-token"))
        assertNull(ActivationLinkParser.parse("goolvpn://account?token=activation-token"))
        assertNull(ActivationLinkParser.parse("GOOLVPN://activate?token=activation-token"))
    }

    @Test
    fun `rejects activation links with extra URI components`() {
        assertNull(ActivationLinkParser.parse("goolvpn://activate/path?token=activation-token"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate:443?token=activation-token"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?token=activation-token#fragment"))
    }

    @Test
    fun `rejects missing malformed and ambiguous query parameters`() {
        assertNull(ActivationLinkParser.parse("goolvpn://activate"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?key=activation-token"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?token=one&code=two"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?token=one&token=two"))
        assertNull(ActivationLinkParser.parse("goolvpn://activate?token=%ZZ"))
    }
}
