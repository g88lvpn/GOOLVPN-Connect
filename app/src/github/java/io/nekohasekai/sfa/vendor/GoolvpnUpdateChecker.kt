package io.nekohasekai.sfa.vendor

import android.os.Build
import io.nekohasekai.sfa.BuildConfig
import io.nekohasekai.sfa.update.UpdateInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class GoolvpnUpdateChecker {
    fun checkUpdate(): UpdateInfo? {
        val endpoint = BuildConfig.GOOLVPN_API_URL.trimEnd('/') + "/app/releases/latest"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "GOOLVPN-Connect/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})",
            )

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw Exception("GOOLVPN update server returned HTTP $responseCode")
            }

            val response = JSONObject(content)
            if (!response.optBoolean("ok", false)) {
                throw Exception("GOOLVPN update server returned an invalid response")
            }
            if (!response.optBoolean("enabled", false)) {
                return null
            }

            val versionCode = response.optInt("version_code", 0)
            if (versionCode <= BuildConfig.VERSION_CODE) {
                return null
            }

            val versionName = response.optString("version_name").trim()
            val downloadUrl = requireGoolvpnUpdateUrl(response.optString("download_url"), "download_url")
            val releaseUrl = requireGoolvpnUpdateUrl(response.optString("release_url"), "release_url")
            val sha256 = response.optString("sha256").trim().lowercase()
            if (versionName.isBlank() || !sha256.matches(Regex("^[0-9a-f]{64}$"))) {
                throw Exception("GOOLVPN update manifest is incomplete")
            }

            return UpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                releaseUrl = releaseUrl,
                releaseNotes = response.optString("release_notes").takeIf { it.isNotBlank() },
                isPrerelease = response.optBoolean("is_prerelease", false),
                fileSize = response.optLong("file_size", 0L).coerceAtLeast(0L),
                sha256 = sha256,
            )
        } finally {
            connection.disconnect()
        }
    }

}

private val trustedGoolvpnUpdateHosts = setOf("goolv.site", "www.goolv.site")

internal fun requireGoolvpnUpdateUrl(value: String, field: String): String {
    val candidate = value.trim()
    val uri = runCatching { URI(candidate) }.getOrNull()
    val host = uri?.host?.lowercase()
    if (
        uri == null ||
        !uri.scheme.equals("https", ignoreCase = true) ||
        uri.userInfo != null ||
        uri.port != -1 ||
        uri.fragment != null ||
        host !in trustedGoolvpnUpdateHosts
    ) {
        throw Exception("GOOLVPN update manifest has invalid $field")
    }
    return candidate
}
