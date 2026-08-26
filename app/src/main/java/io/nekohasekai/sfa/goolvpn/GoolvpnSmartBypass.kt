package io.nekohasekai.sfa.goolvpn

import org.json.JSONArray
import org.json.JSONObject

data class GoolvpnSmartBypassGroup(
    val id: String,
    val title: String,
    val examples: String,
    val domains: Set<String>,
    val catalogVersion: String,
)

internal fun parseSmartBypassGroups(payload: JSONObject?): List<GoolvpnSmartBypassGroup> {
    if (payload == null) return emptyList()
    val version = payload.optString("version").takeIf { it.isNotBlank() } ?: return emptyList()
    val groups = payload.optJSONArray("groups") ?: return emptyList()
    return buildList {
        for (index in 0 until groups.length()) {
            val group = groups.optJSONObject(index) ?: continue
            val id = group.optString("id")
            val title = group.optString("title")
            val domains = group.optJSONArray("domains").toDomainSet()
            if (id.isNotBlank() && title.isNotBlank() && domains.isNotEmpty()) {
                add(GoolvpnSmartBypassGroup(id, title, group.optString("examples"), domains, version))
            }
        }
    }
}

/** Inserts direct rules before GOOLVPN's final selector rule, never replacing it. */
internal fun applySmartBypassRules(
    config: String,
    groups: List<GoolvpnSmartBypassGroup>,
    enabledGroupIds: Set<String>,
): String {
    val enabledGroups = groups.filter { it.id in enabledGroupIds }
    if (enabledGroups.isEmpty()) return config
    val root = JSONObject(config)
    val route = root.optJSONObject("route") ?: return config
    val originalRules = route.optJSONArray("rules") ?: JSONArray()
    val updatedRules = JSONArray()
    var inserted = false
    for (index in 0 until originalRules.length()) {
        val rule = originalRules.optJSONObject(index) ?: continue
        updatedRules.put(rule)
        if (!inserted && rule.optString("protocol") == "bittorrent") {
            enabledGroups.forEach { group ->
                updatedRules.put(
                    JSONObject().apply {
                        put("domain_suffix", JSONArray(group.domains.sorted()))
                        put("action", "route")
                        put("outbound", "direct")
                    },
                )
            }
            inserted = true
        }
    }
    if (!inserted) return config
    route.put("rules", updatedRules)
    return root.toString()
}

private fun JSONArray?.toDomainSet(): Set<String> = buildSet {
    if (this@toDomainSet == null) return@buildSet
    for (index in 0 until this@toDomainSet.length()) {
        val domain = this@toDomainSet.optString(index).trim().lowercase()
        if (domain.matches(Regex("[a-z0-9.-]+")) && !domain.contains("/")) add(domain)
    }
}
