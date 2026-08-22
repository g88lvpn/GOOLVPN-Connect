package io.nekohasekai.sfa.bg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultNetworkRecoveryPolicyTest {
    @Test
    fun `signature distinguishes network and interface changes`() {
        val wifi = signature(networkId = 1L, interfaceName = "wlan0", interfaceIndex = 10)

        assertEquals(wifi, wifi.copy())
        assertNotEquals(wifi, wifi.copy(networkId = 2L))
        assertNotEquals(wifi, wifi.copy(interfaceName = "rmnet_data0"))
        assertNotEquals(wifi, wifi.copy(interfaceIndex = 11))
    }

    @Test
    fun `distinct observation replaces pending recovery during debounce`() {
        val policy = policy(debounceMillis = 100L)
        val wifi = signature(networkId = 1L, interfaceName = "wlan0", interfaceIndex = 10)
        val cellular = signature(networkId = 2L, interfaceName = "rmnet_data0", interfaceIndex = 11)

        assertTrue(policy.observe(wifi, nowMillis = 0L))
        assertNull(policy.takeRecovery(nowMillis = 99L))

        assertTrue(policy.observe(cellular, nowMillis = 50L))
        assertNull(policy.takeRecovery(nowMillis = 149L))
        assertEquals(cellular, policy.takeRecovery(nowMillis = 150L))
    }

    @Test
    fun `failed recovery is held until cooldown expires`() {
        val policy = policy(debounceMillis = 0L, cooldownMillis = 1_000L)
        val wifi = signature(networkId = 1L, interfaceName = "wlan0", interfaceIndex = 10)

        policy.observe(wifi, nowMillis = 0L)
        assertEquals(wifi, policy.takeRecovery(nowMillis = 0L))
        policy.recoveryFailed(wifi)

        assertFalse(policy.observe(wifi, nowMillis = 100L))
        assertEquals(1L, policy.nextRecoveryDelayMillis(nowMillis = 999L))
        assertNull(policy.takeRecovery(nowMillis = 999L))
        assertEquals(wifi, policy.takeRecovery(nowMillis = 1_000L))
    }

    @Test
    fun `successful recovery suppresses repeats but allows a distinct signature`() {
        val policy = policy(debounceMillis = 0L, cooldownMillis = 0L)
        val wifi = signature(networkId = 1L, interfaceName = "wlan0", interfaceIndex = 10)
        val replacementNetwork = wifi.copy(networkId = 2L)
        val replacementInterface = replacementNetwork.copy(interfaceName = "wlan1", interfaceIndex = 12)

        policy.observe(wifi, nowMillis = 0L)
        assertEquals(wifi, policy.takeRecovery(nowMillis = 0L))
        policy.recoverySucceeded(wifi)

        assertFalse(policy.observe(wifi, nowMillis = 60_000L))
        assertNull(policy.nextRecoveryDelayMillis(nowMillis = 60_000L))

        assertTrue(policy.observe(replacementNetwork, nowMillis = 60_001L))
        assertEquals(replacementNetwork, policy.takeRecovery(nowMillis = 60_001L))
        policy.recoverySucceeded(replacementNetwork)

        assertTrue(policy.observe(replacementInterface, nowMillis = 60_002L))
        assertEquals(replacementInterface, policy.takeRecovery(nowMillis = 60_002L))
    }

    @Test
    fun `latest signature remains pending when network changes during recovery`() {
        val policy = policy(debounceMillis = 0L, cooldownMillis = 0L)
        val wifi = signature(networkId = 1L, interfaceName = "wlan0", interfaceIndex = 10)
        val cellular = signature(networkId = 2L, interfaceName = "rmnet_data0", interfaceIndex = 11)

        policy.observe(wifi, nowMillis = 0L)
        assertEquals(wifi, policy.takeRecovery(nowMillis = 0L))
        assertTrue(policy.observe(cellular, nowMillis = 1L))

        policy.recoverySucceeded(wifi)

        assertEquals(cellular, policy.takeRecovery(nowMillis = 1L))
    }

    @Test
    fun `recovery events are bounded for one observed signature`() {
        val policy =
            policy(
                debounceMillis = 0L,
                cooldownMillis = 0L,
                maxRecoveryEvents = 3,
            )
        val wifi = signature(networkId = 1L)
        policy.observe(wifi, nowMillis = 0L)

        repeat(3) { attempt ->
            assertEquals(wifi, policy.takeRecovery(nowMillis = attempt.toLong()))
            policy.recoveryFailed(wifi)
        }

        assertFalse(policy.observe(wifi, nowMillis = 60_000L))
        assertNull(policy.nextRecoveryDelayMillis(nowMillis = 60_000L))
        assertNull(policy.takeRecovery(nowMillis = 60_000L))

        val cellular = signature(networkId = 2L, interfaceName = "rmnet_data0", interfaceIndex = 11)
        assertTrue(policy.observe(cellular, nowMillis = 60_001L))
        assertEquals(cellular, policy.takeRecovery(nowMillis = 60_001L))
    }

    private fun policy(
        debounceMillis: Long = 250L,
        cooldownMillis: Long = 1_000L,
        maxRecoveryEvents: Int = 4,
    ) = DefaultNetworkRecoveryPolicy(
        debounceMillis = debounceMillis,
        cooldownMillis = cooldownMillis,
        maxRecoveryEvents = maxRecoveryEvents,
    )

    private fun signature(
        networkId: Long,
        interfaceName: String = "wlan0",
        interfaceIndex: Int = 10,
    ) = DefaultNetworkSignature(
        networkId = networkId,
        interfaceName = interfaceName,
        interfaceIndex = interfaceIndex,
    )
}
