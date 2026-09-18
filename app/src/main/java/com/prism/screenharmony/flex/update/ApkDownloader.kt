package com.prism.screenharmony.flex.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.prism.screenharmony.flex.diagnostics.AppLogger
import com.prism.screenharmony.flex.diagnostics.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    object Verifying : DownloadState()
    data class ReadyToInstall(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

object ApkDownloader {
    private const val TAG = "ApkDownloader"
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }

    suspend fun downloadAndVerifyApk(
        context: Context,
        downloadUrl: String,
        expectedSha256: String?,
        versionCode: Long
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Downloading(0f, 0L, -1L)
            AppLogger.i(LogCategory.SYSTEM, TAG, "Starting APK download from: $downloadUrl")

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ScreenHarmonyFlex-Updater")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Server returned HTTP $responseCode")
            }

            val totalBytes = connection.contentLength.toLong()
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updateDir, "screenharmony_v${versionCode}.apk")
            if (destinationFile.exists()) destinationFile.delete()

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L

            connection.inputStream.use { input: InputStream ->
                FileOutputStream(destinationFile).use { output: FileOutputStream ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        _downloadState.value = DownloadState.Downloading(progress, downloadedBytes, totalBytes)
                    }
                    output.flush()
                }
            }

            // Cryptographic SHA-256 Verification
            _downloadState.value = DownloadState.Verifying
            if (!expectedSha256.isNullOrBlank()) {
                val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
                AppLogger.i(LogCategory.SYSTEM, TAG, "Computed SHA-256: $computedHash vs Expected: $expectedSha256")
                if (!computedHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    destinationFile.delete()
                    throw SecurityException("APK SHA-256 hash mismatch! Computed: $computedHash, Expected: $expectedSha256")
                }
            }

            AppLogger.i(LogCategory.SYSTEM, TAG, "APK successfully downloaded and verified: ${destinationFile.absolutePath}")
            _downloadState.value = DownloadState.ReadyToInstall(destinationFile)
            Result.success(destinationFile)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SYSTEM, TAG, "APK Download failed: ${e.message}", e)
            _downloadState.value = DownloadState.Failed(e.localizedMessage ?: "Download failed")
            Result.failure(e)
        }
    }

    fun triggerInstall(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) {
                AppLogger.w(LogCategory.SYSTEM, TAG, "Cannot install: File does not exist")
                return false
            }

            // Check Unknown Sources Permission for Android O (API 26) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    AppLogger.i(LogCategory.SYSTEM, TAG, "Requesting UNKNOWN_APP_SOURCES permission")
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return false
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            AppLogger.i(LogCategory.SYSTEM, TAG, "Triggered PackageInstaller for: ${apkFile.name}")
            true
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SYSTEM, TAG, "Error launching PackageInstaller: ${e.message}", e)
            false
        }
    }
}
