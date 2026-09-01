package com.prism.screenharmony.flex.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.data.PauseType
import com.prism.screenharmony.flex.ui.components.ConditionBadge
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun BlocksPage(
    rules: List<BlockRule>,
    onToggleRule: (BlockRule, Boolean) -> Unit,
    onEditRule: (BlockRule) -> Unit,
    onDeleteRule: (BlockRule) -> Unit,
    onPauseRule: (BlockRule, Int) -> Unit,
    isOnlyParentMode: Boolean = false,
    onTurnOffOnlyParentMode: () -> Unit = {}
) {
    if (isOnlyParentMode) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "OFF MODE • ONLY PARENT ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Local Blocker is Off",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This device is set to 'Only Parent Mode'. Local blocking is turned off with 0% background battery drain, operating purely as a parental monitor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onTurnOffOnlyParentMode,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.ToggleOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn OFF 'Only Parent Mode'", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }
    if (rules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "No Active Blocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap '+ Create a Block' to setup apps or websites to block",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                currentTimeMillis = System.currentTimeMillis()
                com.prism.screenharmony.flex.data.BlockRepository.cleanExpiredPauses()
            }
        }

        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())

        val activeRules = remember(rules, currentTimeMillis) {
            rules.filter { it.isEnabled && !it.isPaused() && it.isCurrentlyBlocked(now, day) }
        }
        val pausedRules = remember(rules, currentTimeMillis) {
            rules.filter { it.isEnabled && it.isPaused() }
        }
        val inactiveRules = remember(rules, currentTimeMillis) {
            rules.filter { it.isEnabled && !it.isPaused() && !it.isCurrentlyBlocked(now, day) }
        }
        val disabledRules = remember(rules, currentTimeMillis) {
            rules.filter { !it.isEnabled }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeRules.isNotEmpty()) {
                item {
                    Text(
                        "Active (${activeRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
                items(activeRules, key = { it.id }) { rule ->
                    BlockCardX(
                        rule = rule,
                        onToggle = { onToggleRule(rule, it) },
                        onClick = { onEditRule(rule) },
                        onDelete = { onDeleteRule(rule) },
                        onPause = { duration -> onPauseRule(rule, duration) }
                    )
                }
            }

            if (pausedRules.isNotEmpty()) {
                item {
                    Text(
                        "Paused (${pausedRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(pausedRules, key = { it.id }) { rule ->
                    BlockCardX(
                        rule = rule,
                        onToggle = { onToggleRule(rule, it) },
                        onClick = { onEditRule(rule) },
                        onDelete = { onDeleteRule(rule) },
                        onPause = { duration -> onPauseRule(rule, duration) }
                    )
                }
            }

            if (inactiveRules.isNotEmpty()) {
                item {
                    Text(
                        "Block Inactive (${inactiveRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(inactiveRules, key = { it.id }) { rule ->
                    BlockCardX(
                        rule = rule,
                        onToggle = { onToggleRule(rule, it) },
                        onClick = { onEditRule(rule) },
                        onDelete = { onDeleteRule(rule) },
                        onPause = { duration -> onPauseRule(rule, duration) }
                    )
                }
            }

            if (disabledRules.isNotEmpty()) {
                item {
                    Text(
                        "Disabled (${disabledRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                items(disabledRules, key = { it.id }) { rule ->
                    BlockCardX(
                        rule = rule,
                        onToggle = { onToggleRule(rule, it) },
                        onClick = { onEditRule(rule) },
                        onDelete = { onDeleteRule(rule) },
                        onPause = { duration -> onPauseRule(rule, duration) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
fun BlockCardX(
    rule: BlockRule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPause: (Int) -> Unit,
    isParentSide: Boolean = false
) {
    var showActiveDeleteConfirm by remember { mutableStateOf(false) }
    var showSimpleDeleteConfirm by remember { mutableStateOf(false) }
    var showDelayPauseDialog by remember { mutableStateOf(false) }
    var showParentPauseDialog by remember { mutableStateOf(false) }
    var showDelayToggleDialog by remember { mutableStateOf(false) }
    var pendingToggleValue by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val isStrict = rule.pauseConfig.type == PauseType.STRICT
    val isDelay = rule.pauseConfig.type == PauseType.DELAY
    val delayDuration = rule.pauseConfig.extraValue ?: 10

    val now = LocalTime.now()
    val day = DayOfWeek.from(java.time.LocalDate.now())
    val isCurrentlyActive = rule.isEnabled && rule.isCurrentlyBlocked(now, day)

    Card(
        onClick = if (isStrict) { {} } else onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                if (isCurrentlyActive) MaterialTheme.colorScheme.surfaceContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (rule.isEnabled) {
                        if (isCurrentlyActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    } else MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isStrict -> Icons.Rounded.Lock
                                !rule.isEnabled -> Icons.Rounded.Block
                                isCurrentlyActive -> Icons.Rounded.Shield
                                else -> Icons.Rounded.ShieldMoon
                            },
                            contentDescription = null,
                            tint = when {
                                isStrict -> MaterialTheme.colorScheme.error
                                rule.isEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (rule.name.isEmpty()) "Unnamed Block" else rule.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isStrict) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "STRICT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (rule.isPaused()) {
                            val remainingMillis = (rule.lastPausedAt ?: 0) + (rule.pauseDurationMinutes ?: 0) * 60 * 1000L - System.currentTimeMillis()
                            val remainingMins = (remainingMillis / (60 * 1000L)).coerceAtLeast(0)
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${remainingMins}m", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Text(
                        text = "${rule.selectedApps.size} apps · ${rule.selectedWebsites.size} websites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", fontWeight = FontWeight.Medium) },
                            onClick = { showMenu = false; onClick() },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            enabled = !isStrict,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        )
                        DropdownMenuItem(
                            text = { Text(if (rule.isPaused()) "Unpause" else "Pause", fontWeight = FontWeight.Medium) },
                            onClick = {
                                showMenu = false
                                if (rule.isPaused()) {
                                    onPause(0)
                                } else if (isParentSide) {
                                    showParentPauseDialog = true
                                } else {
                                    showDelayPauseDialog = true
                                }
                            },
                            leadingIcon = { Icon(if (rule.isPaused()) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null) },
                            enabled = !isStrict && rule.isEnabled && (isCurrentlyActive || rule.isPaused()),
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", fontWeight = FontWeight.Medium, color = if (!isStrict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) },
                            onClick = {
                                showMenu = false
                                if (isParentSide) {
                                    showSimpleDeleteConfirm = true
                                } else if (isCurrentlyActive) {
                                    showActiveDeleteConfirm = true
                                } else {
                                    showSimpleDeleteConfirm = true
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = if (!isStrict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) },
                            enabled = !isStrict,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            if (isStrict) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict Mode: Editing, pausing, and deleting are disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (rule.conditions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rule.weeklySchedule?.let { schedule ->
                        ConditionBadge(
                            icon = Icons.Rounded.Schedule,
                            text = if (schedule.slots.isEmpty()) "Always Active" else "${schedule.slots.size} scheduled slots"
                        )
                    }
                }
            }

            if (isParentSide && rule.isPaused()) {
                val durationMins = rule.pauseDurationMinutes ?: 15
                val remainingMillis = (rule.lastPausedAt ?: 0) + durationMins * 60 * 1000L - System.currentTimeMillis()
                val remainingMins = (remainingMillis / (60 * 1000L)).coerceAtLeast(0)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PauseCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Child has paused for $durationMins min",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$remainingMins min remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onPause(0) },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("Unpause", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!isStrict && rule.isEnabled) {
                    FilledTonalButton(
                        onClick = {
                            if (rule.isPaused()) {
                                onPause(0)
                            } else if (isParentSide) {
                                showParentPauseDialog = true
                            } else {
                                showDelayPauseDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(if (rule.isPaused()) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rule.isPaused()) "Unpause" else "Pause",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Switch(
                    checked = rule.isEnabled,
                    enabled = !isStrict,
                    onCheckedChange = { newState ->
                        if (!isParentSide && !newState && isDelay && rule.isEnabled) {
                            pendingToggleValue = false
                            showDelayToggleDialog = true
                        } else {
                            onToggle(newState)
                        }
                    }
                )
            }
        }
    }

    if (showParentPauseDialog) {
        SimplePauseDurationDialog(
            ruleName = rule.name,
            onConfirm = { selectedDuration ->
                onPause(selectedDuration)
                showParentPauseDialog = false
            },
            onDismiss = { showParentPauseDialog = false }
        )
    }

    if (showDelayPauseDialog) {
        DelayPauseWarningDialog(
            title = "Pause Block",
            ruleName = rule.name,
            durationSeconds = if (isDelay) delayDuration else 3,
            onConfirm = { selectedDuration ->
                onPause(selectedDuration)
                showDelayPauseDialog = false
            },
            onDismiss = { showDelayPauseDialog = false }
        )
    }

    if (showDelayToggleDialog) {
        DelayToggleWarningDialog(
            title = "Turn Off Block",
            ruleName = rule.name,
            durationSeconds = delayDuration,
            actionLabel = "Turn Off Block",
            onConfirm = { onToggle(pendingToggleValue); showDelayToggleDialog = false },
            onDismiss = { showDelayToggleDialog = false }
        )
    }

    if (showActiveDeleteConfirm) {
        ActiveBlockActionDialog(
            title = "Delete Active Block?",
            ruleName = rule.name,
            onConfirm = { onDelete(); showActiveDeleteConfirm = false },
            onDismiss = { showActiveDeleteConfirm = false }
        )
    }

    if (showSimpleDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showSimpleDeleteConfirm = false },
            title = { Text("Delete Block?") },
            text = { Text("Are you sure you want to delete '${rule.name}'?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showSimpleDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimpleDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SimplePauseDurationDialog(
    ruleName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPauseMinutes by remember { mutableIntStateOf(15) }
    val quickPresets = remember { listOf(5, 10, 15, 30, 45, 60, 120) }

    val formattedDuration = when {
        selectedPauseMinutes == 1 -> "1 Minute"
        selectedPauseMinutes == 60 -> "1 Hour"
        selectedPauseMinutes == 120 -> "2 Hours"
        selectedPauseMinutes > 60 && selectedPauseMinutes % 60 == 0 -> "${selectedPauseMinutes / 60} Hours"
        selectedPauseMinutes > 60 -> "${selectedPauseMinutes / 60}h ${selectedPauseMinutes % 60}m"
        else -> "$selectedPauseMinutes Minutes"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Pause Block") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select pause duration for \"${ruleName.ifEmpty { "Block Rule" }}\":",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Large Display Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                // Range Slider (1 min to 120 mins)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = selectedPauseMinutes.toFloat(),
                        onValueChange = { selectedPauseMinutes = it.roundToInt().coerceIn(1, 120) },
                        valueRange = 1f..120f,
                        steps = 118,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Max: 2 hours", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Quick Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickPresets.forEach { preset ->
                        val isSelected = selectedPauseMinutes == preset
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { selectedPauseMinutes = preset }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (preset >= 60) "${preset / 60}h" else "${preset}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedPauseMinutes)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Pause ($formattedDuration)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DelayPauseWarningDialog(
    title: String,
    ruleName: String,
    durationSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val quoteItem = remember { com.prism.screenharmony.flex.data.QuoteProvider.getRandomQuote() }

    var timeLeft by remember { mutableIntStateOf(durationSeconds) }
    val progressAnimatable = remember { Animatable(0f) }
    var isTimerFinished by remember { mutableStateOf(false) }

    // Range slider pause duration (1min to 60min ONLY)
    var selectedPauseMinutes by remember { mutableIntStateOf(5) }
    val quickPresets = remember { listOf(1, 5, 10, 15, 30, 45, 60) }

    LaunchedEffect(durationSeconds) {
        progressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationSeconds * 1000, easing = LinearEasing)
        )
    }

    LaunchedEffect(durationSeconds) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        isTimerFinished = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (!isTimerFinished) Icons.Rounded.SelfImprovement else Icons.Rounded.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = if (!isTimerFinished) title else "Select Pause Duration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!isTimerFinished) {
                    Text(
                        text = "Take a moment to reflect before pausing \"${ruleName.ifEmpty { "Unnamed Block" }}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quote Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${quoteItem.quote}\"",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = com.prism.screenharmony.flex.ui.theme.PlayfairFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    lineHeight = 24.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (quoteItem.author.isNotEmpty() && quoteItem.author != "Unknown") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "— ${quoteItem.author}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnimatable.value)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                } else {
                    // STAGE 2: DURATION SLIDER (1min to 60min ONLY)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = when (selectedPauseMinutes) {
                                    60 -> "1 Hour"
                                    1 -> "1 Minute"
                                    else -> "$selectedPauseMinutes Minutes"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        Slider(
                            value = selectedPauseMinutes.toFloat(),
                            onValueChange = { selectedPauseMinutes = it.roundToInt().coerceIn(1, 60) },
                            valueRange = 1f..60f,
                            steps = 58,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Max: 1 hour", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // Quick Presets Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickPresets.forEach { preset ->
                                val isSelected = selectedPauseMinutes == preset
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPauseMinutes = preset }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (preset == 60) "1h" else "${preset}m",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isTimerFinished) {
                                onConfirm(selectedPauseMinutes)
                            }
                        },
                        enabled = isTimerFinished,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isTimerFinished) "Pause" else "Wait ${timeLeft}s",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DelayToggleWarningDialog(
    title: String,
    ruleName: String,
    durationSeconds: Int,
    actionLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(durationSeconds) }
    val progressAnimatable = remember { Animatable(0f) }

    LaunchedEffect(durationSeconds) {
        progressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationSeconds * 1000, easing = LinearEasing)
        )
    }

    LaunchedEffect(durationSeconds) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val isButtonEnabled = timeLeft <= 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Take a moment to reflect before disabling \"${ruleName.ifEmpty { "Unnamed Block" }}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quote Card
                val quoteItem = remember { com.prism.screenharmony.flex.data.QuoteProvider.getRandomQuote() }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${quoteItem.quote}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.prism.screenharmony.flex.ui.theme.PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (quoteItem.author.isNotEmpty() && quoteItem.author != "Unknown") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— ${quoteItem.author}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimatable.value)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = isButtonEnabled,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isButtonEnabled) actionLabel else "Wait ${timeLeft}s",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveBlockActionDialog(
    title: String,
    ruleName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }
    val quoteItem = remember { com.prism.screenharmony.flex.data.QuoteProvider.getRandomQuote() }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = ruleName.ifEmpty { "Unnamed Block" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${quoteItem.quote}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.prism.screenharmony.flex.ui.theme.PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (quoteItem.author.isNotEmpty() && quoteItem.author != "Unknown") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— ${quoteItem.author}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = timeLeft == 0,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (timeLeft > 0) "Wait ${timeLeft}s" else "Delete",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
