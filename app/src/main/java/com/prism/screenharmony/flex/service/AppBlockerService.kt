package com.prism.screenharmony.flex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import kotlinx.coroutines.*

class AppBlockerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isMonitoring = false
    private var lastInterceptedPackage: String? = null

    companion object {
        const val CHANNEL_ID = "screen_harmony_app_blocker"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

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
                        lastInterceptedPackage = null
                    }
                }
                delay(200) // Fast 200ms loop
            }
        }
    }

    private fun getForegroundPackage(usm: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000
        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var latestApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestApp = event.packageName
            }
        }
        return latestApp
    }

    private fun launchBlockWall(
        target: String,
        isWebsite: Boolean,
        quote: String?,
        delaySeconds: Int
    ) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra("TARGET", target)
            putExtra("IS_WEBSITE", isWebsite)
            putExtra("QUOTE", quote)
            putExtra("DELAY_SECONDS", delaySeconds)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenHarmony Active")
            .setContentText("Monitoring & blocking apps (Usage Access)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isMonitoring = false
    }
}
