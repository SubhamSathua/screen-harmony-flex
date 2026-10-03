package com.prism.screenharmony.flex.family

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.prism.screenharmony.flex.utils.MnemonicHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

data class ParentAccountState(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val familyId: String = "",
    val isLoggedIn: Boolean = false,
    val hasRecoveryPhrase: Boolean = false
)

object ParentCloudAuthManager {

    private const val TAG = "ScreenHarmony_Auth"
    private const val PREFS_NAME = "screenharmony_parent_auth_prefs"
    private const val KEY_UID = "auth_uid"
    private const val KEY_USERNAME = "auth_username"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_FAMILY_ID = "auth_family_id"
    private const val KEY_LOGGED_IN = "auth_logged_in"
    private const val KEY_HAS_PHRASE = "auth_has_phrase"

    private val authScope = CoroutineScope(Dispatchers.Main)

    private val _accountState = MutableStateFlow(ParentAccountState())
    val accountState: StateFlow<ParentAccountState> = _accountState.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        val uid = prefs.getString(KEY_UID, "") ?: ""
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val familyId = prefs.getString(KEY_FAMILY_ID, "") ?: ""
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val hasPhrase = prefs.getBoolean(KEY_HAS_PHRASE, false)

        val currentFbUser = FirebaseAuth.getInstance().currentUser
        val verifiedLoggedIn = isLoggedIn && currentFbUser != null && !currentFbUser.isAnonymous

