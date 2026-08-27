package com.prism.screenharmony.flex.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

data class TimeSlot(
    val startMinute: Int = 0,    // 0 - 1439 (e.g., 9:00 AM = 540)
    val endMinute: Int = 1439,   // 0 - 1439 (e.g., 5:00 PM = 1020)
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet()
) {
    fun isCurrentTimeActive(now: LocalTime = LocalTime.now(), day: DayOfWeek = DayOfWeek.from(java.time.LocalDate.now())): Boolean {
        if (!activeDays.contains(day)) return false
        val currentMin = now.hour * 60 + now.minute
        return if (startMinute <= endMinute) {
            currentMin in startMinute..endMinute
        } else {
            // Over midnight support (e.g., 10 PM to 6 AM)
            currentMin >= startMinute || currentMin <= endMinute
        }
    }
}

data class BlockRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "App Block",
    val isEnabled: Boolean = true,
    val selectedApps: Set<String> = emptySet(),        // Package names (Usage Access)
    val selectedWebsites: Set<String> = emptySet(),    // Domains (Accessibility)
    val showQuotes: Boolean = false,                   // Quote Wall ON/OFF toggle per block
    val timeSlot: TimeSlot? = null,                    // Null = Always active
    val pauseDelaySeconds: Int = 0,                    // 0 = Strict (No pausing), >0 = Wait Xs delay
    val lastPausedUntil: Long? = null                  // Timestamp in millis
) {
    fun isPaused(): Boolean {
        val until = lastPausedUntil ?: return false
        return System.currentTimeMillis() < until
    }

    fun isCurrentlyActive(now: LocalTime = LocalTime.now(), day: DayOfWeek = DayOfWeek.from(java.time.LocalDate.now())): Boolean {
        if (!isEnabled || isPaused()) return false
        val slot = timeSlot ?: return true
        return slot.isCurrentTimeActive(now, day)
    }
}
