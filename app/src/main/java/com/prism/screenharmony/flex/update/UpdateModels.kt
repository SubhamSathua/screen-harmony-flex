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
    val versionCode: Long,
    val versionName: String,
    val minSupportedVersionCode: Long,
    val releaseDate: String,
    val updateType: UpdateType,
    val deprecationMessage: String? = null,
    val changelog: List<String> = emptyList(),
    val downloads: DownloadsConfig = DownloadsConfig(),
    val sha256: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject?): ChannelConfig? {
            if (json == null) return null
            val versionCode = json.optLong("versionCode", -1L)
            if (versionCode < 0) return null

            val changelogArray = json.optJSONArray("changelog")
            val changelogList = mutableListOf<String>()
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    val item = changelogArray.optString(i, "")
                    if (item.isNotBlank()) changelogList.add(item)
                }
            }

            return ChannelConfig(
                versionCode = versionCode,
                versionName = json.optString("versionName", "$versionCode"),
                minSupportedVersionCode = json.optLong("minSupportedVersionCode", 0L),
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
    val beta: KillSwitchItem = KillSwitchItem(),
    val stable: KillSwitchItem = KillSwitchItem()
) {
    companion object {
        fun fromJson(json: JSONObject?): KillSwitchConfig {
            if (json == null) return KillSwitchConfig()
            return KillSwitchConfig(
                global = KillSwitchItem.fromJson(json.optJSONObject("global")),
                alpha = KillSwitchItem.fromJson(json.optJSONObject("alpha")),
                beta = KillSwitchItem.fromJson(json.optJSONObject("beta")),
                stable = KillSwitchItem.fromJson(json.optJSONObject("stable") ?: json.optJSONObject("prod"))
            )
        }
    }
}

data class UpdateManifest(
    val killSwitch: KillSwitchConfig = KillSwitchConfig(),
    val stable: ChannelConfig? = null,
    val beta: ChannelConfig? = null,
    val alpha: ChannelConfig? = null
) {
    companion object {
        fun fromJson(json: JSONObject): UpdateManifest {
            return UpdateManifest(
                killSwitch = KillSwitchConfig.fromJson(json.optJSONObject("killSwitch")),
                stable = ChannelConfig.fromJson(json.optJSONObject("stable") ?: json.optJSONObject("prod")),
                beta = ChannelConfig.fromJson(json.optJSONObject("beta")),
                alpha = ChannelConfig.fromJson(json.optJSONObject("alpha"))
            )
        }
    }
}

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()

    data class UpToDate(
        val currentCode: Long,
        val currentName: String,
        val channel: String
    ) : UpdateCheckResult()

    data class UpdateAvailable(
        val config: ChannelConfig,
        val isFloorEnforced: Boolean,
        val updateType: UpdateType,
        val currentCode: Long,
        val currentName: String,
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
