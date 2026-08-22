package io.nekohasekai.sfa.goolvpn

enum class GoolvpnThemeMode(val preferenceValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    fun resolveDarkMode(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromPreference(value: String?): GoolvpnThemeMode =
            entries.firstOrNull { it.preferenceValue == value } ?: System
    }
}
