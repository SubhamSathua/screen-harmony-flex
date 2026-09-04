package com.prism.screenharmony.flex.utils

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.prism.screenharmony.flex.service.WebsiteAccessibilityService

data class PermissionItem(
    val id: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val isCrucialForBackground: Boolean,
    val onGrant: (Context) -> Unit
)

object PermissionHelper {

    /**
     * Checks if Usage Access (PACKAGE_USAGE_STATS) is granted.
     */
    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks if Display Over Other Apps (SYSTEM_ALERT_WINDOW) is granted.
     * CRUCIAL: Allows background service to launch the lock screen directly over other apps!
     */
    fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if Battery Optimization is disabled for this app.
     * CRUCIAL: Prevents Android from killing the background blocker service.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Checks if Accessibility Service is enabled.
     */
    fun isAccessibilityGranted(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${WebsiteAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedServiceName) || enabledServices.contains(context.packageName)
    }

    /**
     * Checks if notifications are enabled.
     */
    fun isNotificationGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasRuntime = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            val isEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            return hasRuntime || isEnabled
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.areNotificationsEnabled() ?: true
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }
    /**
     * Checks if Exact Alarm permission is granted.
     * CRUCIAL: Allows watchdog alarms to wake up the app and enforce blocks even after force stop/kill.
     */
    fun isExactAlarmGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            return alarmManager?.canScheduleExactAlarms() ?: true
        }
        return true
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openAppSettings(context)
            }
        }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Checks if the device runs Xiaomi MIUI or HyperOS.
     */
    fun isMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
        ) {
            return true
        }
        return try {
            val propertyClass = Class.forName("android.os.SystemProperties")
            val getMethod = propertyClass.getMethod("get", String::class.java)
            val miuiVersion = getMethod.invoke(null, "ro.miui.ui.version.name") as? String
            miuiVersion != null && miuiVersion.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if MIUI / HyperOS "Display pop-up windows while running in the background" is granted.
     * Op 10021 is OP_BACKGROUND_START_ACTIVITY in MIUI AppOps.
     */
    fun isMiuiBackgroundPopupGranted(context: Context): Boolean {
        if (!isMiui()) return true
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return true
        return try {
            val checkOpNoThrowMethod = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val mode = checkOpNoThrowMethod.invoke(appOps, 10021, Process.myUid(), context.packageName) as Int
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Directly opens MIUI "Other permissions" (PermissionsEditorActivity) so the user
     * can enable "Display pop-up windows while running in the background".
     */
    fun openMiuiOtherPermissions(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e1: Exception) {
            try {
                val intent2 = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                    putExtra("extra_pkgname", context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent2)
            } catch (e2: Exception) {
                try {
                    val intent3 = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        putExtra("extra_pkgname", context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent3)
                } catch (e3: Exception) {
                    openAppSettings(context)
                }
            }
        }
    }

    fun getAllPermissions(context: Context): List<PermissionItem> {
        val list = mutableListOf(
            PermissionItem(
                id = "usage",
                name = "Usage Access",
                description = "Required to detect which app is running in the foreground",
                isGranted = isUsageAccessGranted(context),
                isCrucialForBackground = true,
                onGrant = { openUsageAccessSettings(it) }
            ),
            PermissionItem(
                id = "overlay",
                name = "Display Over Other Apps",
                description = "Required to immediately show the lock screen over blocked apps in background",
                isGranted = isOverlayGranted(context),
                isCrucialForBackground = true,
                onGrant = { openOverlaySettings(it) }
            ),
            PermissionItem(
                id = "battery",
                name = "Unrestricted Background Battery",
                description = "Prevents Android OS from killing the blocker service in background",
                isGranted = isBatteryOptimizationIgnored(context),
                isCrucialForBackground = true,
                onGrant = { openBatteryOptimizationSettings(it) }
            ),
            PermissionItem(
                id = "alarm",
                name = "Alarms & Reminders",
                description = "Wakes up and resumes block enforcement reliably",
                isGranted = isExactAlarmGranted(context),
                isCrucialForBackground = true,
                onGrant = { openExactAlarmSettings(it) }
            )
        )

        if (isMiui()) {
            list.add(
                PermissionItem(
                    id = "miui_popup",
                    name = "MIUI Pop-up Windows",
                    description = "Required on MIUI / HyperOS to allow lock screen popups from background (Other permissions)",
                    isGranted = isMiuiBackgroundPopupGranted(context),
                    isCrucialForBackground = true,
                    onGrant = { openMiuiOtherPermissions(it) }
                )
            )
        }

        list.add(
            PermissionItem(
                id = "accessibility",
                name = "Accessibility Service",
                description = "Optional: provides instant hardware-level blocking and website blocking",
                isGranted = isAccessibilityGranted(context),
                isCrucialForBackground = false,
                onGrant = { openAccessibilitySettings(it) }
            )
        )

        list.add(
            PermissionItem(
                id = "notifications",
                name = "Notifications",
                description = "Keeps the foreground monitoring service alive",
                isGranted = isNotificationGranted(context),
                isCrucialForBackground = false,
                onGrant = { openNotificationSettings(it) }
            )
        )

        return list
    }
}
