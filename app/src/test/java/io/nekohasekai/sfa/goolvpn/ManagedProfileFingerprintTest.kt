package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedProfileFingerprintTest {
    @Test
    fun `content change forces rewrite even when version is reused`() {
        val oldHash = managedConfigFingerprint("{\"route\":1}")
        val newHash = managedConfigFingerprint("{\"route\":2}")

        assertNotEquals(oldHash, newHash)
        assertTrue(
            shouldRewriteManagedConfig(
                storedVersion = "same",
                incomingVersion = "same",
                storedHash = oldHash,
                incomingHash = newHash,
                configExists = true,
            ),
        )
    }

    @Test
    fun `unchanged content and version do not rewrite`() {
        val hash = managedConfigFingerprint("{\"route\":1}")
        assertFalse(
            shouldRewriteManagedConfig(
                storedVersion = null,
                incomingVersion = null,
                storedHash = hash,
                incomingHash = hash,
                configExists = true,
            ),
        )
    }
}
