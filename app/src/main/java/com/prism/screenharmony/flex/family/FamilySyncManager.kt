package com.prism.screenharmony.flex.family

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.BlockRule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

object FamilySyncManager {

    private const val TAG = "ScreenHarmony_FamilySync"
    private const val PREFS_NAME = "screen_harmony_family_prefs"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null

    private val _familyProfile = MutableStateFlow(FamilyProfile())
    val familyProfile: StateFlow<FamilyProfile> = _familyProfile.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<RemoteChildDevice>>(emptyList())
    val connectedDevices: StateFlow<List<RemoteChildDevice>> = _connectedDevices.asStateFlow()

    private val _childPushedRules = MutableStateFlow<List<BlockRule>>(emptyList())
    val childPushedRules: StateFlow<List<BlockRule>> = _childPushedRules.asStateFlow()

    private val _oneTimeDenialAlert = MutableStateFlow(false)
    val oneTimeDenialAlert: StateFlow<Boolean> = _oneTimeDenialAlert.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var rulesListener: ValueEventListener? = null
    private var devicesListener: ValueEventListener? = null
    private var unlinkRequestListener: ValueEventListener? = null

    fun dismissDenialAlert() {
        _oneTimeDenialAlert.value = false
    }

    fun initialize(context: Context) {
        try {
            // 1. Load saved local family profile immediately so UI state is instant on startup
            loadLocalProfile(context)

            // 2. Enable Firebase Realtime Database Offline Persistence
            val db = FirebaseDatabase.getInstance()
            try {
                db.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Already enabled
            }
            database = db
            auth = FirebaseAuth.getInstance()

            // 3. Ensure anonymous authentication
            ensureAuth()

            // 4. Start active sync depending on role
            startRoleSync(context)

            Log.i(TAG, "FamilySyncManager initialized successfully. Role=${_familyProfile.value.role}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FamilySyncManager", e)
        }
    }

