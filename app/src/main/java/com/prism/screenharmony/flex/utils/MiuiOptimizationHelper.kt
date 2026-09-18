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

    fun isSamsungDevice(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return man.contains("samsung") || brand.contains("samsung")
    }

    fun isHuaweiDevice(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return man.contains("huawei") || brand.contains("huawei") || brand.contains("honor") || man.contains("honor")
    }

    fun isOppoOrRealme(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return man.contains("oppo") || man.contains("realme") || brand.contains("oppo") || brand.contains("realme") || man.contains("oneplus")
    }

    fun isVivoDevice(): Boolean {
        val man = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return man.contains("vivo") || brand.contains("vivo") || man.contains("iqoo") || brand.contains("iqoo")
    }

    fun isOemDeviceWithAggressiveKiller(): Boolean {
        return isXiaomiDevice() || isSamsungDevice() || isHuaweiDevice() || isOppoOrRealme() || isVivoDevice()
    }

    /**
     * Opens the OEM Autostart management settings screen (Xiaomi, Oppo, Vivo, Huawei, OnePlus).
     */
    fun openOemAutostartSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        // 1. Xiaomi / Redmi / POCO (MIUI / HyperOS)
        intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
        intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagement")))
        intents.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))

        // 2. Oppo / Realme / ColorOS / OxygenOS
        intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
        intents.add(Intent().setComponent(ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity")))
        intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
        intents.add(Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")))

        // 3. Vivo / iQOO / FuntouchOS / OriginOS
        intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
        intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
        intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))

        // 4. Huawei / Honor / EMUI / MagicOS
        intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
        intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Autostart intent candidate failed: ${e.message}")
            }
        }
        return openAppDetailsSettings(context)
    }

    /**
     * Backward-compatible alias for MIUI Autostart.
     */
    fun openMiuiAutostartSettings(context: Context): Boolean = openOemAutostartSettings(context)

    /**
     * Opens the OEM Battery Saver / Background Unrestricted screen.
     */
    fun openOemBatterySaverSettings(context: Context): Boolean {
        val intents = mutableListOf<Intent>()

        // 1. Xiaomi / MIUI / HyperOS
        intents.add(
            Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
                .putExtra("package_name", context.packageName)
                .putExtra("package_label", "ScreenHarmony Flex")
        )
        intents.add(Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity")))

        // 2. Samsung OneUI Device Care / Battery
        intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
        intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
        intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")))

        // 3. Oppo / Realme Power Manager
        intents.add(Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")))
        intents.add(Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.PowerConsumptionActivity")))

        // 4. Huawei Protected Apps
        intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity")))

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Battery Saver intent candidate failed: ${e.message}")
            }
        }
        return openAppDetailsSettings(context)
    }

    /**
     * Backward-compatible alias for MIUI Battery Saver.
     */
    fun openMiuiBatterySaverSettings(context: Context): Boolean = openOemBatterySaverSettings(context)

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
            Log.d(TAG, "Other permissions intent failed: ${e.message}")
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
