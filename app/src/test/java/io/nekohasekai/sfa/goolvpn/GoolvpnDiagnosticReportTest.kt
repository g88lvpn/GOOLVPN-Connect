package io.nekohasekai.sfa.goolvpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoolvpnDiagnosticReportTest {
    @Test
    fun `renders stable support fields`() {
        val report = GoolvpnDiagnosticSnapshot(
            appVersion = "0.5.0",
            androidVersion = "15 (35)",
            vpnStatus = "started",
            networkTransport = "wifi",
            networkValidated = true,
            backendReachable = true,
            activated = true,
            accessActive = true,
            profileReady = true,
            connectionMode = "automatic",
            lastError = null,
        ).render()

        assertTrue(report.contains("app_version=0.5.0"))
        assertTrue(report.contains("network=wifi"))
        assertTrue(report.contains("backend=reachable"))
        assertTrue(report.contains("route_strategy=automatic_urltest"))
        assertTrue(report.contains("last_error=none"))
    }

    @Test
    fun `describes the configured route strategy without exposing endpoints`() {
        val snapshot = GoolvpnDiagnosticSnapshot(
            appVersion = "0.7.2",
            androidVersion = "15 (35)",
            vpnStatus = "started",
            networkTransport = "cellular",
            networkValidated = true,
            backendReachable = true,
            activated = true,
            accessActive = true,
            profileReady = true,
            connectionMode = "fast",
            lastError = null,
        )

        assertTrue(snapshot.render().contains("route_strategy=hysteria_first_urltest"))
        assertFalse(snapshot.render().contains("v2.goolv.site"))
    }

    @Test
    fun `sanitizes multiline error and bounds its length`() {
        val secretLookingValue = "first line\nAuthorization: Bearer secret-token" + "x".repeat(300)
        val report = GoolvpnDiagnosticSnapshot(
            appVersion = "0.5.0",
            androidVersion = "15 (35)",
            vpnStatus = "stopped",
            networkTransport = "none",
            networkValidated = false,
            backendReachable = false,
            activated = false,
            accessActive = false,
            profileReady = false,
            connectionMode = "automatic",
            lastError = secretLookingValue,
        ).render()

        val lastErrorLine = report.lineSequence().last()
        assertFalse(lastErrorLine.contains('\n'))
        assertFalse(lastErrorLine.contains("secret-token"))
        assertTrue(lastErrorLine.length <= 171)
    }
}
