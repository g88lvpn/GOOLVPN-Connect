package io.nekohasekai.sfa.goolvpn

import android.content.Context

object GoolvpnPerAppStore {
    private const val PREFERENCES_NAME = "goolvpn_per_app"
    private const val KEY_SELECTED_PACKAGES = "selected_packages"

    fun load(context: Context): Set<String>? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.contains(KEY_SELECTED_PACKAGES)) return null
        return preferences.getStringSet(KEY_SELECTED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun save(context: Context, packageNames: Set<String>) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SELECTED_PACKAGES, packageNames.toSet())
            .apply()
    }
}
