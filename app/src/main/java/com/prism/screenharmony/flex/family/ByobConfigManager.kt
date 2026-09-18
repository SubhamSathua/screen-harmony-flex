package com.prism.screenharmony.flex.family

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class ByobConfig(
    val projectId: String = "",
    val databaseUrl: String = "",
    val apiKey: String = "",
    val isEnabled: Boolean = false
) {
    val isConfigured: Boolean
        get() = isEnabled && databaseUrl.isNotBlank() && projectId.isNotBlank()
}

object ByobConfigManager {

    private const val TAG = "ScreenHarmony_BYOB"
    private const val PREFS_NAME = "screenharmony_byob_prefs"
    private const val KEY_PROJECT_ID = "byob_project_id"
    private const val KEY_DATABASE_URL = "byob_database_url"
    private const val KEY_API_KEY = "byob_api_key"
    private const val KEY_ENABLED = "byob_enabled"

    private val _configFlow = MutableStateFlow(ByobConfig())
    val configFlow: StateFlow<ByobConfig> = _configFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        val config = ByobConfig(
            projectId = prefs.getString(KEY_PROJECT_ID, "") ?: "",
            databaseUrl = prefs.getString(KEY_DATABASE_URL, "") ?: "",
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        )
        _configFlow.value = config
    }

    fun saveConfig(context: Context, projectId: String, databaseUrl: String, apiKey: String, isEnabled: Boolean = true) {
        val normalizedUrl = databaseUrl.trim().removeSuffix("/")
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_PROJECT_ID, projectId.trim())
            .putString(KEY_DATABASE_URL, normalizedUrl)
            .putString(KEY_API_KEY, apiKey.trim())
            .putBoolean(KEY_ENABLED, isEnabled)
            .apply()

        _configFlow.value = ByobConfig(
            projectId = projectId.trim(),
            databaseUrl = normalizedUrl,
            apiKey = apiKey.trim(),
            isEnabled = isEnabled
        )
        Log.i(TAG, "BYOB configuration saved. Enabled=$isEnabled | Project=$projectId | DB=$normalizedUrl")
    }

    fun disableByob(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        _configFlow.value = _configFlow.value.copy(isEnabled = false)
        Log.i(TAG, "BYOB disabled. Reverted to default ScreenHarmony Cloud.")
    }

    fun parseConfigJson(rawJson: String): Result<ByobConfig> {
        return try {
            val obj = JSONObject(rawJson.trim())
            val projectId = obj.optString("projectId", obj.optString("project_id", ""))
            val databaseUrl = obj.optString("databaseUrl", obj.optString("database_url", obj.optString("firebase_url", "")))
            val apiKey = obj.optString("apiKey", obj.optString("api_key", ""))

            if (projectId.isBlank() && databaseUrl.isBlank()) {
                // Try checking google-services format
                val projectInfo = obj.optJSONObject("project_info")
                val pId = projectInfo?.optString("project_id", "") ?: ""
                val dbUrl = projectInfo?.optString("firebase_url", "") ?: ""
                val client = obj.optJSONArray("client")?.optJSONObject(0)
                val key = client?.optJSONArray("api_key")?.optJSONObject(0)?.optString("current_key", "") ?: ""

                if (pId.isNotBlank() && (dbUrl.isNotBlank() || pId.isNotBlank())) {
                    val finalDbUrl = if (dbUrl.isNotBlank()) dbUrl else "https://$pId-default-rtdb.firebaseio.com"
                    return Result.success(ByobConfig(projectId = pId, databaseUrl = finalDbUrl, apiKey = key, isEnabled = true))
                }
                return Result.failure(IllegalArgumentException("Missing projectId or databaseUrl in JSON"))
            }

            val finalDbUrl = if (databaseUrl.isNotBlank()) databaseUrl else "https://$projectId-default-rtdb.firebaseio.com"
            Result.success(ByobConfig(projectId = projectId, databaseUrl = finalDbUrl, apiKey = apiKey, isEnabled = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportConfigJson(config: ByobConfig): String {
        val obj = JSONObject()
        obj.put("projectId", config.projectId)
        obj.put("databaseUrl", config.databaseUrl)
        obj.put("apiKey", config.apiKey)
        return obj.toString(2)
    }
}
