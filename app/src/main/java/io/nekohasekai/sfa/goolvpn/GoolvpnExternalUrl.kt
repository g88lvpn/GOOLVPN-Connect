package io.nekohasekai.sfa.goolvpn

import java.net.URI

private val trustedGoolvpnHosts = setOf("goolv.site", "www.goolv.site")

internal fun trustedGoolvpnUrl(value: String?, fallback: String): String {
    val candidate = value?.trim().orEmpty()
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return fallback
    val host = uri.host?.lowercase() ?: return fallback
    return candidate.takeIf {
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.userInfo == null &&
            uri.port == -1 &&
            host in trustedGoolvpnHosts
    } ?: fallback
}
