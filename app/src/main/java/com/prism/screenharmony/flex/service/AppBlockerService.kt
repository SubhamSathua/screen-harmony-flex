package com.prism.screenharmony.flex.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.prism.screenharmony.flex.R
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.PauseType
import com.prism.screenharmony.flex.data.WallConfig
import com.prism.screenharmony.flex.diagnostics.AppLogger
import com.prism.screenharmony.flex.diagnostics.LogCategory
import com.prism.screenharmony.flex.ui.blocker.BlockedActivity
import com.prism.screenharmony.flex.utils.PermissionHelper
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AppBlockerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastQueryTime = System.currentTimeMillis() - 5_000L

    companion object {
        private const val TAG = "ScreenHarmony_Blocker"
        const val CHANNEL_ID = "screen_harmony_app_blocker_service"
        const val LOCK_CHANNEL_ID = "screen_harmony_lock_channel"
        const val NOTIFICATION_ID = 2001
        const val LOCK_NOTIFICATION_ID = 2002

        val isRunning = AtomicBoolean(false)
        @Volatile
        var lastInterceptedPackage: String? = null

        fun resetInterceptState() {
            lastInterceptedPackage = null
        }

        fun start(context: Context) {
            if (isRunning.get()) {
                return // Already running smoothly, avoid re-invoking startForegroundService
            }
            val intent = Intent(context, AppBlockerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "AppBlockerService start requested")
                AppLogger.i(LogCategory.BLOCKER, TAG, "AppBlockerService started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppBlockerService", e)
                AppLogger.e(LogCategory.BLOCKER, TAG, "Failed to start AppBlockerService", e)
            }
        }

        fun stop(context: Context) {
            if (!isRunning.get()) {
                return
            }
            try {
                val intent = Intent(context, AppBlockerService::class.java)
                context.stopService(intent)
                isRunning.set(false)
                Log.d(TAG, "AppBlockerService stop requested (idle state)")
                AppLogger.i(LogCategory.BLOCKER, TAG, "AppBlockerService stopped (0% background drain)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AppBlockerService", e)
                AppLogger.e(LogCategory.BLOCKER, TAG, "Failed to stop AppBlockerService", e)
            }
        }
    }

    private var currentActiveForegroundPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppBlockerService onCreate")
        BlockRepository.initialize(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppBlockerService onStartCommand")

        val notification = createForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isRunning.set(true)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
        }

        startAppMonitoringLoop()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "AppBlockerService onTaskRemoved. Re-evaluating schedule...")
        BlockScheduleManager.reschedule(this)
    }

    private var monitoringJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "AppBlockerService onDestroy.")
        isRunning.set(false)
        monitoringJob?.cancel()
        serviceScope.cancel()
    }

    private fun startAppMonitoringLoop() {
        if (monitoringJob?.isActive == true) {
            return // Already actively monitoring, prevent duplicated loops!
        }

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm == null) {
            Log.e(TAG, "UsageStatsManager service is NULL on this device")
            return
        }

        monitoringJob = serviceScope.launch {
            Log.i(TAG, "Starting optimized Usage Access monitoring loop (IO Dispatcher)")

            while (isActive) {
                try {
                    val hasUsage = PermissionHelper.isUsageAccessGranted(this@AppBlockerService)
                    if (hasUsage) {
                        val detectedForeground = getForegroundPackage(usm)
                        if (detectedForeground != null) {
                            currentActiveForegroundPackage = detectedForeground
                        }

                        val activeApp = currentActiveForegroundPackage
                        if (activeApp != null && activeApp != packageName) {
                            val matchingRule = BlockRepository.getActiveRuleForApp(activeApp)

                            if (matchingRule != null) {
                                if (lastInterceptedPackage != activeApp) {
                                    lastInterceptedPackage = activeApp
                                    Log.i(TAG, "🚨 BLOCKED APP DETECTED: '$activeApp' | Rule: '${matchingRule.name}'")

                                    val customQuote = if (matchingRule.wallConfig is WallConfig.StandardQuote) {
                                        (matchingRule.wallConfig as WallConfig.StandardQuote).quote
                                    } else null

                                    val delaySec = matchingRule.blockDurationSeconds

                                    executeBlockTakeover(
                                        targetPackage = activeApp,
                                        quote = customQuote,
                                        delaySeconds = delaySec
                                    )
                                }
                            } else {
                                // The active app is not blocked (e.g. Home Launcher, ScreenHarmony, or unblocked app)
                                // Reset lastInterceptedPackage so returning to a blocked app triggers immediately
                                lastInterceptedPackage = null
                            }
                        }
                    } else {
                        delay(2000)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                }

                delay(300) // 300ms lightweight poll
            }
        }
    }

    /**
     * Efficiently finds the active foreground app using a sliding window.
     */
    private var lastTakeoverNotificationTime = 0L

    private fun getForegroundPackage(usm: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val queryStart = (now - 5_000L).coerceAtMost(lastQueryTime)
        lastQueryTime = now

        val events = usm.queryEvents(queryStart, now)
        val event = UsageEvents.Event()

        var latestTime = 0L
        var latestApp: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if ((event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) && event.timeStamp >= latestTime) {
                latestTime = event.timeStamp
                latestApp = event.packageName
            }
        }

        if (latestApp == null) {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now)
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats.filter { it.packageName != packageName }.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && (now - mostRecent.lastTimeUsed) < 15_000L) {
                    latestApp = mostRecent.packageName
                }
            }
        }

        return latestApp
    }

    /**
     * Executes block takeover directly into BlockedActivity.
     */
    private fun executeBlockTakeover(
        targetPackage: String,
        quote: String?,
        delaySeconds: Int
    ) {
        Log.i(TAG, "Executing block takeover for $targetPackage")

        // Launch BlockedActivity directly on top
        val blockIntent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("TARGET", targetPackage)
            putExtra("IS_WEBSITE", false)
            putExtra("QUOTE", quote)
            putExtra("DELAY_SECONDS", delaySeconds)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 1. Post Full-Screen Intent Notification with a 2-second rate limiter to avoid sound spam
        val now = System.currentTimeMillis()
        if (now - lastTakeoverNotificationTime > 2000L) {
            lastTakeoverNotificationTime = now
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post lock notification", e)
            }
        }

        // 2. Start activity directly
        try {
            startActivity(blockIntent)
            Log.i(TAG, "BlockedActivity launched successfully")
        } catch (e: Exception) {
            try {
                pendingIntent.send()
            } catch (e2: Exception) {
                Log.e(TAG, "PendingIntent send failed", e2)
            }
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenHarmony Active")
            .setContentText("Enforcing app blocks in real time")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Harmony Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

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
}