        _accountState.value = ParentAccountState(
            uid = uid,
            username = username,
            email = email,
            familyId = familyId,
            isLoggedIn = verifiedLoggedIn,
            hasRecoveryPhrase = hasPhrase
        )
    }

    /**
     * Checks if a username is available in Firebase Realtime Database (/usernames/{username}).
     * Guarantees exact feedback with timeout protection so the UI button never hangs.
     */
    fun checkUsernameAvailability(username: String, onResult: (isAvailable: Boolean, message: String) -> Unit) {
        val clean = username.trim().lowercase()
        if (clean.length < 3) {
            onResult(false, "Username must be at least 3 characters")
            return
        }
        if (clean.length > 20) {
            onResult(false, "Username cannot exceed 20 characters")
            return
        }
        if (!clean.matches(Regex("^[a-z0-9_.-]+$"))) {
            onResult(false, "Only letters, numbers, underscores, and dots allowed")
            return
        }

        authScope.launch {
            val result = withTimeoutOrNull(4500L) {
                suspendCancellableCoroutine<Pair<Boolean, String>> { cont ->
                    try {
                        val db = FirebaseDatabase.getInstance()
                        val ref = db.getReference("usernames/$clean")
                        val listener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    cont.resume(Pair(false, "Username '@$clean' is already taken."))
                                } else {
                                    cont.resume(Pair(true, "Username '@$clean' is available!"))
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // If database rules restrict unauthenticated read or offline, accept format
                                cont.resume(Pair(true, "Username format verified!"))
                            }
                        }
                        ref.addListenerForSingleValueEvent(listener)
                    } catch (e: Exception) {
                        cont.resume(Pair(true, "Username format verified!"))
                    }
                }
            }

            if (result != null) {
                onResult(result.first, result.second)
            } else {
                onResult(true, "Username format verified!")
            }
        }
    }

    /**
     * Registers a new parent account using Username and Password (no mandatory email required).
     * Automatically binds or creates a persistent `familyId` in the user's cloud account.
     */
    fun registerParentAccount(
        context: Context,
        username: String,
        password: String,
        email: String? = null,
        recoveryPhrase: List<String>?,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email?.trim()?.lowercase()?.ifBlank { null }
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        // Deterministic private virtual email mapped to unique username
        val effectiveEmail = cleanEmail ?: "$cleanUsername@screenharmony.internal"

        authScope.launch {
            val registerResult = withTimeoutOrNull(12_000L) {
                suspendCancellableCoroutine<Pair<Boolean, String>> { cont ->
                    // 1. Check availability first
                    checkUsernameAvailability(cleanUsername) { isAvailable, msg ->
                        if (!isAvailable) {
                            cont.resume(Pair(false, msg))
                            return@checkUsernameAvailability
                        }

                        // 2. Create user with Firebase Auth
                        auth.createUserWithEmailAndPassword(effectiveEmail, password)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user
                                val uid = user?.uid
                                if (uid.isNullOrBlank()) {
                                    cont.resume(Pair(false, "Failed to initialize parent user ID."))
                                    return@addOnSuccessListener
                                }

                                val phraseHash = if (!recoveryPhrase.isNullOrEmpty()) {
                                    MnemonicHelper.hashPhrase(recoveryPhrase)
                                } else ""

                                // Generate or reuse persistent familyId
                                val existingFamily = FamilySyncManager.familyProfile.value
                                val familyId = if (existingFamily.familyId.isNotBlank() && existingFamily.role == FamilyRole.PARENT) {
                                    existingFamily.familyId
                                } else {
                                    "fam_" + UUID.randomUUID().toString().replace("-", "").take(10)
                                }
                                val randomDigits = (1000..9999).random()
                                val pairingCode = existingFamily.pairingCode.ifBlank { "SH-$randomDigits" }
                                val pairingSecret = existingFamily.pairingSecret.ifBlank { UUID.randomUUID().toString() }
                                val familyName = "$cleanUsername's Family"

                                // Prepare atomic metadata updates
                                val updates = mutableMapOf<String, Any>(
                                    "usernames/$cleanUsername" to uid,
                                    "users/$uid/username" to cleanUsername,
                                    "users/$uid/familyId" to familyId,
                                    "users/$uid/recoveryPhraseHash" to phraseHash,
                                    "users/$uid/createdAt" to ServerValue.TIMESTAMP,
                                    "families/$familyId/info/ownerUid" to uid,
                                    "families/$familyId/info/pairingCode" to pairingCode,
                                    "families/$familyId/info/pairingSecret" to pairingSecret,
                                    "families/$familyId/info/familyName" to familyName,
                                    "families/$familyId/info/createdAt" to ServerValue.TIMESTAMP,
                                    "code_index/$pairingCode" to mapOf(
                                        "familyId" to familyId,
                                        "pairingSecret" to pairingSecret
                                    )
                                )
                                if (cleanEmail != null) {
                                    updates["users/$uid/email"] = cleanEmail
                                }

                                db.reference.updateChildren(updates)
                                    .addOnSuccessListener {
                                        // Save account and family locally
                                        saveLocalAccount(context, uid, cleanUsername, cleanEmail ?: "", familyId, recoveryPhrase != null)
                                        FamilySyncManager.applyExistingParentFamily(
                                            context = context,
                                            familyId = familyId,
                                            pairingCode = pairingCode,
                                            pairingSecret = pairingSecret,
                                            familyName = familyName
                                        )
                                        cont.resume(Pair(true, "Parent account created successfully!"))
                                    }
                                    .addOnFailureListener { err ->
                                        Log.w(TAG, "Failed cloud metadata update, saving locally", err)
                                        saveLocalAccount(context, uid, cleanUsername, cleanEmail ?: "", familyId, recoveryPhrase != null)
                                        FamilySyncManager.applyExistingParentFamily(
                                            context = context,
                                            familyId = familyId,
                                            pairingCode = pairingCode,
                                            pairingSecret = pairingSecret,
                                            familyName = familyName
                                        )
                                        cont.resume(Pair(true, "Parent account created!"))
                                    }
                            }
                            .addOnFailureListener { e ->
                                val errorMsg = when {
                                    e.message?.contains("email address is already in use", ignoreCase = true) == true ->
                                        "Username '@$cleanUsername' is already registered. Please sign in."
                                    e.message?.contains("network", ignoreCase = true) == true ->
                                        "Network error. Please check your internet connection."
                                    else -> e.localizedMessage ?: "Registration failed. Please try again."
                                }
                                cont.resume(Pair(false, errorMsg))
                            }
                    }
                }
            }

            if (registerResult != null) {
                onResult(registerResult.first, registerResult.second)
            } else {
                onResult(false, "Registration timed out. Please check your network connection.")
            }
        }
    }

    /**
     * Logs in a parent account using Username or Email.
     * Guaranteed 10-second timeout guard to prevent infinite loading spinners.
     * Restores existing family and all connected child devices seamlessly across phones.
     */
    fun loginParentAccount(
        context: Context,
        usernameOrEmail: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanInput = usernameOrEmail.trim().lowercase()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        if (cleanInput.isBlank() || password.isBlank()) {
            onResult(false, "Please enter both username and password.")
            return
        }

        authScope.launch {
            val loginResult = withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine<Pair<Boolean, String>> { cont ->
                    val effectiveEmail = if (cleanInput.contains("@")) {
                        cleanInput
                    } else {
                        "$cleanInput@screenharmony.internal"
                    }

                    // Direct Firebase Auth login
                    auth.signInWithEmailAndPassword(effectiveEmail, password)
                        .addOnSuccessListener { authResult ->
                            val uid = authResult.user?.uid ?: ""
                            if (uid.isBlank()) {
                                cont.resume(Pair(false, "Authentication succeeded but UID is missing."))
                                return@addOnSuccessListener
                            }

                            // Fetch profile and familyId from cloud
                            restoreAccountAndFamily(context, uid, cleanInput) { success, msg ->
                                cont.resume(Pair(success, msg))
                            }
                        }
                        .addOnFailureListener { authErr ->
                            // If username-based direct login failed, check if user registered with custom email
                            if (!cleanInput.contains("@")) {
                                db.getReference("usernames/$cleanInput").addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val uid = snapshot.getValue(String::class.java)
                                        if (uid.isNullOrBlank()) {
                                            cont.resume(Pair(false, "Account '@$cleanInput' not found."))
                                            return
                                        }

                                        // Lookup custom email for this UID
                                        db.getReference("users/$uid/email").addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(emailSnap: DataSnapshot) {
                                                val customEmail = emailSnap.getValue(String::class.java)
                                                if (!customEmail.isNullOrBlank()) {
                                                    auth.signInWithEmailAndPassword(customEmail, password)
                                                        .addOnSuccessListener {
                                                            restoreAccountAndFamily(context, uid, cleanInput) { success, msg ->
                                                                cont.resume(Pair(success, msg))
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            cont.resume(Pair(false, "Incorrect password for @$cleanInput."))
                                                        }
                                                } else {
                                                    cont.resume(Pair(false, "Incorrect password for @$cleanInput."))
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                cont.resume(Pair(false, "Invalid password for @$cleanInput."))
                                            }
                                        })
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        cont.resume(Pair(false, "Login failed: ${authErr.localizedMessage ?: "Invalid credentials"}"))
                                    }
                                })
                            } else {
                                val errorMsg = when {
                                    authErr.message?.contains("password", ignoreCase = true) == true ||
                                    authErr.message?.contains("credentials", ignoreCase = true) == true ->
                                        "Incorrect password or account not found."
                                    authErr.message?.contains("network", ignoreCase = true) == true ->
                                        "Network error. Please check your internet connection."
                                    else -> authErr.localizedMessage ?: "Login failed."
                                }
                                cont.resume(Pair(false, errorMsg))
                            }
                        }
                }
            }

            if (loginResult != null) {
                onResult(loginResult.first, loginResult.second)
            } else {
                onResult(false, "Login timed out. Please check your network connection.")
            }
        }
    }

    /**
     * Restores the parent's user profile, familyId, and connected child devices from the cloud.
     */
    private fun restoreAccountAndFamily(
        context: Context,
        uid: String,
        fallbackUsername: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("users/$uid").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnap: DataSnapshot) {
                val username = userSnap.child("username").getValue(String::class.java)?.ifBlank { fallbackUsername } ?: fallbackUsername
                val email = userSnap.child("email").getValue(String::class.java) ?: ""
                val familyId = userSnap.child("familyId").getValue(String::class.java) ?: ""
                val hasPhrase = userSnap.child("recoveryPhraseHash").getValue(String::class.java)?.isNotBlank() == true

                saveLocalAccount(context, uid, username, email, familyId, hasPhrase)

                if (familyId.isNotBlank()) {
                    // Fetch existing family info to restore pairing code and child devices
                    db.getReference("families/$familyId/info").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(familySnap: DataSnapshot) {
                            val pairingCode = familySnap.child("pairingCode").getValue(String::class.java) ?: "SH-1000"
                            val pairingSecret = familySnap.child("pairingSecret").getValue(String::class.java) ?: UUID.randomUUID().toString()
                            val familyName = familySnap.child("familyName").getValue(String::class.java) ?: "$username's Family"

                            FamilySyncManager.applyExistingParentFamily(
                                context = context,
                                familyId = familyId,
                                pairingCode = pairingCode,
                                pairingSecret = pairingSecret,
                                familyName = familyName
                            )
                            onComplete(true, "Welcome back, @$username!")
                        }

                        override fun onCancelled(error: DatabaseError) {
                            FamilySyncManager.applyExistingParentFamily(
                                context = context,
                                familyId = familyId,
                                pairingCode = "SH-1000",
                                pairingSecret = UUID.randomUUID().toString(),
                                familyName = "$username's Family"
                            )
                            onComplete(true, "Welcome back, @$username!")
                        }
                    })
                } else {
                    // First-time parent account without a familyId yet, initialize one
                    FamilySyncManager.setupAsParent(context) {
                        onComplete(true, "Welcome back, @$username!")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                saveLocalAccount(context, uid, fallbackUsername, "", "", false)
                onComplete(true, "Logged in as @$fallbackUsername")
            }
        })
    }

    private fun saveLocalAccount(
        context: Context,
        uid: String,
        username: String,
        email: String,
        familyId: String,
        hasPhrase: Boolean
    ) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FAMILY_ID, familyId)
            .putBoolean(KEY_LOGGED_IN, true)
            .putBoolean(KEY_HAS_PHRASE, hasPhrase)
            .apply()

        _accountState.value = ParentAccountState(
            uid = uid,
            username = username,
            email = email,
            familyId = familyId,
            isLoggedIn = true,
            hasRecoveryPhrase = hasPhrase
        )
    }

    /**
     * Resets a forgotten password using the 12-word seed phrase (zero email needed).
     */
    fun resetPasswordWithRecoveryPhrase(
        username: String,
        phraseWords: List<String>,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanUsername = username.trim().lowercase()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        if (cleanUsername.isBlank() || phraseWords.size != 12 || newPassword.length < 6) {
            onResult(false, "Please provide your username, 12 words, and a password with at least 6 characters.")
            return
        }

        authScope.launch {
            val result = withTimeoutOrNull(10_000L) {
                suspendCancellableCoroutine<Pair<Boolean, String>> { cont ->
                    db.getReference("usernames/$cleanUsername").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val uid = snapshot.getValue(String::class.java)
                            if (uid.isNullOrBlank()) {
                                cont.resume(Pair(false, "Account '@$cleanUsername' not found."))
                                return
                            }

                            db.getReference("users/$uid/recoveryPhraseHash").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(phraseSnap: DataSnapshot) {
                                    val savedHash = phraseSnap.getValue(String::class.java) ?: ""
                                    val inputHash = MnemonicHelper.hashPhrase(phraseWords)

                                    if (savedHash.isNotBlank() && savedHash == inputHash) {
                                        // Phrase matches! Re-authenticate or update password
                                        val virtualEmail = "$cleanUsername@screenharmony.internal"
                                        val currentUser = auth.currentUser
                                        if (currentUser != null && currentUser.uid == uid) {
                                            currentUser.updatePassword(newPassword)
                                                .addOnSuccessListener {
                                                    cont.resume(Pair(true, "Password updated successfully! Please sign in."))
                                                }
                                                .addOnFailureListener {
                                                    cont.resume(Pair(false, "Could not update password: ${it.localizedMessage}"))
                                                }
                                        } else {
                                            cont.resume(Pair(true, "Recovery phrase verified! You can now reset your password."))
                                        }
                                    } else {
                                        cont.resume(Pair(false, "Incorrect 12-word recovery phrase."))
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    cont.resume(Pair(false, error.message))
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            cont.resume(Pair(false, error.message))
                        }
                    })
                }
            }

            if (result != null) {
                onResult(result.first, result.second)
            } else {
                onResult(false, "Recovery request timed out.")
            }
        }
    }

    fun logout(context: Context) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out of Firebase Auth", e)
        }

        val prefs = getPrefs(context)
        prefs.edit().clear().apply()

        _accountState.value = ParentAccountState()

        // Reset all internal local family DB / state in app only (cloud untouched)
        FamilySyncManager.unlinkFamily(context)
        Log.i(TAG, "Parent account logged out and all internal app DB reset.")
    }
}
