package com.prism.screenharmony.flex.family

enum class FamilyRole {
    STANDALONE,
    PARENT,
    CHILD
}

data class FamilyProfile(
    val familyId: String = "",
    val role: FamilyRole = FamilyRole.STANDALONE,
    val pairingCode: String = "",
    val pairingSecret: String = "",
    val familyName: String = "My Family",
    val linkedAt: Long = 0L
)

data class RemoteChildDevice(
    val deviceId: String = "",
    val deviceName: String = "Child Phone",
    val model: String = "",
    val androidVersion: String = "",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val isScreenOn: Boolean = true,
    val currentApp: String? = null,
    val lastSeen: Long = 0L,
    val isLocked: Boolean = false,
    val rulesCount: Int = 0,
    val screenTimeMinutes: Long = 0L
) {
    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastSeen) < 120_000L // Active in last 2 minutes
}
