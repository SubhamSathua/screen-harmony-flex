package com.prism.screenharmony.flex.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.prism.screenharmony.flex.data.BlockCondition
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.BlockRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Intelligent Event-Driven Background LifeCycle & Schedule Manager
 * 
 * Ensures the app only runs AppBlockerService in the background when an active block
 * rule is in effect. At all other times, the service is stopped (0% CPU, 0% battery),
 * and an exact alarm wakes the app precisely when the next block period begins.
 */
object BlockScheduleManager {

    private const val TAG = "ScreenHarmony_Scheduler"
    private const val REQUEST_CODE_SCHEDULE = 3001

    fun reschedule(context: Context) {
        val appContext = context.applicationContext
        BlockRepository.initialize(appContext)
        BlockRepository.cleanExpiredPauses()

        val rules = BlockRepository.rules.value
        val nowTime = LocalTime.now()
        val today = DayOfWeek.from(LocalDate.now())

        // 1. Check if any rule is actively blocking right now
        val activeRules = rules.filter { it.isEnabled && it.isCurrentlyBlocked(nowTime, today) }
        val hasActiveRuleNow = activeRules.isNotEmpty()

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (hasActiveRuleNow) {
            Log.i(TAG, "🟢 Active rules detected (${activeRules.size} rules). Starting Blocker Service...")
            AppBlockerService.start(appContext)

            // Calculate when the current active block window will end or pause will expire
            val nextEndMillis = calculateEarliestActiveEndMillis(activeRules)
            val nextPauseExpiryMillis = calculateEarliestPauseExpiryMillis(rules)

            val triggerTimes = listOfNotNull(nextEndMillis, nextPauseExpiryMillis)
            val earliestEvent = triggerTimes.minOrNull()

            if (earliestEvent != null && alarmManager != null) {
                scheduleExactWakeup(appContext, alarmManager, earliestEvent, "End of current block window")
            }
        } else {
            Log.i(TAG, "⚪ No active rules right now. Stopping Blocker Service (0% Idle CPU/Battery)...")
            AppBlockerService.stop(appContext)

            // Calculate when the NEXT block window will start or when a paused rule resumes
            val nextStartMillis = calculateNextUpcomingStartMillis(rules)
            val nextPauseExpiryMillis = calculateEarliestPauseExpiryMillis(rules)

            val triggerTimes = listOfNotNull(nextStartMillis, nextPauseExpiryMillis)
            val earliestUpcomingEvent = triggerTimes.minOrNull()

            if (earliestUpcomingEvent != null && alarmManager != null) {
                scheduleExactWakeup(appContext, alarmManager, earliestUpcomingEvent, "Start of next block window")
            } else {
                // Cancel any pending alarms if there are no future rules
                alarmManager?.let { cancelAlarm(appContext, it) }
                Log.i(TAG, "No upcoming block events found. All alarms cleared.")
            }
        }
    }

    private fun scheduleExactWakeup(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        reason: String
    ) {
        val intent = Intent(context, BlockAlarmReceiver::class.java).apply {
            action = BlockAlarmReceiver.ACTION_EVALUATE_SCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val secondsUntil = ((triggerAtMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
        Log.i(TAG, "⏰ Scheduling exact alarm for '$reason' in ${secondsUntil}s (at $triggerAtMillis)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm. Falling back to non-exact set().", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (err: Exception) {
                Log.e(TAG, "AlarmManager set failed", err)
            }
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, BlockAlarmReceiver::class.java).apply {
            action = BlockAlarmReceiver.ACTION_EVALUATE_SCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SCHEDULE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Calculates the earliest epoch millisecond when the currently active block rules finish.
     */
    private fun calculateEarliestActiveEndMillis(activeRules: List<BlockRule>): Long? {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        var earliestEnd: Long? = null

        for (rule in activeRules) {
            val weekly = rule.weeklySchedule
            if (weekly == null || weekly.slots.isEmpty()) {
                // Rule has no time schedule -> It is 24/7 active (infinite)
                continue
            }

            for (slot in weekly.slots) {
                val currentMin = now.hour * 60 + now.minute
                val todayMask = 1 shl (now.dayOfWeek.value - 1)

                if ((slot.dayBitmask and todayMask) != 0) {
                    if (slot.startMinute <= slot.endMinute) {
                        if (currentMin in slot.startMinute..slot.endMinute) {
                            val endDateTime = now.toLocalDate().atTime(slot.endMinute / 60, slot.endMinute % 60, 59)
                            val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
                            if (earliestEnd == null || endMillis < earliestEnd) {
                                earliestEnd = endMillis
                            }
                        }
                    } else {
                        // Spans midnight: ends early tomorrow at endMinute
                        if (currentMin >= slot.startMinute) {
                            val endDateTime = now.toLocalDate().plusDays(1).atTime(slot.endMinute / 60, slot.endMinute % 60, 59)
                            val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
                            if (earliestEnd == null || endMillis < earliestEnd) {
                                earliestEnd = endMillis
                            }
                        }
                    }
                }
            }
        }
        return earliestEnd
    }

    /**
     * Calculates the nearest epoch millisecond in the future when a rule will become active.
     */
    private fun calculateNextUpcomingStartMillis(rules: List<BlockRule>): Long? {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val currentMillis = System.currentTimeMillis()
        var nextStart: Long? = null

        // Scan upcoming 7 days
        for (dayOffset in 0..7) {
            val checkDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val dayOfWeek = checkDate.dayOfWeek
            val dayMask = 1 shl (dayOfWeek.value - 1)

            for (rule in rules) {
                if (!rule.isEnabled) continue

                val weekly = rule.weeklySchedule
                if (weekly != null && weekly.slots.isNotEmpty()) {
                    for (slot in weekly.slots) {
                        if ((slot.dayBitmask and dayMask) != 0) {
                            val startDateTime = checkDate.atTime(slot.startMinute / 60, slot.startMinute % 60, 0)
                            val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()

                            // Must be in the future
                            if (startMillis > currentMillis + 1000L) {
                                if (nextStart == null || startMillis < nextStart) {
                                    nextStart = startMillis
                                }
                            }
                        }
                    }
                }
            }
        }
        return nextStart
    }

    /**
     * Calculates when any currently paused rule's pause duration expires.
     */
    private fun calculateEarliestPauseExpiryMillis(rules: List<BlockRule>): Long? {
        val currentMillis = System.currentTimeMillis()
        var earliestExpiry: Long? = null

        for (rule in rules) {
            val pausedAt = rule.lastPausedAt
            val durationMinutes = rule.pauseDurationMinutes
            if (rule.isEnabled && pausedAt != null && durationMinutes != null) {
                val expiryMillis = pausedAt + (durationMinutes * 60 * 1000L)
                if (expiryMillis > currentMillis) {
                    if (earliestExpiry == null || expiryMillis < earliestExpiry) {
                        earliestExpiry = expiryMillis
                    }
                }
            }
        }
        return earliestExpiry
    }
}
