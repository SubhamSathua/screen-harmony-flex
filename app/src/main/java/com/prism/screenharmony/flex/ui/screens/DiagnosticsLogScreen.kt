package com.prism.screenharmony.flex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.diagnostics.AppLogger
import com.prism.screenharmony.flex.diagnostics.DiagnosticsUnlockManager
import com.prism.screenharmony.flex.diagnostics.LogCategory
import com.prism.screenharmony.flex.diagnostics.LogEntry
import com.prism.screenharmony.flex.diagnostics.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allLogs by AppLogger.logs.collectAsState()

    var selectedCategory by remember { mutableStateOf(LogCategory.ALL) }
    var selectedMinutes by remember { mutableStateOf<Int?>(null) } // null = All
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val timeOptions = remember {
        listOf(
            "All" to null,
            "1m" to 1,
            "2m" to 2,
            "3m" to 3,
            "5m" to 5,
            "7m" to 7,
            "10m" to 10,
            "20m" to 20,
            "30m" to 30
        )
    }

    val filteredLogs = remember(allLogs, selectedCategory, selectedMinutes, searchQuery) {
        val now = System.currentTimeMillis()
        val cutoff = if (selectedMinutes != null) now - (selectedMinutes!! * 60 * 1000L) else 0L

        allLogs.filter { entry ->
            val matchesTime = entry.timestamp >= cutoff
            val matchesCategory = (selectedCategory == LogCategory.ALL || entry.category == selectedCategory)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.tag.contains(searchQuery, ignoreCase = true) ||
                (entry.details?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesTime && matchesCategory && matchesSearch
        }
    }

    fun copyCurrentLogs() {
        val textToExport = AppLogger.exportLogs(filteredLogs)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("ScreenHarmony Logs", textToExport))
        val timeLabel = if (selectedMinutes != null) " (last ${selectedMinutes}m)" else ""
        Toast.makeText(context, "Copied ${filteredLogs.size} logs$timeLabel to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun exportAndShareLogs() {
        if (filteredLogs.isEmpty()) {
            Toast.makeText(context, "No logs to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val logsDir = File(context.cacheDir, "logs").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileSuffix = if (selectedMinutes != null) "_last_${selectedMinutes}m" else ""
            val logFile = File(logsDir, "screenharmony_diagnostics_${timeStamp}${fileSuffix}.txt")
            logFile.writeText(AppLogger.exportLogs(filteredLogs))

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScreenHarmony Flex Diagnostics Log")
                putExtra(Intent.EXTRA_TEXT, "Exported ScreenHarmony Diagnostics (${filteredLogs.size} entries)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Save or Share Diagnostics Log")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Diagnostics Logs?", fontWeight = FontWeight.Bold) },
            text = { Text("This will erase all telemetry, blocker, and sync logs currently buffered in memory.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppLogger.clear()
                        showClearDialog = false
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diagnostics & Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredLogs.size} of ${allLogs.size} events",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::copyCurrentLogs) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Logs")
                    }
                    IconButton(onClick = ::exportAndShareLogs) {
                        Icon(Icons.Rounded.Share, contentDescription = "Download / Share Logs")
                    }
                    if (!DiagnosticsUnlockManager.isAlwaysUnlocked()) {
                        IconButton(
                            onClick = {
                                DiagnosticsUnlockManager.setLogsUnlocked(context, false)
                                Toast.makeText(context, "Diagnostics Logs locked", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Lock Diagnostics")
                        }
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search logs, tags, errors...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Time Window Presets (last 1m, 2m, 3m, 5m, 7m, 10m, 20m, 30m, All)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Time Range:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(timeOptions) { (label, mins) ->
                        val isSelected = selectedMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = mins },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Category Filter Row
            ScrollableTabRow(
                selectedTabIndex = LogCategory.values().indexOf(selectedCategory),
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                indicator = {}
            ) {
                LogCategory.values().forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Logs List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No log records found for selection",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        LogCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val (badgeBg, badgeText) = when (log.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        LogLevel.WARN -> Color(0xFFFFE082) to Color(0xFF5D4037)
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        LogLevel.NETWORK -> Color(0xFFE1BEE7) to Color(0xFF4A148C)
        LogLevel.SYNC -> Color(0xFFB2DFDB) to Color(0xFF004D40)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (log.details != null) {
                    expanded = !expanded
                } else {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Log Entry", log.toExportString()))
                    Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Level Badge
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.level.label,
                            color = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Category Tag
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.category.label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = log.formattedTime(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (log.details != null) {
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedVisibility(visible = expanded) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                if (!expanded) {
                    Text(
                        text = "Tap to view stack trace / details...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
