package com.prism.screenharmony.flex.service

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.prism.screenharmony.flex.family.FamilySyncManager

class ScreenHarmonyMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ScreenHarmony_FCM"
        private const val WAKELOCK_TAG = "ScreenHarmony:FCM_WakeLock"
        private const val WAKELOCK_TIMEOUT_MS = 15_000L // 15 seconds
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "🔑 New FCM Registration Token received: ")
        FamilySyncManager.registerFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        try {
            val data = remoteMessage.data
            val action = data["action"] ?: "SYNC_RULES"
            Log.i(TAG, "⚡ FCM High-Priority Data Push received! action=, data=")

            when (action) {
                "LOCK_NOW" -> {
                    Log.i(TAG, "🚨 Executing Remote Lock from FCM Push...")
                    val locked = WebsiteAccessibilityService.lockDevice()
                    Log.i(TAG, "🔒 Accessibility Lock executed: ")

                    if (!locked) {
                        WebsiteAccessibilityService.launchBlockWall(
                            context = applicationContext,
                            target = "Remote Device Lock",
                            isWebsite = false,
                            quote = "Your parent has remotely locked this device.",
                            delaySeconds = 0
                        )
                    }

                    // Also refresh family sync state
                    FamilySyncManager.startRoleSync(applicationContext)
                }

                "SYNC_RULES" -> {
                    Log.i(TAG, "🔄 Syncing remote rules triggered by FCM Push...")
                    FamilySyncManager.startRoleSync(applicationContext)
                    BlockScheduleManager.reschedule(applicationContext)

                    // Ensure AppBlockerService is active
                    try {
                        AppBlockerService.start(applicationContext)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not start AppBlockerService from FCM context: ")
                    }
                }

                "PING", "WAKEUP" -> {
                    Log.i(TAG, "📡 Heartbeat / Wake-Up signal received from parent via FCM")
                    FamilySyncManager.handleWakeUpSignal(applicationContext)
                }

                else -> {
                    Log.i(TAG, "Unknown action , executing default rule refresh")
                    FamilySyncManager.startRoleSync(applicationContext)
                    BlockScheduleManager.reschedule(applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling FCM message", e)
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {}
        }
    }
}
