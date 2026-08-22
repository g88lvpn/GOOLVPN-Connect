package io.nekohasekai.sfa.vendor

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.core.content.FileProvider
import io.nekohasekai.sfa.update.UpdateState
import java.io.File

object SystemPackageInstaller {

    fun canSystemSilentInstall(): Boolean = false

    fun install(context: Context, apkFile: File) {
        require(apkFile.isFile && apkFile.length() > 0L) {
            "Downloaded update APK is missing"
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.cache",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        UpdateState.setInstallStatus(UpdateState.InstallStatus.Installing)
        try {
            context.startActivity(installIntent)
        } catch (error: ActivityNotFoundException) {
            UpdateState.setInstallStatus(
                UpdateState.InstallStatus.Failed("Android package installer is unavailable"),
            )
            throw error
        }
    }
}
