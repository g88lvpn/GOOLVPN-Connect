package io.nekohasekai.sfa.goolvpn

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoolvpnSmartBypassTest {
    @Test
    fun `only selected reviewed group is routed directly`() {
        val groups = parseSmartBypassGroups(
            JSONObject(
                """{"version":"test.1","groups":[{"id":"yandex","title":"Yandex","domains":["yandex.ru"]},{"id":"invalid","title":"Invalid","domains":["0.0.0.0/0"]}]}""",
            ),
        )
        val config = """{"route":{"rules":[{"protocol":"bittorrent","action":"reject"},{"network":"icmp","outbound":"direct"}],"final":"GOOLVPN"}}"""

        val rules = JSONObject(applySmartBypassRules(config, groups, setOf("yandex")))
            .getJSONObject("route").getJSONArray("rules")

        assertEquals(3, rules.length())
        assertEquals("direct", rules.getJSONObject(1).getString("outbound"))
        assertEquals("yandex.ru", rules.getJSONObject(1).getJSONArray("domain_suffix").getString(0))
        assertFalse(groups.any { it.id == "invalid" })
    }

    @Test
    fun `disabled catalog leaves server config unchanged`() {
        val config = """{"route":{"rules":[]}}"""
        assertEquals(config, applySmartBypassRules(config, emptyList(), emptySet()))
    }
}
