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

    fun getAllPermissions(context: Context): List<PermissionItem> {
        return listOf(
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
                id = "accessibility",
                name = "Accessibility Service",
                description = "Optional: provides instant hardware-level blocking and website blocking",
                isGranted = isAccessibilityGranted(context),
                isCrucialForBackground = false,
                onGrant = { openAccessibilitySettings(it) }
            ),
            PermissionItem(
                id = "notifications",
                name = "Notifications",
                description = "Keeps the foreground monitoring service alive",
                isGranted = isNotificationGranted(context),
                isCrucialForBackground = false,
                onGrant = { openNotificationSettings(it) }
            )
        )
    }
}