    private fun ensureAuth(onReady: () -> Unit = {}) {
        val a = auth ?: FirebaseAuth.getInstance().also { auth = it }
        val user = a.currentUser
        if (user != null) {
            onReady()
        } else {
            a.signInAnonymously()
                .addOnSuccessListener {
                    Log.i(TAG, "Firebase Anonymous Auth succeeded. UID=${it.user?.uid}")
                    onReady()
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Firebase Anonymous Auth failed", error)
                    onReady()
                }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
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

    private fun loadLocalProfile(context: Context) {
        val prefs = getPrefs(context)
        val familyId = prefs.getString("family_id", "") ?: ""
        val roleStr = prefs.getString("role", FamilyRole.STANDALONE.name) ?: FamilyRole.STANDALONE.name
        val pairingCode = prefs.getString("pairing_code", "") ?: ""
        val pairingSecret = prefs.getString("pairing_secret", "") ?: ""
        val familyName = prefs.getString("family_name", "My Family") ?: "My Family"
        val linkedAt = prefs.getLong("linked_at", 0L)

        val role = try { FamilyRole.valueOf(roleStr) } catch (e: Exception) { FamilyRole.STANDALONE }
        _familyProfile.value = FamilyProfile(
            familyId = familyId,
            role = role,
            pairingCode = pairingCode,
            pairingSecret = pairingSecret,
            familyName = familyName,
            linkedAt = linkedAt
        )
    }

    private fun saveLocalProfile(context: Context, profile: FamilyProfile) {
        _familyProfile.value = profile
        getPrefs(context).edit()
            .putString("family_id", profile.familyId)
            .putString("role", profile.role.name)
            .putString("pairing_code", profile.pairingCode)
            .putString("pairing_secret", profile.pairingSecret)
            .putString("family_name", profile.familyName)
            .putLong("linked_at", profile.linkedAt)
            .apply()
    }

    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    // =========================================================================
    // PARENT SETUP: CREATE FAMILY
    // =========================================================================

    fun setupAsParent(context: Context, familyName: String = "My Family", onComplete: (Boolean) -> Unit) {
        ensureAuth {
            val familyId = "fam_" + UUID.randomUUID().toString().replace("-", "").take(10)
            val randomDigits = (1000..9999).random()
            val pairingCode = "SH-$randomDigits"
            val pairingSecret = UUID.randomUUID().toString()

            val profile = FamilyProfile(
                familyId = familyId,
                role = FamilyRole.PARENT,
                pairingCode = pairingCode,
                pairingSecret = pairingSecret,
                familyName = familyName,
                linkedAt = System.currentTimeMillis()
            )

            saveLocalProfile(context, profile)

            // Write family metadata to Firebase
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            val familyRef = db.getReference("families/$familyId/info")
            val data = mapOf(
                "pairingCode" to pairingCode,
                "pairingSecret" to pairingSecret,
                "familyName" to familyName,
                "createdAt" to ServerValue.TIMESTAMP
            )

            familyRef.setValue(data)
                .addOnSuccessListener {
                    // Also create index mapping for 6-digit code lookup
                    db.getReference("code_index/$pairingCode").setValue(mapOf(
                        "familyId" to familyId,
                        "pairingSecret" to pairingSecret
                    ))
                    startRoleSync(context)
                    onComplete(true)
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to register family in cloud", it)
                    onComplete(false)
                }
        }
    }

    // =========================================================================
    // CHILD SETUP: JOIN FAMILY VIA QR OR CODE
    // =========================================================================

    fun joinFamilyViaQr(context: Context, qrPayload: String, onComplete: (Boolean, String?) -> Unit) {
        ensureAuth {
            try {
                val json = JSONObject(qrPayload)
                val familyId = json.getString("familyId")
                val pairingCode = json.optString("code", "")
                val pairingSecret = json.optString("secret", "")
                val familyName = json.optString("name", "Parent's Family")

                completeChildJoin(context, familyId, pairingCode, pairingSecret, familyName, onComplete)
            } catch (e: Exception) {
                onComplete(false, "Invalid QR Code format: ${e.message}")
            }
        }
    }

    fun joinFamilyViaCode(context: Context, code: String, onComplete: (Boolean, String?) -> Unit) {
        ensureAuth {
            val cleanCode = code.trim().uppercase()
            val formattedCode = if (!cleanCode.startsWith("SH-")) "SH-$cleanCode" else cleanCode

            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            db.getReference("code_index/$formattedCode").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val familyId = snapshot.child("familyId").getValue(String::class.java)
                    val pairingSecret = snapshot.child("pairingSecret").getValue(String::class.java)

                    if (familyId != null && pairingSecret != null) {
                        completeChildJoin(context, familyId, formattedCode, pairingSecret, "Parent's Family", onComplete)
                    } else {
                        onComplete(false, "Pairing code not found or expired")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(false, error.message)
                }
            })
        }
    }

    private fun completeChildJoin(
        context: Context,
        familyId: String,
        pairingCode: String,
        pairingSecret: String,
        familyName: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val profile = FamilyProfile(
            familyId = familyId,
            role = FamilyRole.CHILD,
            pairingCode = pairingCode,
            pairingSecret = pairingSecret,
            familyName = familyName,
            linkedAt = System.currentTimeMillis()
        )

        saveLocalProfile(context, profile)

        val deviceId = getDeviceId(context)
        val db = database ?: FirebaseDatabase.getInstance().also { database = it }

        // Register child device metadata
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

        val deviceInfo = mapOf(
            "deviceId" to deviceId,
            "deviceName" to Build.MODEL,
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to "Android ${Build.VERSION.RELEASE}",
            "batteryLevel" to batteryLevel,
            "isCharging" to false,
            "lastSeen" to ServerValue.TIMESTAMP,
            "isLocked" to false
        )

        db.getReference("families/$familyId/devices/$deviceId/info").setValue(deviceInfo)
            .addOnSuccessListener {
                startRoleSync(context)
                onComplete(true, null)
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to register child device", it)
                onComplete(false, it.message)
            }
    }

    // =========================================================================
    // ACTIVE SYNC ENGINE (CHILD & PARENT)
    // =========================================================================

    fun startRoleSync(context: Context) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

