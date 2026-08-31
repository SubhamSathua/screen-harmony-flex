package com.prism.screenharmony.flex.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class BlockAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenHarmony_AlarmRx"
        const val ACTION_EVALUATE_SCHEDULE = "com.prism.screenharmony.flex.ACTION_EVALUATE_SCHEDULE"
        private const val WAKE_LOCK_TAG = "ScreenHarmony:BlockAlarmWakeLock"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "Exact Block Alarm fired! Action=${intent?.action}")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(3000L) // 3 seconds max to re-evaluate and start/stop service
        }

        try {
            BlockScheduleManager.reschedule(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating schedule on alarm receive", e)
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
