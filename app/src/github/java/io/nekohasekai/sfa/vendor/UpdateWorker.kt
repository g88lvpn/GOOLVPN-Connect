package io.nekohasekai.sfa.vendor

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.update.UpdateState
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateWorker(private val appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        private const val WORK_NAME = "AutoUpdate"
        private const val TAG = "UpdateWorker"

        fun schedule(context: Context) {
            if (!Settings.autoUpdateEnabled) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Auto update disabled, cancelled scheduled work")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
                24,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
            Log.d(TAG, "Auto update scheduled")
        }
    }

    override suspend fun doWork(): Result {
        if (!Settings.autoUpdateEnabled) {
            Log.d(TAG, "Auto update disabled, skipping")
            return Result.success()
        }

        Log.d(TAG, "Checking for updates...")

        return try {
            val updateInfo = GoolvpnUpdateChecker().checkUpdate()

            if (updateInfo == null) {
                Log.d(TAG, "No update available")
                UpdateReadyNotification.cancel(appContext)
                UpdateState.clear()
                return Result.success()
            }

            Log.d(TAG, "Update available: ${updateInfo.versionName}")
            UpdateState.setUpdate(updateInfo)

            val expectedSha256 = updateInfo.sha256.orEmpty()
            val cachedFile = Settings.cachedApkPath
                .takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { ApkDownloader.matchesSha256(it, expectedSha256) }
            val apkFile = cachedFile ?: ApkDownloader().use {
                Log.d(TAG, "Preloading update over unmetered network...")
                it.download(updateInfo.downloadUrl, expectedSha256)
            }
            UpdateState.saveApkPath(apkFile)

            if (Settings.silentInstallEnabled && ApkInstaller.canSilentInstall()) {
                Log.d(TAG, "Installing update...")
                ApkInstaller.install(appContext, apkFile)
                Log.d(TAG, "Update installed successfully")
            } else {
                if (Settings.updateReadyNotifiedVersion != updateInfo.versionCode) {
                    Settings.lastShownUpdateVersion = 0
                    Settings.updateReadyNotifiedVersion = updateInfo.versionCode
                    UpdateReadyNotification.show(appContext, updateInfo)
                }
                Log.d(TAG, "Update is verified and ready for user confirmation")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto update failed", e)
            Result.retry()
        }
    }
}
