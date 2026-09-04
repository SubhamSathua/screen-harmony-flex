package com.prism.screenharmony.flex.diagnostics

import android.content.Context
import android.content.SharedPreferences
import com.prism.screenharmony.flex.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DiagnosticsUnlockManager {
    private const val PREFS_NAME = "screenharmony_diagnostics_prefs"
    private const val KEY_LOGS_UNLOCKED = "logs_unlocked"

    private val _isUnlockedFlow = MutableStateFlow<Boolean?>(null)
    val isUnlockedFlow: StateFlow<Boolean?> = _isUnlockedFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLogsUnlocked(context: Context): Boolean {
        // In alpha flavor, logs are enabled by default
        if (BuildConfig.LOGS_ENABLED_DEFAULT || BuildConfig.IS_ALPHA) {
            return true
        }
        val saved = getPrefs(context).getBoolean(KEY_LOGS_UNLOCKED, false)
        if (_isUnlockedFlow.value != saved) {
            _isUnlockedFlow.value = saved
        }
        return saved
    }

    fun setLogsUnlocked(context: Context, unlocked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOGS_UNLOCKED, unlocked).apply()
        _isUnlockedFlow.value = unlocked
    }

    fun isAlwaysUnlocked(): Boolean {
        return BuildConfig.LOGS_ENABLED_DEFAULT || BuildConfig.IS_ALPHA
    }
}
