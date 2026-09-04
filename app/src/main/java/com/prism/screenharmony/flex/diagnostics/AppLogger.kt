package com.prism.screenharmony.flex.diagnostics

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel(val label: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    NETWORK("NET"),
    SYNC("SYNC")
}

enum class LogCategory(val label: String) {
    ALL("All"),
    NETWORK("Network"),
    BLOCKER("Blocker"),
    ACCESSIBILITY("Accessibility"),
    SECURITY("Security"),
    SYSTEM("System")
}

data class LogEntry(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: LogCategory,
    val tag: String,
    val message: String,
    val details: String? = null
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun toExportString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val dt = sdf.format(Date(timestamp))
        val det = if (details != null) "\n  └─ Details: $details" else ""
        return "[$dt] [${level.label}] [${category.label}] [$tag]: $message$det"
    }
}

object AppLogger {

    private const val MAX_LOGS = 400
    private val idCounter = AtomicLong(1)
    private val logBuffer = ArrayDeque<LogEntry>(MAX_LOGS + 10)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    @Synchronized
    private fun appendLog(
        level: LogLevel,
        category: LogCategory,
        tag: String,
        message: String,
        details: String? = null
    ) {
        val entry = LogEntry(
            id = idCounter.getAndIncrement(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            details = details
        )

        logBuffer.addFirst(entry)
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.removeLast()
        }
        _logs.value = logBuffer.toList()

        // Also output to standard Android Logcat
        when (level) {
            LogLevel.INFO -> Log.i(tag, "[${category.label}] $message")
            LogLevel.WARN -> Log.w(tag, "[${category.label}] $message")
            LogLevel.ERROR -> Log.e(tag, "[${category.label}] $message ${details ?: ""}")
            LogLevel.NETWORK -> Log.d(tag, "[NET] $message")
            LogLevel.SYNC -> Log.d(tag, "[SYNC] $message")
        }
    }

    fun i(category: LogCategory, tag: String, message: String, details: String? = null) {
        appendLog(LogLevel.INFO, category, tag, message, details)
    }

    fun w(category: LogCategory, tag: String, message: String, details: String? = null) {
        appendLog(LogLevel.WARN, category, tag, message, details)
    }

    fun e(category: LogCategory, tag: String, message: String, throwable: Throwable? = null) {
        val details = throwable?.stackTraceToString()
        appendLog(LogLevel.ERROR, category, tag, message, details)
    }

    fun net(tag: String, message: String, details: String? = null) {
        appendLog(LogLevel.NETWORK, LogCategory.NETWORK, tag, message, details)
    }

    fun sync(tag: String, message: String, details: String? = null) {
        appendLog(LogLevel.SYNC, LogCategory.NETWORK, tag, message, details)
    }

    fun clear() {
        synchronized(this) {
            logBuffer.clear()
            _logs.value = emptyList()
        }
    }

    fun exportLogs(logsToExport: List<LogEntry>): String {
        if (logsToExport.isEmpty()) return "No logs recorded for selected criteria."
        return logsToExport.reversed().joinToString("\n") { it.toExportString() }
    }

    fun exportAll(): String {
        return exportLogs(_logs.value)
    }
}
