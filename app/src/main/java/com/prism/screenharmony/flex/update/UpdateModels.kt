package com.prism.screenharmony.flex.update

import org.json.JSONObject

enum class UpdateType {
    OPTIONAL,
    RECOMMENDED,
    CRITICAL;

    companion object {
        fun fromString(value: String?): UpdateType = when (value?.trim()?.uppercase()) {
            "CRITICAL" -> CRITICAL
            "RECOMMENDED" -> RECOMMENDED
            else -> OPTIONAL
        }
    }
}

object VersionHelper {
    /**
     * Robust semantic version comparator (e.g. "2.9.0", "2.9.00", "2.10.1", "2.8.5-alpha", "v2.9.0").
     * Returns:
     *   > 0 if v1 > v2
     *   0 if v1 == v2
     *   < 0 if v1 < v2
     */
    fun compare(v1: String?, v2: String?): Int {
        if (v1.isNullOrBlank() && v2.isNullOrBlank()) return 0
        if (v1.isNullOrBlank()) return -1
        if (v2.isNullOrBlank()) return 1

        val clean1 = v1.trim().removePrefix("v").removePrefix("V")
        val clean2 = v2.trim().removePrefix("v").removePrefix("V")

        val parts1 = clean1.split("-", limit = 2)
        val parts2 = clean2.split("-", limit = 2)

        val nums1 = parts1[0].split(".").mapNotNull { it.trim().toIntOrNull() }
        val nums2 = parts2[0].split(".").mapNotNull { it.trim().toIntOrNull() }

        val maxLen = maxOf(nums1.size, nums2.size)
        for (i in 0 until maxLen) {
            val n1 = nums1.getOrElse(i) { 0 }
            val n2 = nums2.getOrElse(i) { 0 }
            if (n1 != n2) {
                return n1.compareTo(n2)
            }
        }

        val hasSuffix1 = parts1.size > 1
        val hasSuffix2 = parts2.size > 1
        if (!hasSuffix1 && hasSuffix2) return 1
        if (hasSuffix1 && !hasSuffix2) return -1
        if (hasSuffix1 && hasSuffix2) {
            return parts1[1].compareTo(parts2[1])
        }

        return 0
    }
}

data class DownloadsConfig(
    val directApk: String? = null,
    val github: String? = null,
    val obtainium: String? = null,
    val fdroid: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject?): DownloadsConfig {
            if (json == null) return DownloadsConfig()
            return DownloadsConfig(
                directApk = json.optString("directApk", "").takeIf { it.isNotBlank() },
                github = json.optString("github", "").takeIf { it.isNotBlank() },
                obtainium = json.optString("obtainium", "").takeIf { it.isNotBlank() },
                fdroid = json.optString("fdroid", "").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class ChannelConfig(
    val version: String,
    val minVersion: String = "0.0.0",
    val releaseDate: String = "",
    val updateType: UpdateType = UpdateType.OPTIONAL,
    val deprecationMessage: String? = null,
    val changelog: List<String> = emptyList(),
    val downloads: DownloadsConfig = DownloadsConfig(),
    val sha256: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject?): ChannelConfig? {
            if (json == null) return null
            val versionStr = json.optString("version", json.optString("versionName", "")).trim()
            if (versionStr.isBlank()) return null

            val minVersionStr = json.optString("minVersion", json.optString("minSupportedVersion", json.optString("min", "0.0.0"))).trim()

            val changelogArray = json.optJSONArray("changelog")
            val changelogList = mutableListOf<String>()
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    val item = changelogArray.optString(i, "")
                    if (item.isNotBlank()) changelogList.add(item)
                }
            }

            return ChannelConfig(
                version = versionStr,
                minVersion = if (minVersionStr.isNotBlank()) minVersionStr else "0.0.0",
                releaseDate = json.optString("releaseDate", ""),
                updateType = UpdateType.fromString(json.optString("updateType", "OPTIONAL")),
                deprecationMessage = json.optString("deprecationMessage", "").takeIf { it.isNotBlank() },
                changelog = changelogList,
                downloads = DownloadsConfig.fromJson(json.optJSONObject("downloads")),
                sha256 = json.optString("sha256", "").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class KillSwitchItem(
    val enabled: Boolean = false,
    val title: String = "",
    val message: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject?): KillSwitchItem {
            if (json == null) return KillSwitchItem()
            return KillSwitchItem(
                enabled = json.optBoolean("enabled", false),
                title = json.optString("title", "Service Temporarily Suspended"),
                message = json.optString("message", "This service is temporarily paused for maintenance.")
            )
        }
    }
}

data class KillSwitchConfig(
    val global: KillSwitchItem = KillSwitchItem(),
    val alpha: KillSwitchItem = KillSwitchItem(),
    val stable: KillSwitchItem = KillSwitchItem()
) {
    companion object {
        fun fromJson(json: JSONObject?): KillSwitchConfig {
            if (json == null) return KillSwitchConfig()
            return KillSwitchConfig(
                global = KillSwitchItem.fromJson(json.optJSONObject("global")),
                alpha = KillSwitchItem.fromJson(json.optJSONObject("alpha")),
                stable = KillSwitchItem.fromJson(json.optJSONObject("stable") ?: json.optJSONObject("prod"))
            )
        }
    }
}

data class UpdateManifest(
    val killSwitch: KillSwitchConfig = KillSwitchConfig(),
    val stable: ChannelConfig? = null,
    val alpha: ChannelConfig? = null
) {
    companion object {
        fun fromJson(json: JSONObject): UpdateManifest {
            return UpdateManifest(
                killSwitch = KillSwitchConfig.fromJson(json.optJSONObject("killSwitch")),
                stable = ChannelConfig.fromJson(json.optJSONObject("stable") ?: json.optJSONObject("prod")),
                alpha = ChannelConfig.fromJson(json.optJSONObject("alpha"))
            )
        }
    }
}

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()

    data class UpToDate(
        val currentVersion: String,
        val channel: String
    ) : UpdateCheckResult()

    data class UpdateAvailable(
        val config: ChannelConfig,
        val isFloorEnforced: Boolean,
        val updateType: UpdateType,
        val currentVersion: String,
        val channel: String
    ) : UpdateCheckResult()

    data class KillSwitchTriggered(
        val title: String,
        val message: String,
        val isGlobal: Boolean,
        val channel: String
    ) : UpdateCheckResult()

    data class Error(
        val message: String
    ) : UpdateCheckResult()
}
