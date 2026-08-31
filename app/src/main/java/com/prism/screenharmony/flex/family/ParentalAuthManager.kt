package com.prism.screenharmony.flex.family

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.prism.screenharmony.flex.data.AppLockManager
import java.security.MessageDigest
import java.security.SecureRandom

enum class UnlinkAuthMode(val label: String, val description: String) {
    APP_PIN("Same as App PIN", "Use your ScreenHarmony security PIN"),
    DEVICE_BIOMETRIC("Device Lock / Biometrics", "Use your phone's fingerprint, face, or lock screen"),
    CUSTOM_PIN("Custom PIN", "Set a dedicated parental unlink PIN"),
    NONE("None", "No authentication required to unlink")
}

object ParentalAuthManager {

    private const val PREFS_NAME = "screenharmony_parental_auth_prefs"
    private const val KEY_AUTH_MODE = "unlink_auth_mode"
    private const val KEY_CUSTOM_PIN_HASH = "unlink_custom_pin_hash"
    private const val KEY_CUSTOM_PIN_SALT = "unlink_custom_pin_salt"
    private const val KEY_ONLY_PARENT_MODE = "key_only_parent_mode"

    private val _onlyParentModeFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    val onlyParentModeFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _onlyParentModeFlow

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        _onlyParentModeFlow.value = prefs.getBoolean(KEY_ONLY_PARENT_MODE, false)
    }

    private fun ensureInit(context: Context) {
        if (!::prefs.isInitialized) {
            initialize(context)
        }
    }

    fun getSelectedAuthMode(context: Context): UnlinkAuthMode {
        ensureInit(context)
        val isAppPinAvailable = AppLockManager.isAppLockEnabled
        val defaultMode = if (isAppPinAvailable) UnlinkAuthMode.APP_PIN.name else UnlinkAuthMode.DEVICE_BIOMETRIC.name
        val savedName = prefs.getString(KEY_AUTH_MODE, defaultMode) ?: defaultMode
        return try {
            val parsed = UnlinkAuthMode.valueOf(savedName)
            if (parsed == UnlinkAuthMode.APP_PIN && !isAppPinAvailable) {
                UnlinkAuthMode.DEVICE_BIOMETRIC
            } else {
                parsed
            }
        } catch (e: Exception) {
            if (isAppPinAvailable) UnlinkAuthMode.APP_PIN else UnlinkAuthMode.DEVICE_BIOMETRIC
        }
    }

    fun setAuthMode(context: Context, mode: UnlinkAuthMode) {
        ensureInit(context)
        prefs.edit().putString(KEY_AUTH_MODE, mode.name).apply()
    }

    fun setCustomPin(context: Context, pin: String) {
        ensureInit(context)
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_CUSTOM_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_CUSTOM_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_AUTH_MODE, UnlinkAuthMode.CUSTOM_PIN.name)
            .apply()
    }

    fun verifyCustomPin(context: Context, pin: String): Boolean {
        ensureInit(context)
        val saltStr = prefs.getString(KEY_CUSTOM_PIN_SALT, null) ?: return false
        val hashStr = prefs.getString(KEY_CUSTOM_PIN_HASH, null) ?: return false

        val salt = Base64.decode(saltStr, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashStr, Base64.NO_WRAP)
        val computedHash = hashPin(pin, salt)

        return MessageDigest.isEqual(expectedHash, computedHash)
    }

    fun hasCustomPin(context: Context): Boolean {
        ensureInit(context)
        return prefs.contains(KEY_CUSTOM_PIN_HASH)
    }

    fun isOnlyParentMode(context: Context): Boolean {
        ensureInit(context)
        return prefs.getBoolean(KEY_ONLY_PARENT_MODE, false)
    }

    fun setOnlyParentMode(context: Context, enabled: Boolean) {
        ensureInit(context)
        prefs.edit().putBoolean(KEY_ONLY_PARENT_MODE, enabled).apply()
        _onlyParentModeFlow.value = enabled
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8))
    }
}
