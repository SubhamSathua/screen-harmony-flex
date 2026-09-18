package com.prism.screenharmony.flex.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.prism.screenharmony.flex.BuildConfig
import com.prism.screenharmony.flex.diagnostics.AppLogger
import com.prism.screenharmony.flex.diagnostics.LogCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "screen_harmony_updater_prefs"
    private const val KEY_CHANNEL = "update_channel"
    private const val KEY_LAST_CHECK = "last_check_timestamp"
    private const val KEY_IGNORED_VERSION = "ignored_optional_version"
    private const val KEY_CUSTOM_MANIFEST_URL = "custom_manifest_url"

    const val DEFAULT_MANIFEST_URL = "https://subhamsathua.github.io/screen-harmony-flex/update/update.json"
    const val FALLBACK_MANIFEST_URL = "https://raw.githubusercontent.com/SubhamSathua/screen-harmony-flex/main/update/update.json"

    private val _updateResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateResult: StateFlow<UpdateCheckResult> = _updateResult.asStateFlow()

    private val _lastCheckedTimestamp = MutableStateFlow<Long>(0L)
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    private val _currentChannel = MutableStateFlow<String>("stable")
    val currentChannel: StateFlow<String> = _currentChannel.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultChannel = if (BuildConfig.IS_ALPHA) "alpha" else "stable"
        _currentChannel.value = prefs.getString(KEY_CHANNEL, defaultChannel) ?: defaultChannel
        _lastCheckedTimestamp.value = prefs.getLong(KEY_LAST_CHECK, 0L)
    }

    fun setUpdateChannel(context: Context, channel: String) {
        _currentChannel.value = channel
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHANNEL, channel)
            .apply()
        AppLogger.i(LogCategory.SYSTEM, TAG, "Update channel changed to: $channel")
    }

    fun getLocalVersionCode(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    fun getLocalVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    fun dismissCurrentUpdate() {
        _updateResult.value = UpdateCheckResult.Idle
    }

    fun checkForUpdates(
        context: Context,
        isUserInitiated: Boolean = false,
        onComplete: ((UpdateCheckResult) -> Unit)? = null
    ) {
        _updateResult.value = UpdateCheckResult.Checking
        CoroutineScope(Dispatchers.IO).launch {
            val result = performUpdateEvaluation(context, isUserInitiated)
            _updateResult.value = result
            
            val now = System.currentTimeMillis()
            _lastCheckedTimestamp.value = now
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_CHECK, now)
                .apply()

            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }

    private suspend fun performUpdateEvaluation(
        context: Context,
        isUserInitiated: Boolean
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val manifestUrl = prefs.getString(KEY_CUSTOM_MANIFEST_URL, DEFAULT_MANIFEST_URL) ?: DEFAULT_MANIFEST_URL
        val channel = _currentChannel.value
        val localCode = getLocalVersionCode(context)
        val localName = getLocalVersionName(context)

        AppLogger.i(LogCategory.NETWORK, TAG, "Checking updates on channel '$channel' (Local: $localName / Code: $localCode)")

        val manifest = fetchManifest(context, manifestUrl)
            ?: fetchManifest(context, FALLBACK_MANIFEST_URL)

        if (manifest == null) {
            val errorMsg = "Unable to fetch update manifest from server. Please check internet connection."
            AppLogger.w(LogCategory.NETWORK, TAG, "Update check failed: $errorMsg")
            return@withContext UpdateCheckResult.Error(errorMsg)
        }

        // Decision Step 1: Global Emergency Kill-Switch
        if (manifest.killSwitch.global.enabled) {
            val ks = manifest.killSwitch.global
            AppLogger.w(LogCategory.SYSTEM, TAG, "GLOBAL KILL SWITCH TRIGGERED: ${ks.title}")
            return@withContext UpdateCheckResult.KillSwitchTriggered(
                title = ks.title.ifBlank { "Service Temporarily Suspended" },
                message = ks.message.ifBlank { "App operations are temporarily paused for maintenance." },
                isGlobal = true,
                channel = channel
            )
        }

        // Decision Step 2: Channel-Scoped Emergency Kill-Switch
        val channelKs = when (channel.lowercase()) {
            "alpha" -> manifest.killSwitch.alpha
            "beta" -> manifest.killSwitch.beta
            else -> manifest.killSwitch.stable
        }
        if (channelKs.enabled) {
            AppLogger.w(LogCategory.SYSTEM, TAG, "CHANNEL ($channel) KILL SWITCH TRIGGERED: ${channelKs.title}")
            return@withContext UpdateCheckResult.KillSwitchTriggered(
                title = channelKs.title.ifBlank { "${channel.replaceFirstChar { it.uppercase() }} Track Suspended" },
                message = channelKs.message.ifBlank { "The $channel channel is temporarily locked for maintenance." },
                isGlobal = false,
                channel = channel
            )
        }

        // Decision Step 3: Resolve Track Channel Configuration
        val channelConfig = when (channel.lowercase()) {
            "alpha" -> manifest.alpha ?: manifest.stable
            "beta" -> manifest.beta ?: manifest.stable
            else -> manifest.stable
        }

        if (channelConfig == null) {
            return@withContext UpdateCheckResult.UpToDate(localCode, localName, channel)
        }

        // Decision Step 4: Minimum Supported Build Floor Test (Mandatory Compulsory Upgrade)
        if (localCode < channelConfig.minSupportedVersionCode) {
            AppLogger.w(
                LogCategory.SYSTEM,
                TAG,
                "FLOOR VIOLATION: Local ($localCode) < MinSupported (${channelConfig.minSupportedVersionCode})"
            )
            return@withContext UpdateCheckResult.UpdateAvailable(
                config = channelConfig,
                isFloorEnforced = true,
                updateType = UpdateType.CRITICAL,
                currentCode = localCode,
                currentName = localName,
                channel = channel
            )
        }

        // Decision Step 5: Version Progression Comparison
        if (channelConfig.versionCode > localCode) {
            AppLogger.i(
                LogCategory.NETWORK,
                TAG,
                "UPDATE AVAILABLE: Local ($localCode) < Remote (${channelConfig.versionCode}) [${channelConfig.updateType}]"
            )
            return@withContext UpdateCheckResult.UpdateAvailable(
                config = channelConfig,
                isFloorEnforced = false,
                updateType = channelConfig.updateType,
                currentCode = localCode,
                currentName = localName,
                channel = channel
            )
        }

        // Decision Step 6: Up to Date
        AppLogger.i(LogCategory.NETWORK, TAG, "Application is fully up to date on channel: $channel")
        UpdateCheckResult.UpToDate(localCode, localName, channel)
    }

    private fun fetchManifest(context: Context, urlString: String): UpdateManifest? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "ScreenHarmonyFlex-Updater")
            }

            if (connection.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.readText()
                reader.close()
                val jsonObject = JSONObject(jsonString)
                UpdateManifest.fromJson(jsonObject)
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(LogCategory.NETWORK, TAG, "Failed fetching from $urlString: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun openGitHubRelease(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.e(LogCategory.SYSTEM, TAG, "Failed opening GitHub Release: ${e.message}", e)
        }
    }

    fun openObtainium(context: Context, obtainiumUri: String, fallbackUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(obtainiumUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.w(LogCategory.SYSTEM, TAG, "Obtainium app not found, falling back to browser: ${e.message}")
            openGitHubRelease(context, fallbackUrl)
        }
    }
}
