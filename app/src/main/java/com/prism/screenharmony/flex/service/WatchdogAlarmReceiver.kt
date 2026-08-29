package com.prism.screenharmony.flex.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.prism.screenharmony.flex.data.BlockRepository

class WatchdogAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenHarmony_Watchdog"
        const val ACTION_WATCHDOG_TICK = "com.prism.screenharmony.flex.ACTION_WATCHDOG_TICK"
        private const val WAKE_LOCK_TAG = "ScreenHarmony:WatchdogWakeLock"
        private const val INTERVAL_MILLIS = 60_000L // 1 minute watchdog interval

        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WatchdogAlarmReceiver::class.java).apply {
                action = ACTION_WATCHDOG_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = SystemClock.elapsedRealtime() + INTERVAL_MILLIS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled next watchdog alarm in ${INTERVAL_MILLIS / 1000}s")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule exact watchdog alarm", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Watchdog alarm tick received. Action=${intent?.action}")

        // Acquire a short partial wake lock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(5000L) // 5 seconds max
        }

        try {
            BlockRepository.initialize(context)

            // Re-assert foreground blocker service
            AppBlockerService.start(context)

            // Schedule the subsequent watchdog tick
            scheduleNext(context)
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }
        }
    }
}
