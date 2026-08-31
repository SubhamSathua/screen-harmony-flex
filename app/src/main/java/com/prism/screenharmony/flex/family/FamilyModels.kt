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

data class ChildPermissionsState(
    val isUsageGranted: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBatteryIgnored: Boolean = false,
    val isExactAlarmGranted: Boolean = false,
    val isAccessibilityGranted: Boolean = false,
    val isNotificationGranted: Boolean = false
) {
    val totalCount: Int = 6
    val grantedCount: Int
        get() = listOf(
            isUsageGranted,
            isOverlayGranted,
            isBatteryIgnored,
            isExactAlarmGranted,
            isAccessibilityGranted,
            isNotificationGranted
        ).count { it }

    val areAllGranted: Boolean
        get() = grantedCount == totalCount

    val hasCrucialGranted: Boolean
        get() = isUsageGranted && isOverlayGranted && isBatteryIgnored
}

data class RemoteChildDevice(
    val deviceId: String = "",
    val deviceName: String = "Child Phone",
    val customName: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val isScreenOn: Boolean = true,
    val currentApp: String? = null,
    val lastSeen: Long = 0L,
    val isLocked: Boolean = false,
    val rulesCount: Int = 0,
    val screenTimeMinutes: Long = 0L,
    val unlinkRequested: Boolean = false,
    val unlinkRequestedAt: Long = 0L,
    val unlinkReason: String = "",
    val permissions: ChildPermissionsState = ChildPermissionsState()
) {
    val displayName: String
        get() = customName.ifBlank { deviceName }

    val isOnline: Boolean
        get() = (System.currentTimeMillis() - lastSeen) < 120_000L // Active in last 2 minutes
}
