package com.prism.screenharmony.flex.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

enum class LockTimeout(val label: String, val millis: Long) {
    IMMEDIATELY("Immediately", 0L),
    MIN_1("1 min", 60_000L),
    MIN_2("2 min", 120_000L),
    MIN_5("5 min", 300_000L),
    MIN_10("10 min", 600_000L),
    MIN_30("30 min", 1_800_000L),
    HOUR_1("1 hr", 3_600_000L)
}

object AppLockManager {
    private const val PREFS_NAME = "screenharmony_app_lock_secure_prefs"
    private const val KEY_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_PIN_HINT = "pin_hint"
    private const val KEY_HAS_HINT = "has_hint"
    private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
    private const val KEY_TIMEOUT = "lock_timeout"

    // Recovery config keys
    private const val KEY_REC_SEED_ENABLED = "rec_seed_enabled"
    private const val KEY_REC_SEED_PHRASE = "rec_seed_phrase"
    private const val KEY_REC_BIO_ENABLED = "rec_bio_enabled"
    private const val KEY_REC_QUESTION_ENABLED = "rec_question_enabled"
    private const val KEY_REC_QUESTION = "rec_question"
    private const val KEY_REC_ANSWER_HASH = "rec_answer_hash"
    private const val KEY_REC_ANSWER_SALT = "rec_answer_salt"

    private lateinit var prefs: SharedPreferences

    // In-memory session lock state
    private var isSessionUnlocked: Boolean = false
    private var backgroundTimestamp: Long = 0L

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
    }

    var isAppLockEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_LOCK_ENABLED, false) else false
        private set(value) {
            prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()
        }

    var isBiometricsEnabled: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false) else false
        set(value) {
            prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()
        }

    var lockTimeout: LockTimeout
        get() {
            val name = if (::prefs.isInitialized) prefs.getString(KEY_TIMEOUT, LockTimeout.IMMEDIATELY.name) else LockTimeout.IMMEDIATELY.name
            return try {
                LockTimeout.valueOf(name ?: LockTimeout.IMMEDIATELY.name)
            } catch (e: Exception) {
                LockTimeout.IMMEDIATELY
            }
        }
        set(value) {
            prefs.edit().putString(KEY_TIMEOUT, value.name).apply()
        }

    val pinHint: String?
        get() = if (::prefs.isInitialized && prefs.getBoolean(KEY_HAS_HINT, false)) {
            prefs.getString(KEY_PIN_HINT, null)
        } else null

    val hasHint: Boolean
        get() = if (::prefs.isInitialized) prefs.getBoolean(KEY_HAS_HINT, false) else false

    fun savePin(pin: String, hint: String?) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltBase64)
            .putBoolean(KEY_HAS_HINT, !hint.isNullOrBlank())
            .putString(KEY_PIN_HINT, hint?.trim())
            .apply()

        isSessionUnlocked = true
    }

    fun verifyPin(inputPin: String): Boolean {
        if (!isAppLockEnabled) return true
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val inputHash = hashPin(inputPin, salt)
        val isCorrect = inputHash == storedHash
        if (isCorrect) {
            isSessionUnlocked = true
        }
        return isCorrect
    }

    fun saveRecoveryConfig(
        isSeedEnabled: Boolean,
        seedPhrase: String?,
        isBioEnabled: Boolean,
        isQuestionEnabled: Boolean,
        question: String?,
        answer: String?
    ) {
        val editor = prefs.edit()
            .putBoolean(KEY_REC_SEED_ENABLED, isSeedEnabled)
            .putBoolean(KEY_REC_BIO_ENABLED, isBioEnabled)
            .putBoolean(KEY_REC_QUESTION_ENABLED, isQuestionEnabled)

        if (isSeedEnabled && !seedPhrase.isNullOrBlank()) {
            editor.putString(KEY_REC_SEED_PHRASE, seedPhrase.trim())
        } else {
            editor.remove(KEY_REC_SEED_PHRASE)
        }

        if (isQuestionEnabled && !question.isNullOrBlank() && !answer.isNullOrBlank()) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val normalizedAnswer = answer.trim().lowercase()
            val answerHash = hashPin(normalizedAnswer, salt)

            editor.putString(KEY_REC_QUESTION, question)
            editor.putString(KEY_REC_ANSWER_HASH, answerHash)
            editor.putString(KEY_REC_ANSWER_SALT, saltBase64)
        } else {
            editor.remove(KEY_REC_QUESTION)
            editor.remove(KEY_REC_ANSWER_HASH)
            editor.remove(KEY_REC_ANSWER_SALT)
        }

        editor.apply()
    }

    fun getRecoveryConfig(): RecoveryConfig {
        if (!::prefs.isInitialized) return RecoveryConfig()
        return RecoveryConfig(
            isSeedPhraseEnabled = prefs.getBoolean(KEY_REC_SEED_ENABLED, false),
            seedPhrase = prefs.getString(KEY_REC_SEED_PHRASE, null),
            isBiometricsRecoveryEnabled = prefs.getBoolean(KEY_REC_BIO_ENABLED, false),
            isSecurityQuestionEnabled = prefs.getBoolean(KEY_REC_QUESTION_ENABLED, false),
            securityQuestion = prefs.getString(KEY_REC_QUESTION, null),
            securityAnswerHash = prefs.getString(KEY_REC_ANSWER_HASH, null),
            securityAnswerSalt = prefs.getString(KEY_REC_ANSWER_SALT, null)
        )
    }

    fun verifySeedPhrase(inputPhrase: String): Boolean {
        val stored = prefs.getString(KEY_REC_SEED_PHRASE, null) ?: return false
        val normalizedInput = inputPhrase.trim().lowercase().replace(Regex("\\s+"), " ")
        val normalizedStored = stored.trim().lowercase().replace(Regex("\\s+"), " ")
        return normalizedInput == normalizedStored
    }

    fun verifySecurityAnswer(inputAnswer: String): Boolean {
        val storedHash = prefs.getString(KEY_REC_ANSWER_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_REC_ANSWER_SALT, null) ?: return false

        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val normalizedInput = inputAnswer.trim().lowercase()
        val inputHash = hashPin(normalizedInput, salt)
        return inputHash == storedHash
    }

    fun resetPin(newPin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(newPin, salt)

        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, saltBase64)
            .apply()

        isSessionUnlocked = true
    }

    fun disableAppLock() {
        prefs.edit()
            .putBoolean(KEY_LOCK_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HINT)
            .putBoolean(KEY_HAS_HINT, false)
            .putBoolean(KEY_BIOMETRICS_ENABLED, false)
            .remove(KEY_REC_SEED_ENABLED)
            .remove(KEY_REC_SEED_PHRASE)
            .remove(KEY_REC_BIO_ENABLED)
            .remove(KEY_REC_QUESTION_ENABLED)
            .remove(KEY_REC_QUESTION)
            .remove(KEY_REC_ANSWER_HASH)
            .remove(KEY_REC_ANSWER_SALT)
            .apply()

        isSessionUnlocked = true
    }

    fun isAppLocked(): Boolean {
        if (!isAppLockEnabled) return false
        return !isSessionUnlocked
    }

    fun unlockSession() {
        isSessionUnlocked = true
    }

    fun onAppBackgrounded() {
        if (isAppLockEnabled) {
            backgroundTimestamp = SystemClock.elapsedRealtime()
        }
    }

    fun onAppForegrounded() {
        if (!isAppLockEnabled) return
        val elapsed = SystemClock.elapsedRealtime() - backgroundTimestamp
        if (backgroundTimestamp != 0L && elapsed >= lockTimeout.millis) {
            isSessionUnlocked = false
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}
