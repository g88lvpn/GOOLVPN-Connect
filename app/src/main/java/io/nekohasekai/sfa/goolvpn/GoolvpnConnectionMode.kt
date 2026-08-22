package io.nekohasekai.sfa.goolvpn

enum class GoolvpnConnectionMode(
    val preferenceValue: String,
    val outboundTag: String,
) {
    Automatic("auto", "goolvpn-auto"),
    Fast("fast", "goolvpn-fast"),
    Stable("stable", "goolvpn-stable"),
    ;

    companion object {
        fun fromPreference(value: String?): GoolvpnConnectionMode =
            entries.firstOrNull { it.preferenceValue == value } ?: Automatic
    }
}
