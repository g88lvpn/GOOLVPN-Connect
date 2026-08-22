package io.nekohasekai.sfa.goolvpn

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ActivationLinkParser {
    private const val ACTIVATION_SCHEME = "goolvpn"
    private const val ACTIVATION_AUTHORITY = "activate"
    private val activationParameters = setOf("token", "code")

    fun parse(link: String): String? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return null
        if (
            uri.scheme != ACTIVATION_SCHEME ||
            uri.rawAuthority != ACTIVATION_AUTHORITY ||
            !uri.rawPath.isNullOrEmpty() ||
            uri.rawFragment != null
        ) {
            return null
        }

        val queryParts = uri.rawQuery?.split('&') ?: return null
        if (queryParts.size != 1) return null

        val queryPart = queryParts.single()
        val separatorIndex = queryPart.indexOf('=')
        if (separatorIndex <= 0) return null

        val parameterName = queryPart.substring(0, separatorIndex)
        if (parameterName !in activationParameters) return null

        val value = decode(queryPart.substring(separatorIndex + 1))?.trim()
        return value?.takeIf(String::isNotEmpty)
    }

    private fun decode(value: String): String? = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrNull()
}
