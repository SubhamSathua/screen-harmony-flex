package com.prism.screenharmony.flex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.prism.screenharmony.flex.ui.theme.JetBrainsMonoFontFamily
import com.prism.screenharmony.flex.ui.theme.PlayfairFontFamily
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun BlocksPage(
    rules: List<BlockRule>,
    onToggleRule: (BlockRule, Boolean) -> Unit,
    onEditRule: (BlockRule) -> Unit,
    onDeleteRule: (BlockRule) -> Unit,
    onPauseRule: (BlockRule) -> Unit
) {
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
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())

        val activeRules = rules.filter { it.isCurrentlyBlocked(now, day) }
        val pausedRules = rules.filter { it.isEnabled && !it.isCurrentlyBlocked(now, day) }
        val disabledRules = rules.filter { !it.isEnabled }

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
                        onPause = { onPauseRule(rule) }
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
                        onPause = { onPauseRule(rule) }
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
                        onPause = { onPauseRule(rule) }
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
    onPause: () -> Unit
) {
    var showActiveDeleteConfirm by remember { mutableStateOf(false) }
    var showSimpleDeleteConfirm by remember { mutableStateOf(false) }
    var showDelayPauseDialog by remember { mutableStateOf(false) }
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
        onClick = onClick,
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
                            val remainingMillis = (rule.lastPausedAt ?: 0) + (rule.pauseDurationMinutes ?: 0) * 60 * 1000 - System.currentTimeMillis()
                            val remainingMins = (remainingMillis / (60 * 1000)).coerceAtLeast(0)
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
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onClick() },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (rule.isPaused()) "Unpause" else "Pause") },
                            onClick = {
                                showMenu = false
                                if (rule.isPaused()) {
                                    onPause()
                                } else if (isDelay) {
                                    showDelayPauseDialog = true
                                } else {
                                    onPause()
                                }
                            },
                            leadingIcon = { Icon(if (rule.isPaused()) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null) },
                            enabled = !isStrict && rule.isEnabled
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                if (isCurrentlyActive) {
                                    showActiveDeleteConfirm = true
                                } else {
                                    showSimpleDeleteConfirm = true
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            enabled = !isStrict
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
                            text = "Strict Mode: Cannot be paused, disabled, or deleted",
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!isStrict && rule.isEnabled) {
                    FilledTonalButton(
                        onClick = {
                            if (rule.isPaused()) {
                                onPause()
                            } else if (isDelay) {
                                showDelayPauseDialog = true
                            } else {
                                onPause()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(if (rule.isPaused()) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (rule.isPaused()) "Unpause" else "Pause", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Switch(
                    checked = rule.isEnabled,
                    enabled = !isStrict,
                    onCheckedChange = { newState ->
                        if (!newState && isDelay && rule.isEnabled) {
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

    if (showDelayPauseDialog) {
        DelayPauseWarningDialog(
            title = "Pause Block?",
            ruleName = rule.name,
            durationSeconds = delayDuration,
            actionLabel = "Confirm Pause",
            onConfirm = { onPause(); showDelayPauseDialog = false },
            onDismiss = { showDelayPauseDialog = false }
        )
    }

    if (showDelayToggleDialog) {
        DelayPauseWarningDialog(
            title = "Disable Block?",
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
            text = { Text("Are you sure you want to delete \"${rule.name.ifEmpty { "Unnamed Block" }}\"?") },
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
fun DelayPauseWarningDialog(
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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                    text = "\"${ruleName.ifEmpty { "Unnamed Block" }}\" has a ${durationSeconds}s reflection delay. Take a deep breath before deciding.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
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
                            fontFamily = if (isButtonEnabled) null else JetBrainsMonoFontFamily,
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

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Discipline is choosing between what you want now and what you want most.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontFamily = PlayfairFontFamily,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
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
                            fontFamily = if (timeLeft > 0) JetBrainsMonoFontFamily else null,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
