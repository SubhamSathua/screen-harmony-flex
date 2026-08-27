package com.prism.screenharmony.flex.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/**
 * Helper for handling days of the week as a bitmask.
 * MONDAY = 1, TUESDAY = 2, ..., SUNDAY = 64
 */
object DayBitmask {
    const val MONDAY = 1 shl 0
    const val TUESDAY = 1 shl 1
    const val WEDNESDAY = 1 shl 2
    const val THURSDAY = 1 shl 3
    const val FRIDAY = 1 shl 4
    const val SATURDAY = 1 shl 5
    const val SUNDAY = 1 shl 6

    const val ALL = (1 shl 7) - 1
    const val WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
    const val WEEKEND = SATURDAY or SUNDAY

    fun fromDays(days: Set<DayOfWeek>): Int {
        var mask = 0
        days.forEach { mask = mask or (1 shl (it.value - 1)) }
        return mask
    }

    fun toDays(mask: Int): Set<DayOfWeek> {
        val days = mutableSetOf<DayOfWeek>()
        for (i in 0..6) {
            if ((mask and (1 shl i)) != 0) {
                days.add(DayOfWeek.of(i + 1))
            }
        }
        return days
    }

    fun fromNames(dayNames: Set<String>): Int {
        val mapping = mapOf(
            "Mon" to MONDAY, "Tue" to TUESDAY, "Wed" to WEDNESDAY,
            "Thu" to THURSDAY, "Fri" to FRIDAY, "Sat" to SATURDAY, "Sun" to SUNDAY
        )
        var mask = 0
        dayNames.forEach { mask = mask or (mapping[it] ?: 0) }
        return mask
    }

    fun toNames(mask: Int): Set<String> {
        val mapping = mapOf(
            MONDAY to "Mon", TUESDAY to "Tue", WEDNESDAY to "Wed",
            THURSDAY to "Thu", FRIDAY to "Fri", SATURDAY to "Sat", SUNDAY to "Sun"
        )
        val names = mutableSetOf<String>()
        mapping.forEach { (m, name) ->
            if ((mask and m) != 0) names.add(name)
        }
        return names
    }
}

/**
 * Represents a time slot on specific days.
 */
data class TimeSlot(
    val id: String = UUID.randomUUID().toString(),
    val dayBitmask: Int = DayBitmask.ALL,
    val startMinute: Int = 0, // 0-1439
    val endMinute: Int = 1439 // 0-1439
) {
    fun isActive(now: LocalTime = LocalTime.now(), day: DayOfWeek = DayOfWeek.from(java.time.LocalDate.now())): Boolean {
        if ((dayBitmask and (1 shl (day.value - 1))) == 0) return false
        val currentMin = now.hour * 60 + now.minute
        return if (startMinute <= endMinute) {
            currentMin in startMinute..endMinute
        } else {
            // Over midnight support (e.g. 10 PM to 2 AM)
            currentMin >= startMinute || currentMin <= endMinute
        }
    }

    val startTime: LocalTime get() = LocalTime.of(startMinute / 60, startMinute % 60)
    val endTime: LocalTime get() = LocalTime.of(endMinute / 60, endMinute % 60)
}

/**
 * Blocking conditions.
 */
sealed class BlockCondition {
    abstract val id: String

    data class WeeklySchedule(
        override val id: String = UUID.randomUUID().toString(),
        val slots: List<TimeSlot> = emptyList()
    ) : BlockCondition()

    data class ScreenTimeLimit(
        override val id: String = UUID.randomUUID().toString(),
        val seconds: Int = 3600
    ) : BlockCondition()

    data class OpenLimit(
        override val id: String = UUID.randomUUID().toString(),
        val count: Int = 10
    ) : BlockCondition()
}

/**
 * Configuration for pausing a block.
 */
enum class PauseType {
    DELAY, STRICT, TYPE_TEXT, SCAN_QR, SCAN_NFC, PAUSABLE
}

data class PauseConfig(
    val type: PauseType = PauseType.DELAY,
    val extraValue: Int? = 10, // Default delay 10s
    val typeTextLength: Int = 5,
    val typeTextCount: Int = 3,
    val scannedCode: String? = null
)

/**
 * Configuration for the Block Wall overlay.
 */
sealed class WallConfig {
    data class StandardQuote(val quote: String? = null) : WallConfig()
    data object Emoji : WallConfig()
    data object Task : WallConfig()
}

enum class BlockType {
    HARD, SOFT, STRICT
}

/**
 * The primary entity for a blocking rule.
 */
data class BlockRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val isEnabled: Boolean = true,
    val selectedApps: Set<String> = emptySet(),
    val selectedWebsites: Set<String> = emptySet(),
    val blockAppLaunch: Boolean = true,
    val blockNotifications: Boolean = false,
    val pauseConfig: PauseConfig = PauseConfig(),
    val conditions: List<BlockCondition> = emptyList(),
    val blockType: BlockType = BlockType.HARD,
    val wallConfig: WallConfig = WallConfig.StandardQuote(),
    val lastPausedAt: Long? = null,
    val pauseDurationMinutes: Int? = null
) {
    val weeklySchedule: BlockCondition.WeeklySchedule?
        get() = conditions.filterIsInstance<BlockCondition.WeeklySchedule>().firstOrNull()

    val screenTimeLimit: BlockCondition.ScreenTimeLimit?
        get() = conditions.filterIsInstance<BlockCondition.ScreenTimeLimit>().firstOrNull()

    val openLimit: BlockCondition.OpenLimit?
        get() = conditions.filterIsInstance<BlockCondition.OpenLimit>().firstOrNull()

    fun isPaused(): Boolean {
        val pausedAt = lastPausedAt ?: return false
        val duration = pauseDurationMinutes ?: return false
        val now = System.currentTimeMillis()
        return now < pausedAt + (duration * 60 * 1000)
    }

    fun isCurrentlyBlocked(now: LocalTime = LocalTime.now(), day: DayOfWeek = DayOfWeek.from(java.time.LocalDate.now())): Boolean {
        if (!isEnabled || isPaused()) return false

        // If no schedule conditions are set, rule is always active
        if (conditions.isEmpty()) return true

        return conditions.any { condition ->
            when (condition) {
                is BlockCondition.WeeklySchedule -> {
                    condition.slots.isEmpty() || condition.slots.any { it.isActive(now, day) }
                }
                else -> false
            }
        }
    }
}
