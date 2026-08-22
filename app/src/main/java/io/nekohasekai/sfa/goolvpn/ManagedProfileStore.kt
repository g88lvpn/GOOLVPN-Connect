package io.nekohasekai.sfa.goolvpn

import android.content.Context
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.database.Profile
import io.nekohasekai.sfa.database.ProfileManager
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.database.TypedProfile
import java.io.File
import java.security.MessageDigest
import java.util.Date

class ManagedProfileStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("goolvpn_profile", Context.MODE_PRIVATE)

    suspend fun apply(config: String, version: String?): Boolean {
        Libbox.checkConfig(config)
        val configDirectory = File(context.filesDir, "configs").also { it.mkdirs() }
        val configFile = File(configDirectory, "goolvpn-managed.json")
        val configHash = managedConfigFingerprint(config)
        val changed = shouldRewriteManagedConfig(
            storedVersion = preferences.getString(KEY_VERSION, null),
            incomingVersion = version,
            storedHash = preferences.getString(KEY_CONFIG_HASH, null),
            incomingHash = configHash,
            configExists = configFile.exists(),
        )

        if (changed) {
            val temporaryFile = File(configDirectory, "goolvpn-managed.tmp")
            temporaryFile.writeText(config, Charsets.UTF_8)
            if (!temporaryFile.renameTo(configFile)) {
                configFile.writeText(config, Charsets.UTF_8)
                temporaryFile.delete()
            }
        }

        val savedId = preferences.getLong(KEY_PROFILE_ID, -1L)
        val existing = savedId.takeIf { it >= 0 }?.let { ProfileManager.get(it) }
        val typed = (existing?.typed ?: TypedProfile()).apply {
            type = TypedProfile.Type.Local
            path = configFile.path
            remoteURL = ""
            autoUpdate = false
            lastUpdated = Date()
        }
        val profile = if (existing == null) {
            Profile(
                name = PROFILE_NAME,
                typed = typed,
                userOrder = ProfileManager.nextOrder(),
            ).let { ProfileManager.create(it, andSelect = true) }
        } else {
            existing.name = PROFILE_NAME
            existing.typed = typed
            ProfileManager.update(existing)
            Settings.selectedProfile = existing.id
            existing
        }

        preferences.edit()
            .putLong(KEY_PROFILE_ID, profile.id)
            .putString(KEY_VERSION, version)
            .putString(KEY_CONFIG_HASH, configHash)
            .apply()
        return changed
    }

    suspend fun clear() {
        val profileId = preferences.getLong(KEY_PROFILE_ID, -1L)
        if (profileId >= 0) {
            ProfileManager.get(profileId)?.let { ProfileManager.delete(it) }
            if (Settings.selectedProfile == profileId) {
                Settings.selectedProfile = -1L
            }
        }
        File(context.filesDir, "configs/goolvpn-managed.json").delete()
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PROFILE_NAME = "GOOLVPN"
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_VERSION = "profile_version"
        const val KEY_CONFIG_HASH = "config_hash"
    }
}

internal fun managedConfigFingerprint(config: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(config.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

internal fun shouldRewriteManagedConfig(
    storedVersion: String?,
    incomingVersion: String?,
    storedHash: String?,
    incomingHash: String,
    configExists: Boolean,
): Boolean = !configExists || storedVersion != incomingVersion || storedHash != incomingHash