        when (profile.role) {
            FamilyRole.PARENT -> {
                // Parent listens to all child devices
                devicesListener?.let { db.getReference("families/${profile.familyId}/devices").removeEventListener(it) }

                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val devices = mutableListOf<RemoteChildDevice>()
                        for (childSnap in snapshot.children) {
                            val deviceId = childSnap.key ?: continue
                            val infoSnap = childSnap.child("info")
                            val statusSnap = childSnap.child("status")
                            val rulesSnap = childSnap.child("rules")
                            val screenTimeSnap = childSnap.child("screenTime")
                            val unlinkSnap = childSnap.child("unlinkRequest")

                            val deviceName = infoSnap.child("deviceName").getValue(String::class.java) ?: "Child Device"
                            val customName = infoSnap.child("customName").getValue(String::class.java) ?: ""
                            val model = infoSnap.child("model").getValue(String::class.java) ?: ""
                            val androidVersion = infoSnap.child("androidVersion").getValue(String::class.java) ?: ""
                            val batteryLevel = (infoSnap.child("batteryLevel").getValue(Long::class.java) ?: 100L).toInt()
                            val isCharging = infoSnap.child("isCharging").getValue(Boolean::class.java) ?: false
                            val isScreenOn = statusSnap.child("isScreenOn").getValue(Boolean::class.java) ?: true
                            val currentApp = statusSnap.child("currentApp").getValue(String::class.java)
                            val lastSeen = infoSnap.child("lastSeen").getValue(Long::class.java) ?: 0L
                            val isLocked = childSnap.child("commands/lockNow").getValue(Boolean::class.java) ?: false
                            val rulesCount = rulesSnap.childrenCount.toInt()
                            val screenTimeMinutes = screenTimeSnap.child("todayMinutes").getValue(Long::class.java) ?: 0L

                            val unlinkRequested = unlinkSnap.child("requested").getValue(Boolean::class.java) ?: false
                            val unlinkRequestedAt = unlinkSnap.child("requestedAt").getValue(Long::class.java) ?: 0L
                            val unlinkReason = unlinkSnap.child("reason").getValue(String::class.java) ?: ""

                            if (unlinkRequested && unlinkRequestedAt > 0L) {
                                val prefs = getPrefs(context)
                                val lastNotified = prefs.getLong("last_notified_unlink_$deviceId", 0L)
                                if (unlinkRequestedAt > lastNotified) {
                                    prefs.edit().putLong("last_notified_unlink_$deviceId", unlinkRequestedAt).apply()
                                    FamilyNotificationHelper.postUnlinkRequestNotification(
                                        context,
                                        customName.ifBlank { deviceName },
                                        unlinkReason
                                    )
                                }
                            }

                            devices.add(
                                RemoteChildDevice(
                                    deviceId = deviceId,
                                    deviceName = deviceName,
                                    customName = customName,
                                    model = model,
                                    androidVersion = androidVersion,
                                    batteryLevel = batteryLevel,
                                    isCharging = isCharging,
                                    isScreenOn = isScreenOn,
                                    currentApp = currentApp,
                                    lastSeen = lastSeen,
                                    isLocked = isLocked,
                                    rulesCount = rulesCount,
                                    screenTimeMinutes = screenTimeMinutes,
                                    unlinkRequested = unlinkRequested,
                                    unlinkRequestedAt = unlinkRequestedAt,
                                    unlinkReason = unlinkReason
                                )
                            )
                        }
                        _connectedDevices.value = devices
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Parent devices listener cancelled", error.toException())
                    }
                }
                devicesListener = listener
                db.getReference("families/${profile.familyId}/devices").addValueEventListener(listener)
            }

            FamilyRole.CHILD -> {
                val deviceId = getDeviceId(context)

                // 1. Listen for remote rules pushed by Parent
                rulesListener?.let { db.getReference("families/${profile.familyId}/devices/$deviceId/rules").removeEventListener(it) }
                val ruleListListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.i(TAG, "Child rules snapshot received: ${snapshot.childrenCount} rules")
                        val remoteRules = mutableListOf<BlockRule>()
                        for (rSnap in snapshot.children) {
                            try {
                                val rawJson = rSnap.child("ruleJson").getValue(String::class.java)
                                val parsedRule = if (!rawJson.isNullOrBlank()) {
                                    BlockRepository.deserializeSingleRule(rawJson)
                                } else {
                                    val id = rSnap.child("id").getValue(String::class.java) ?: rSnap.key ?: continue
                                    val name = rSnap.child("name").getValue(String::class.java) ?: "Remote Rule"
                                    val isEnabled = rSnap.child("isEnabled").getValue(Boolean::class.java) ?: true
                                    val durationSec = (rSnap.child("blockDurationSeconds").getValue(Long::class.java) ?: 5L).toInt()

                                    val apps = mutableSetOf<String>()
                                    for (appSnap in rSnap.child("selectedApps").children) {
                                        appSnap.getValue(String::class.java)?.let { apps.add(it) }
                                    }

                                    val sites = mutableSetOf<String>()
                                    for (siteSnap in rSnap.child("selectedWebsites").children) {
                                        siteSnap.getValue(String::class.java)?.let { sites.add(it) }
                                    }

                                    BlockRule(
                                        id = id,
                                        name = name,
                                        isEnabled = isEnabled,
                                        selectedApps = apps,
                                        selectedWebsites = sites,
                                        blockDurationSeconds = durationSec
                                    )
                                }

                                if (parsedRule != null) {
                                    remoteRules.add(parsedRule)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remote rule", e)
                            }
                        }

                        val previousRemoteRules = _childPushedRules.value
                        _childPushedRules.value = remoteRules

                        // Sync with local SQLite BlockRepository:
                        // 1. Delete rules that were removed remotely
                        val newRemoteIds = remoteRules.map { it.id }.toSet()
                        for (prevRule in previousRemoteRules) {
                            if (!newRemoteIds.contains(prevRule.id)) {
                                BlockRepository.deleteRule(prevRule.id)
                            }
                        }

                        // 2. Add or update active remote rules
                        for (rule in remoteRules) {
                            BlockRepository.addOrUpdateRule(rule)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
                rulesListener = ruleListListener
                db.getReference("families/${profile.familyId}/devices/$deviceId/rules").addValueEventListener(ruleListListener)

                // 2. Listen for Unlink Denial from Parent
                unlinkRequestListener?.let { db.getReference("families/${profile.familyId}/devices/$deviceId/unlinkRequest").removeEventListener(it) }
                val uListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.child("status").getValue(String::class.java)
                        val deniedAt = snapshot.child("deniedAt").getValue(Long::class.java) ?: 0L
                        val prefs = getPrefs(context)
                        val lastSeenDenial = prefs.getLong("last_seen_denial_timestamp", 0L)

                        if (status == "DENIED" && deniedAt > lastSeenDenial) {
                            prefs.edit().putLong("last_seen_denial_timestamp", deniedAt).apply()
                            _oneTimeDenialAlert.value = true
                            FamilyNotificationHelper.postUnlinkDeniedNotification(context)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                unlinkRequestListener = uListener
                db.getReference("families/${profile.familyId}/devices/$deviceId/unlinkRequest").addValueEventListener(uListener)

                // 3. Listen for Device Removal by Parent
                db.getReference("families/${profile.familyId}/devices/$deviceId").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists() && _familyProfile.value.role == FamilyRole.CHILD) {
                            Log.w(TAG, "Child device was removed by Parent. Unlinking...")
                            unlinkFamily(context)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 4. Child telemetry loop (Heartbeat, battery, screen time, active app, installed apps)
                scope.launch {
                    // Sync installed apps immediately on startup
                    syncChildInstalledApps(context)

                    var loopCount = 0
                    while (isActive) {
                        try {
                            pushChildTelemetry(context)
                            // Sync full installed app list every 3 minutes (4 * 45s)
                            if (loopCount % 4 == 0) {
                                syncChildInstalledApps(context)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error pushing child telemetry", e)
                        }
                        loopCount++
                        delay(45_000L) // Push heartbeat every 45s
                    }
                }
            }

            FamilyRole.STANDALONE -> {
                // No active sync
            }
        }
        }
    }

    private fun pushChildTelemetry(context: Context) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.CHILD || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            val deviceId = getDeviceId(context)

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            val isCharging = bm?.isCharging ?: false

            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isScreenOn = pm?.isInteractive ?: true

            val todayMinutes = FamilyUsageHelper.getTodayUsageMinutes(context)
            val topApps = FamilyUsageHelper.getTodayTopApps(context, limit = 10)
            val appsMap = mutableMapOf<String, Any>()
            for (app in topApps) {
                val safeKey = app.packageName.replace(".", "_")
                appsMap[safeKey] = mapOf(
                    "packageName" to app.packageName,
                    "appName" to app.appName,
                    "durationMinutes" to app.durationMinutes
                )
            }

            val updates = mapOf(
                "batteryLevel" to batteryLevel,
                "isCharging" to isCharging,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            db.getReference("families/${profile.familyId}/devices/$deviceId/info").updateChildren(updates)

            val statusUpdates = mapOf(
                "isScreenOn" to isScreenOn
            )
            db.getReference("families/${profile.familyId}/devices/$deviceId/status").updateChildren(statusUpdates)

            val screenTimeUpdates = mapOf(
                "todayMinutes" to todayMinutes,
                "apps" to appsMap
            )
            db.getReference("families/${profile.familyId}/devices/$deviceId/screenTime").updateChildren(screenTimeUpdates)
        }
    }

    fun forceRefresh(context: Context, onResult: (Boolean, String) -> Unit) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.familyId.isBlank()) {
                onResult(false, "No active family connection")
                return@ensureAuth
            }
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            when (profile.role) {
                FamilyRole.PARENT -> {
                    db.getReference("families/${profile.familyId}/devices").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            startRoleSync(context)
                            onResult(true, "Data refreshed successfully")
                        }
                        override fun onCancelled(error: DatabaseError) {
                            onResult(false, "Refresh failed: ${error.message}")
                        }
                    })
                }
                FamilyRole.CHILD -> {
                    val deviceId = getDeviceId(context)
                    pushChildTelemetry(context)
                    db.getReference("families/${profile.familyId}/devices/$deviceId/rules").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            startRoleSync(context)
                            onResult(true, "Data refreshed successfully")
                        }
                        override fun onCancelled(error: DatabaseError) {
                            onResult(false, "Refresh failed: ${error.message}")
                        }
                    })
                }
                FamilyRole.STANDALONE -> {
                    onResult(true, "Refreshed")
                }
            }
        }
    }

    fun listenChildScreenTimeApps(childDeviceId: String, onApps: (List<ChildAppUsage>) -> Unit): ValueEventListener? {
        val profile = _familyProfile.value
        if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return null
        val db = database ?: FirebaseDatabase.getInstance().also { database = it }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChildAppUsage>()
                for (appSnap in snapshot.child("apps").children) {
                    val pkg = appSnap.child("packageName").getValue(String::class.java) ?: continue
                    val name = appSnap.child("appName").getValue(String::class.java) ?: pkg
                    val duration = appSnap.child("durationMinutes").getValue(Long::class.java) ?: 0L
                    list.add(ChildAppUsage(packageName = pkg, appName = name, durationMinutes = duration))
                }
                onApps(list.sortedByDescending { it.durationMinutes })
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.getReference("families/${profile.familyId}/devices/$childDeviceId/screenTime").addValueEventListener(listener)
        return listener
    }

    fun removeScreenTimeListener(childDeviceId: String, listener: ValueEventListener) {
        val profile = _familyProfile.value
        if (profile.familyId.isBlank()) return
        database?.getReference("families/${profile.familyId}/devices/$childDeviceId/screenTime")?.removeEventListener(listener)
    }

    // =========================================================================
    // CHILD UNLINK REQUEST
    // =========================================================================

    fun requestUnlinkFromChild(context: Context, reason: String = "", onComplete: (Boolean) -> Unit) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.CHILD || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            val deviceId = getDeviceId(context)

            val requestData = mapOf(
                "requested" to true,
                "requestedAt" to ServerValue.TIMESTAMP,
                "status" to "PENDING",
                "reason" to reason
            )

            db.getReference("families/${profile.familyId}/devices/$deviceId/unlinkRequest")
                .setValue(requestData)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun ignoreUnlinkRequest(childDeviceId: String, onComplete: (Boolean) -> Unit = {}) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            val denialData = mapOf(
                "requested" to false,
                "status" to "DENIED",
                "deniedAt" to ServerValue.TIMESTAMP
            )

            db.getReference("families/${profile.familyId}/devices/$childDeviceId/unlinkRequest")
                .setValue(denialData)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun cancelUnlinkRequestFromChild(context: Context, onComplete: (Boolean) -> Unit) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.CHILD || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            val deviceId = getDeviceId(context)

            db.getReference("families/${profile.familyId}/devices/$deviceId/unlinkRequest")
                .removeValue()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    // =========================================================================
    // PARENT REMOTE COMMANDS
    // =========================================================================

    fun updateFamilyName(context: Context, newName: String, onComplete: (Boolean) -> Unit = {}) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            val updatedProfile = profile.copy(familyName = newName)
            _familyProfile.value = updatedProfile
            saveLocalProfile(context, updatedProfile)

            if (profile.role == FamilyRole.PARENT) {
                db.getReference("families/${profile.familyId}/info/familyName")
                    .setValue(newName)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            } else {
                onComplete(true)
            }
        }
    }

    fun renameChildDevice(childDeviceId: String, customName: String, onComplete: (Boolean) -> Unit = {}) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            db.getReference("families/${profile.familyId}/devices/$childDeviceId/info/customName")
                .setValue(customName.trim())
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun removeAndUnlinkChildDevice(childDeviceId: String, onComplete: (Boolean) -> Unit = {}) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            db.getReference("families/${profile.familyId}/devices/$childDeviceId")
                .removeValue()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun toggleRemoteLock(childDeviceId: String, lock: Boolean) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            db.getReference("families/${profile.familyId}/devices/$childDeviceId/commands/lockNow").setValue(lock)
        }
    }

    fun pushRuleToChild(childDeviceId: String, rule: BlockRule) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }

            val ruleJson = BlockRepository.serializeRule(rule)
            val ruleMap = mapOf(
                "id" to rule.id,
                "name" to rule.name,
                "isEnabled" to rule.isEnabled,
                "blockDurationSeconds" to rule.blockDurationSeconds,
                "selectedApps" to rule.selectedApps.toList(),
                "selectedWebsites" to rule.selectedWebsites.toList(),
                "ruleJson" to ruleJson,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            db.getReference("families/${profile.familyId}/devices/$childDeviceId/rules/${rule.id}").setValue(ruleMap)
        }
    }

    fun toggleRuleOnChild(childDeviceId: String, rule: BlockRule, isEnabled: Boolean) {
        val updated = rule.copy(isEnabled = isEnabled)
        pushRuleToChild(childDeviceId, updated)
    }

    fun pauseRuleOnChild(childDeviceId: String, rule: BlockRule, durationMinutes: Int) {
        val updated = if (durationMinutes > 0) {
            rule.copy(
                lastPausedAt = System.currentTimeMillis(),
                pauseDurationMinutes = durationMinutes
            )
        } else {
            rule.copy(
                lastPausedAt = null,
                pauseDurationMinutes = null
            )
        }
        pushRuleToChild(childDeviceId, updated)
    }

    fun deleteRuleOnChild(childDeviceId: String, ruleId: String) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            db.getReference("families/${profile.familyId}/devices/$childDeviceId/rules/$ruleId").removeValue()
        }
    }

    fun listenChildRules(childDeviceId: String, onRules: (List<BlockRule>) -> Unit): ValueEventListener? {
        val profile = _familyProfile.value
        if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return null
        val db = database ?: FirebaseDatabase.getInstance().also { database = it }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<BlockRule>()
                for (rSnap in snapshot.children) {
                    try {
                        val rawJson = rSnap.child("ruleJson").getValue(String::class.java)
                        val parsedRule = if (!rawJson.isNullOrBlank()) {
                            BlockRepository.deserializeSingleRule(rawJson)
                        } else {
                            val id = rSnap.child("id").getValue(String::class.java) ?: rSnap.key ?: continue
                            val name = rSnap.child("name").getValue(String::class.java) ?: "Remote Rule"
                            val isEnabled = rSnap.child("isEnabled").getValue(Boolean::class.java) ?: true
                            val durationSec = (rSnap.child("blockDurationSeconds").getValue(Long::class.java) ?: 5L).toInt()

                            val apps = mutableSetOf<String>()
                            for (appSnap in rSnap.child("selectedApps").children) {
                                appSnap.getValue(String::class.java)?.let { apps.add(it) }
                            }

                            val sites = mutableSetOf<String>()
                            for (siteSnap in rSnap.child("selectedWebsites").children) {
                                siteSnap.getValue(String::class.java)?.let { sites.add(it) }
                            }

                            BlockRule(
                                id = id,
                                name = name,
                                isEnabled = isEnabled,
                                selectedApps = apps,
                                selectedWebsites = sites,
                                blockDurationSeconds = durationSec
                            )
                        }

                        if (parsedRule != null) {
                            list.add(parsedRule)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing child rule", e)
                    }
                }
                onRules(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.getReference("families/${profile.familyId}/devices/$childDeviceId/rules").addValueEventListener(listener)
        return listener
    }

    fun removeRulesListener(childDeviceId: String, listener: ValueEventListener) {
        val profile = _familyProfile.value
        if (profile.familyId.isBlank()) return
        database?.getReference("families/${profile.familyId}/devices/$childDeviceId/rules")?.removeEventListener(listener)
    }

    fun toggleChildRule(childDeviceId: String, ruleId: String, isEnabled: Boolean) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            db.getReference("families/${profile.familyId}/devices/$childDeviceId/rules/$ruleId/isEnabled").setValue(isEnabled)
        }
    }

    // =========================================================
    // Child Installed Apps Synchronization (Text string metadata only - 0 images)
    // =========================================================

    fun syncChildInstalledApps(context: Context) {
        ensureAuth {
            val profile = _familyProfile.value
            if (profile.role != FamilyRole.CHILD || profile.familyId.isBlank()) return@ensureAuth
            val db = database ?: FirebaseDatabase.getInstance().also { database = it }
            val deviceId = getDeviceId(context)

            try {
                val pm = context.packageManager
                val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                }
                val resolved = pm.queryIntentActivities(mainIntent, 0)
                val appsMap = mutableMapOf<String, Any>()
                for (resolveInfo in resolved) {
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg == context.packageName) continue
                    val label = resolveInfo.loadLabel(pm).toString()
                    val safeKey = pkg.replace(".", "_")
                    appsMap[safeKey] = mapOf(
                        "packageName" to pkg,
                        "name" to label
                    )
                }
                db.getReference("families/${profile.familyId}/devices/$deviceId/installedApps").setValue(appsMap)
                Log.i(TAG, "Synced ${appsMap.size} installed apps metadata to cloud from child device")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing child installed apps", e)
            }
        }
    }

    fun listenChildInstalledApps(childDeviceId: String, onApps: (List<ChildAppInfo>) -> Unit): ValueEventListener? {
        val profile = _familyProfile.value
        if (profile.role != FamilyRole.PARENT || profile.familyId.isBlank()) return null
        val db = database ?: FirebaseDatabase.getInstance().also { database = it }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChildAppInfo>()
                for (child in snapshot.children) {
                    val pkg = child.child("packageName").getValue(String::class.java) ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: pkg
                    list.add(ChildAppInfo(packageName = pkg, name = name))
                }
                list.sortBy { it.name.lowercase() }
                onApps(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.getReference("families/${profile.familyId}/devices/$childDeviceId/installedApps").addValueEventListener(listener)
        return listener
    }

    fun removeInstalledAppsListener(childDeviceId: String, listener: ValueEventListener) {
        val profile = _familyProfile.value
        if (profile.familyId.isBlank()) return
        database?.getReference("families/${profile.familyId}/devices/$childDeviceId/installedApps")?.removeEventListener(listener)
    }

    fun unlinkFamily(context: Context) {
        saveLocalProfile(context, FamilyProfile())
        _connectedDevices.value = emptyList()
    }
}

data class ChildAppInfo(
    val packageName: String = "",
    val name: String = ""
)
