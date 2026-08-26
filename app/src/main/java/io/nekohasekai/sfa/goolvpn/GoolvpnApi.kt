package io.nekohasekai.sfa.goolvpn

import android.os.Build
import io.nekohasekai.sfa.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GoolvpnProfile(
    val active: Boolean,
    val status: String,
    val planName: String,
    val planTitle: String,
    val expiresAt: String?,
    val isLifetime: Boolean,
    val profileVersion: String?,
    val config: String?,
    val smartBypassGroups: List<GoolvpnSmartBypassGroup>,
    val message: String?,
    val accountUrl: String,
    val supportUrl: String,
)

data class GoolvpnDevice(
    val id: Int,
    val name: String,
    val appVersion: String?,
    val lastSeenAt: String?,
    val isCurrent: Boolean,
)

data class GoolvpnDevices(val limit: Int, val used: Int, val devices: List<GoolvpnDevice>)

class GoolvpnApi {
    fun healthCheck(): Boolean = request("GET", "/site/status").optBoolean("ok", false)

    fun activate(activationToken: String): String {
        val body = JSONObject().apply {
            put("token", activationToken)
            put("device_name", listOf(Build.MANUFACTURER, Build.MODEL).joinToString(" ").trim())
            put("platform", "android")
            put("app_version", BuildConfig.VERSION_NAME)
        }
        return request("POST", "/app/activate", body = body)
            .getString("device_token")
    }

    fun getProfile(deviceToken: String): GoolvpnProfile {
        val response = request("GET", "/app/profile", bearerToken = deviceToken)
        val defaultAccountUrl = "https://goolv.site/connect.html"
        val defaultSupportUrl = "https://goolv.site/support.html"
        return GoolvpnProfile(
            active = response.optBoolean("active"),
            status = response.optString("status", "inactive"),
            planName = response.optString("plan_name", "base"),
            planTitle = response.optString("plan_title", "GOOLVPN"),
            expiresAt = response.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
            isLifetime = response.optBoolean("is_lifetime"),
            profileVersion = response.optString("profile_version").takeIf { it.isNotBlank() && it != "null" },
            config = response.optJSONObject("config")?.toString(),
            smartBypassGroups = parseSmartBypassGroups(response.optJSONObject("smart_bypass")),
            message = response.optString("message").takeIf { it.isNotBlank() && it != "null" },
            accountUrl = trustedGoolvpnUrl(response.optString("account_url"), defaultAccountUrl),
            supportUrl = trustedGoolvpnUrl(response.optString("support_url"), defaultSupportUrl),
        )
    }

    fun revoke(deviceToken: String) {
        request("POST", "/app/devices/revoke-current", bearerToken = deviceToken)
    }

    fun getDevices(deviceToken: String): GoolvpnDevices {
        val response = request("GET", "/app/devices", bearerToken = deviceToken)
        val devices = response.optJSONArray("devices") ?: org.json.JSONArray()
        return GoolvpnDevices(
            limit = response.optInt("limit"),
            used = response.optInt("used"),
            devices = List(devices.length()) { index ->
                val device = devices.getJSONObject(index)
                GoolvpnDevice(
                    id = device.getInt("id"),
                    name = device.optString("device_name", "Android device"),
                    appVersion = device.optString("app_version").takeIf { it.isNotBlank() && it != "null" },
                    lastSeenAt = device.optString("last_seen_at").takeIf { it.isNotBlank() && it != "null" },
                    isCurrent = device.optBoolean("current"),
                )
            },
        )
    }

    fun revokeDevice(deviceToken: String, deviceId: Int) {
        request("POST", "/app/devices/$deviceId/revoke", bearerToken = deviceToken)
    }

    fun submitDiagnostic(deviceToken: String, report: String): Int {
        val body = JSONObject().put("report", report)
        return request("POST", "/app/support/diagnostic", body, deviceToken)
            .getInt("ticket_id")
    }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): JSONObject {
        val endpoint = BuildConfig.GOOLVPN_API_URL.trimEnd('/') + path
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "GOOLVPN-Connect/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})",
            )
            if (!bearerToken.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body.toString())
                }
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val detail = runCatching { JSONObject(content).optString("detail") }.getOrNull()
                throw GoolvpnApiException(responseCode, detail?.takeIf { it.isNotBlank() } ?: "Server error")
            }
            return if (content.isBlank()) JSONObject() else JSONObject(content)
        } finally {
            connection.disconnect()
        }
    }
}

class GoolvpnApiException(val statusCode: Int, message: String) : Exception(message)
