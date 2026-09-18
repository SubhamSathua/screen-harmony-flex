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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ParentAccountState(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false,
    val hasRecoveryPhrase: Boolean = false
)

object ParentCloudAuthManager {

    private const val TAG = "ScreenHarmony_Auth"
    private const val PREFS_NAME = "screenharmony_parent_auth_prefs"
    private const val KEY_UID = "auth_uid"
    private const val KEY_USERNAME = "auth_username"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_LOGGED_IN = "auth_logged_in"
    private const val KEY_HAS_PHRASE = "auth_has_phrase"

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
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val hasPhrase = prefs.getBoolean(KEY_HAS_PHRASE, false)

        val currentFbUser = FirebaseAuth.getInstance().currentUser
        val verifiedLoggedIn = isLoggedIn && currentFbUser != null && !currentFbUser.isAnonymous

        _accountState.value = ParentAccountState(
            uid = uid,
            username = username,
            email = email,
            isLoggedIn = verifiedLoggedIn,
            hasRecoveryPhrase = hasPhrase
        )
    }

    fun checkUsernameAvailability(username: String, onResult: (isAvailable: Boolean, message: String) -> Unit) {
        val clean = username.trim().lowercase()
        if (clean.length < 3) {
            onResult(false, "Username must be at least 3 characters")
            return
        }
        if (!clean.matches(Regex("^[a-z0-9_.-]+$"))) {
            onResult(false, "Only letters, numbers, underscores, and dots allowed")
            return
        }

        val db = FirebaseDatabase.getInstance()
        db.getReference("usernames/$clean").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    onResult(false, "Username '@$clean' is already taken.")
                } else {
                    onResult(true, "Username '@$clean' is available! ✅")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(false, "Check failed: ${error.message}")
            }
        })
    }

    fun registerParentAccount(
        context: Context,
        username: String,
        password: String,
        email: String?,
        recoveryPhrase: List<String>?,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email?.trim()?.lowercase()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        val effectiveEmail = if (!cleanEmail.isNullOrBlank()) cleanEmail else "$cleanUsername@screenharmony.internal"

        // 1. Verify availability one final time
        checkUsernameAvailability(cleanUsername) { isAvailable, msg ->
            if (!isAvailable) {
                onResult(false, msg)
                return@checkUsernameAvailability
            }

            // 2. Create Firebase Auth user
            auth.createUserWithEmailAndPassword(effectiveEmail, password)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    val uid = user?.uid ?: ""
                    val phraseHash = if (recoveryPhrase != null && recoveryPhrase.isNotEmpty()) {
                        MnemonicHelper.hashPhrase(recoveryPhrase)
                    } else ""

                    // 3. Reserve username and write user profile
                    val updates = mapOf(
                        "usernames/$cleanUsername" to uid,
                        "users/$uid/username" to cleanUsername,
                        "users/$uid/email" to (cleanEmail ?: ""),
                        "users/$uid/recoveryPhraseHash" to phraseHash,
                        "users/$uid/createdAt" to ServerValue.TIMESTAMP
                    )

                    db.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            saveLocalAccount(context, uid, cleanUsername, cleanEmail ?: "", recoveryPhrase != null)
                            onResult(true, "Parent account created successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to write user metadata", e)
                            saveLocalAccount(context, uid, cleanUsername, cleanEmail ?: "", recoveryPhrase != null)
                            onResult(true, "Account created with warnings: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firebase Auth registration error", e)
                    onResult(false, e.localizedMessage ?: "Registration failed")
                }
        }
    }

    fun loginParentAccount(
        context: Context,
        usernameOrEmail: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanInput = usernameOrEmail.trim().lowercase()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        if (cleanInput.contains("@")) {
            // Direct Email login
            auth.signInWithEmailAndPassword(cleanInput, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    fetchAndSaveProfile(context, uid, onResult)
                }
                .addOnFailureListener { e ->
                    onResult(false, e.localizedMessage ?: "Login failed")
                }
        } else {
            // Username lookup
            val virtualEmail = "$cleanInput@screenharmony.internal"
            auth.signInWithEmailAndPassword(virtualEmail, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    saveLocalAccount(context, uid, cleanInput, "", false)
                    onResult(true, "Welcome back, @$cleanInput!")
                }
                .addOnFailureListener {
                    // Fallback check if user registered with custom email
                    db.getReference("usernames/$cleanInput").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val uid = snapshot.getValue(String::class.java)
                            if (uid.isNullOrBlank()) {
                                onResult(false, "Account '@$cleanInput' not found.")
                                return
                            }
                            db.getReference("users/$uid/email").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(emailSnap: DataSnapshot) {
                                    val realEmail = emailSnap.getValue(String::class.java)
                                    if (!realEmail.isNullOrBlank()) {
                                        auth.signInWithEmailAndPassword(realEmail, password)
                                            .addOnSuccessListener {
                                                saveLocalAccount(context, uid, cleanInput, realEmail, false)
                                                onResult(true, "Welcome back, @$cleanInput!")
                                            }
                                            .addOnFailureListener { err ->
                                                onResult(false, err.localizedMessage ?: "Invalid password")
                                            }
                                    } else {
                                        onResult(false, "Incorrect password for @$cleanInput")
                                    }
                                }
                                override fun onCancelled(err: DatabaseError) {
                                    onResult(false, err.message)
                                }
                            })
                        }
                        override fun onCancelled(err: DatabaseError) {
                            onResult(false, err.message)
                        }
                    })
                }
        }
    }

    private fun fetchAndSaveProfile(context: Context, uid: String, onResult: (Boolean, String) -> Unit) {
        val db = FirebaseDatabase.getInstance()
        db.getReference("users/$uid").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Parent"
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val hasPhrase = snapshot.child("recoveryPhraseHash").getValue(String::class.java)?.isNotBlank() == true

                saveLocalAccount(context, uid, username, email, hasPhrase)
                onResult(true, "Logged in as @$username")
            }

            override fun onCancelled(error: DatabaseError) {
                saveLocalAccount(context, uid, "Parent", "", false)
                onResult(true, "Logged in successfully")
            }
        })
    }

    private fun saveLocalAccount(context: Context, uid: String, username: String, email: String, hasPhrase: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_LOGGED_IN, true)
            .putBoolean(KEY_HAS_PHRASE, hasPhrase)
            .apply()

        _accountState.value = ParentAccountState(
            uid = uid,
            username = username,
            email = email,
            isLoggedIn = true,
            hasRecoveryPhrase = hasPhrase
        )
    }

    fun logout(context: Context) {
        FirebaseAuth.getInstance().signOut()
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()

        _accountState.value = _accountState.value.copy(isLoggedIn = false)
        Log.i(TAG, "Parent account logged out.")
    }
}
