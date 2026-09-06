package com.prism.screenharmony.flex.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object MiuiOptimizationHelper {

    private const val TAG = "ScreenHarmony_OEM"

    fun isXiaomiDevice(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val finger = Build.FINGERPRINT.lowercase()
        return man.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") || finger.contains("miui")
    }

    fun isOemDeviceWithAggressiveKiller(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return isXiaomiDevice() || man.contains("samsung") || man.contains("oppo") || man.contains("realme") || man.contains("vivo") || man.contains("huawei") || brand.contains("honor")
    }

    /**
     * Opens the MIUI / HyperOS Autostart management settings screen.
     */
    fun openMiuiAutostartSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagement")),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Autostart intent failed: ")
            }
        }
        return openAppDetailsSettings(context)
    }

    /**
     * Opens the MIUI Battery Saver screen to set 'No Restrictions'.
     */
    fun openMiuiBatterySaverSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                .putExtra("package_name", context.packageName)
                .putExtra("package_label", "ScreenHarmony Flex"),
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"))
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Battery Saver intent failed: ")
            }
        }
        return openAppDetailsSettings(context)
    }

    /**
     * Opens the MIUI Other Permissions screen (Pop-up windows & Lock screen display).
     */
    fun openMiuiOtherPermissions(context: Context): Boolean {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
            putExtra("extra_pkgname", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Other permissions intent failed: ")
            openAppDetailsSettings(context)
        }
    }

    fun openAppDetailsSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "App details intent failed", e)
            false
        }
    }
}
