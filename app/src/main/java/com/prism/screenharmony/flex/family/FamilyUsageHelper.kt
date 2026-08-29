package com.prism.screenharmony.flex.family

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.Calendar

data class ChildAppUsage(
    val packageName: String,
    val appName: String,
    val durationMinutes: Long,
    val icon: Drawable? = null
)

object FamilyUsageHelper {

    fun getTodayUsageMinutes(context: Context): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = usageStatsManager.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())
        var totalMillis = 0L
        for ((_, usage) in stats) {
            totalMillis += usage.totalTimeInForeground
        }
        return totalMillis / 60_000L
    }

    fun getTodayTopApps(context: Context, limit: Int = 6): List<ChildAppUsage> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val packageManager = context.packageManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = usageStatsManager.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())

        return stats.values
            .filter { it.totalTimeInForeground > 60_000L } // More than 1 min
            .sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map { usage ->
                val pkg = usage.packageName
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg
                }
                val icon = try {
                    packageManager.getApplicationIcon(pkg)
                } catch (e: Exception) {
                    null
                }
                ChildAppUsage(
                    packageName = pkg,
                    appName = appName,
                    durationMinutes = usage.totalTimeInForeground / 60_000L,
                    icon = icon
                )
            }
    }
}
