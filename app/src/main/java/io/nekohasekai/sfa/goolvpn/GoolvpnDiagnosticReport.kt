package io.nekohasekai.sfa.goolvpn

data class GoolvpnDiagnosticSnapshot(
    val appVersion: String,
    val androidVersion: String,
    val vpnStatus: String,
    val networkTransport: String,
    val networkValidated: Boolean,
    val backendReachable: Boolean,
    val activated: Boolean,
    val accessActive: Boolean,
    val profileReady: Boolean,
    val connectionMode: String,
    val smartBypassVersion: String? = null,
    val smartBypassGroups: Set<String> = emptySet(),
    val lastError: String?,
) {
    val isHealthy: Boolean
        get() = networkValidated && backendReachable && activated && accessActive && profileReady

    /**
     * This describes the configured selection strategy, not a guessed live node.
     * sing-box may change the leaf outbound inside a URLTest group at any time.
     */
    val routeStrategy: String
        get() = when (connectionMode) {
            "automatic" -> "automatic_urltest"
            "fast" -> "hysteria_first_urltest"
            "stable" -> "compatible_urltest"
            else -> "unknown"
        }

    fun render(): String = buildString {
        appendLine("GOOLVPN Connect diagnostics")
        appendLine("app_version=${oneLine(appVersion)}")
        appendLine("android=${oneLine(androidVersion)}")
        appendLine("vpn_status=${oneLine(vpnStatus)}")
        appendLine("network=${oneLine(networkTransport)}")
        appendLine("network_validated=$networkValidated")
        appendLine("backend=${if (backendReachable) "reachable" else "unreachable"}")
        appendLine("activated=$activated")
        appendLine("access_active=$accessActive")
        appendLine("profile_ready=$profileReady")
        appendLine("mode=${oneLine(connectionMode)}")
        appendLine("route_strategy=$routeStrategy")
        appendLine("smart_bypass_version=${smartBypassVersion?.let(::oneLine) ?: "none"}")
        appendLine("smart_bypass_groups=${smartBypassGroups.sorted().joinToString(",")}")
        append("last_error=${lastError?.let(::oneLine) ?: "none"}")
    }

    private fun oneLine(value: String): String = value
        .replace(BEARER_TOKEN, "Bearer [redacted]")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .trim()
        .take(MAX_VALUE_LENGTH)

    private companion object {
        const val MAX_VALUE_LENGTH = 160
        val BEARER_TOKEN = Regex("(?i)Bearer\\s+[^\\s,;]+")
    }
}
