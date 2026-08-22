package io.nekohasekai.sfa.vendor

import io.nekohasekai.libbox.HTTPResponseWriteToProgressHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.update.UpdateState
import io.nekohasekai.sfa.utils.HTTPClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ApkDownloader : Closeable {
    private val client = Libbox.newHTTPClient().apply {
        modernTLS()
        keepAlive()
    }

    suspend fun download(url: String, expectedSha256: String): File = withContext(Dispatchers.IO) {
        val cacheDir = File(Application.application.cacheDir, "updates")
        cacheDir.mkdirs()
        val apkFile = File(cacheDir, "update.apk")

        if (apkFile.exists()) apkFile.delete()

        val request = client.newRequest()
        request.setUserAgent(HTTPClient.userAgent)
        request.setURL(url)

        val response = request.execute()
        response.writeToWithProgress(
            apkFile.absolutePath,
            object : HTTPResponseWriteToProgressHandler {
                override fun update(progress: Long, total: Long) {
                    UpdateState.downloadProgress.value =
                        if (total > 0) progress.toFloat() / total.toFloat() else null
                }
            },
        )

        if (!apkFile.exists() || apkFile.length() == 0L) {
            throw Exception("Download failed: empty file")
        }
        if (!matchesSha256(apkFile, expectedSha256)) {
            apkFile.delete()
            throw Exception("Download failed: SHA-256 mismatch")
        }

        UpdateState.saveApkPath(apkFile)
        apkFile
    }

    companion object {
        fun matchesSha256(file: File, expectedSha256: String?): Boolean {
            val expected = expectedSha256?.trim()?.lowercase().orEmpty()
            if (!expected.matches(Regex("^[0-9a-f]{64}$")) || !file.isFile) {
                return false
            }

            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
            return actual == expected
        }
    }

    override fun close() {
        client.close()
    }
}
