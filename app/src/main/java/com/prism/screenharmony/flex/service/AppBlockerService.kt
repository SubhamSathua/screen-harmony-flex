package com.prism.screenharmony.flex.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prism.screenharmony.flex.R
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.PauseType
import com.prism.screenharmony.flex.data.WallConfig
import com.prism.screenharmony.flex.ui.blocker.BlockedActivity
import com.prism.screenharmony.flex.utils.PermissionHelper
import kotlinx.coroutines.*

class AppBlockerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    private var lastInterceptedPackage: String? = null

    companion object {
        const val CHANNEL_ID = "screen_harmony_app_blocker_service"
        const val LOCK_CHANNEL_ID = "screen_harmony_lock_channel"
        const val NOTIFICATION_ID = 2001
        const val LOCK_NOTIFICATION_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        if (!isMonitoring) {
            isMonitoring = true
            startAppMonitoringLoop()
        }
        return START_STICKY
    }

    private fun startAppMonitoringLoop() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return

        serviceScope.launch {
            while (isActive) {
                if (PermissionHelper.isUsageAccessGranted(this@AppBlockerService)) {
                    val currentForeground = getForegroundPackage(usm)

                    if (currentForeground != null && currentForeground != packageName) {
                        val matchingRule = BlockRepository.getActiveRuleForApp(currentForeground)

                        if (matchingRule != null) {
                            if (lastInterceptedPackage != currentForeground) {
                                lastInterceptedPackage = currentForeground
                                val customQuote = if (matchingRule.wallConfig is WallConfig.StandardQuote) {
                                    (matchingRule.wallConfig as WallConfig.StandardQuote).quote
                                } else null

                                val delaySec = if (matchingRule.pauseConfig.type == PauseType.DELAY) {
                                    matchingRule.pauseConfig.extraValue ?: 5
                                } else if (matchingRule.pauseConfig.type == PauseType.STRICT) {
                                    0
                                } else {
                                    5
                                }

                                launchBlockWall(
                                    target = currentForeground,
                                    isWebsite = false,
                                    quote = customQuote,
                                    delaySeconds = delaySec
                                )
                            }
                        } else {
                            // User is on an unblocked app (e.g. Launcher or settings)
                            lastInterceptedPackage = null
                        }
                    }
                }
                delay(200) // Fast 200ms check
            }
        }
    }

    /**
     * Finds the currently resumed foreground application package using UsageEvents.
     * Searches a 60-second window to guarantee detecting active apps accurately.
     */
    private fun getForegroundPackage(usm: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 60_000
        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()

        var latestTime = 0L
        var latestApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // EventType 1 = ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
            if ((event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestApp = event.packageName
            }
        }
        return latestApp
    }

    /**
     * Launches BlockedActivity over the blocked app.
     * Uses BOTH a High-Priority FullScreenIntent and direct intent launch to bypass OS restrictions.
     */
    private fun launchBlockWall(
        target: String,
        isWebsite: Boolean,
        quote: String?,
        delaySeconds: Int
    ) {
        val blockIntent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra("TARGET", target)
            putExtra("IS_WEBSITE", isWebsite)
            putExtra("QUOTE", quote)
            putExtra("DELAY_SECONDS", delaySeconds)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Send High Priority Full-Screen Intent Notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val lockNotification = NotificationCompat.Builder(this, LOCK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("App Blocked")
            .setContentText("Focus mode active")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(LOCK_NOTIFICATION_ID, lockNotification)

        // 2. Direct Activity launch
        try {
            pendingIntent.send()
        } catch (e: Exception) {
            try {
                startActivity(blockIntent)
            } catch (e2: Exception) {
                // Handled via FullScreenIntent
            }
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenHarmony Engine Active")
            .setContentText("App blocking active via Usage Access")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            // Service persistent channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Harmony Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // High-Priority Lock Overlay channel
            val lockChannel = NotificationChannel(
                LOCK_CHANNEL_ID,
                "Screen Harmony Lock Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(lockChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isMonitoring = false
    }
}
