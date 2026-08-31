package com.prism.screenharmony.flex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prism.screenharmony.flex.data.*
import com.prism.screenharmony.flex.ui.components.*
import com.prism.screenharmony.flex.utils.formatDelay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildBlockEditorScreen(
    childName: String,
    rule: BlockRule,
    onRuleChanged: (BlockRule) -> Unit,
    onSelectApps: () -> Unit,
    onSave: (BlockRule) -> Unit,
    onBack: () -> Unit
) {
    var showWebsiteSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showPauseOptionsSheet by remember { mutableStateOf(false) }
    var showAddScheduleSheet by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (rule.name.isEmpty()) "Create Parental Block" else rule.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enforcing on $childName's Phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = tween(durationMillis = 100),
                label = "DoneButtonScale"
            )

            ExtendedFloatingActionButton(
                onClick = { onSave(rule) },
                interactionSource = interactionSource,
                modifier = Modifier
                    .padding(16.dp)
                    .scale(scale),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Rounded.Done, contentDescription = null) },
                text = { Text("Save & Push Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = rule.name,
                    onValueChange = { onRuleChanged(rule.copy(name = it)) },
                    label = { Text("Block Name") },
                    placeholder = { Text("e.g. Focus & Study Time") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item { SectionHeader(title = "What to Block on $childName's Phone") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectApps() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Select Child Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (rule.selectedApps.isNotEmpty()) "${rule.selectedApps.size} apps selected" else "Select apps installed on $childName's phone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWebsiteSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Select Websites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (rule.selectedWebsites.isNotEmpty()) "${rule.selectedWebsites.size} websites selected" else "Select website domains to block",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            item { SectionHeader(title = "When to Block") }

            val schedule = rule.weeklySchedule
            if (schedule != null && schedule.slots.isNotEmpty()) {
                item {
                    WeeklyScheduleCard(timeSlots = schedule.slots, onClick = { showScheduleSheet = true })
                }
            } else {
                item {
                    CreateBlockCard(
                        title = "Add Schedule",
                        subtitle = "Select active days and time periods",
                        icon = Icons.Rounded.Schedule,
                        onClick = { showAddScheduleSheet = true }
                    )
                }
            }

            item { SectionHeader(title = "Pause & Strict Mode Restrictions") }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Pause Mode Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPauseOptionsSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (rule.pauseConfig.type) {
                                            PauseType.STRICT -> Icons.Rounded.Lock
                                            PauseType.DELAY -> Icons.Rounded.Timer
                                            PauseType.TYPE_TEXT -> Icons.Rounded.TextFields
                                            PauseType.SCAN_QR -> Icons.Rounded.QrCodeScanner
                                            PauseType.SCAN_NFC -> Icons.Rounded.Nfc
                                            PauseType.PAUSABLE -> Icons.Rounded.PauseCircle
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pause Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = when (rule.pauseConfig.type) {
                                        PauseType.STRICT -> "Strict (Child Cannot Pause)"
                                        PauseType.DELAY -> "Delay of ${formatDelay(rule.pauseConfig.extraValue ?: 10)}"
                                        PauseType.TYPE_TEXT -> "Type random text"
                                        PauseType.SCAN_QR -> "Scan QR code"
                                        PauseType.SCAN_NFC -> "Scan NFC tag"
                                        PauseType.PAUSABLE -> "Pausable by child"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Block Screen Duration Expandable Row
                        var isDurationExpanded by remember { mutableStateOf(false) }
                        val durationPresets = remember { listOf(3, 5, 7, 10, 15) }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDurationExpanded = !isDurationExpanded }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Block Wall Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Hold time before unlocking: ${rule.blockDurationSeconds}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${rule.blockDurationSeconds}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isDurationExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = isDurationExpanded,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    durationPresets.forEach { seconds ->
                                        val isSelected = rule.blockDurationSeconds == seconds
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onRuleChanged(rule.copy(blockDurationSeconds = seconds)) }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${seconds}s",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showWebsiteSheet) {
        WebsiteBottomSheet(
            selectedWebsites = rule.selectedWebsites,
            onWebsitesChanged = { onRuleChanged(rule.copy(selectedWebsites = it)) },
            onDismiss = { showWebsiteSheet = false }
        )
    }

    if (showScheduleSheet) {
        val currentSchedule = rule.weeklySchedule
        WeeklyScheduleBottomSheet(
            timeSlots = currentSchedule?.slots ?: emptyList(),
            onTimeSlotsChanged = { slots ->
                val newConditions = rule.conditions.filterNot { it is BlockCondition.WeeklySchedule } +
                        BlockCondition.WeeklySchedule(slots = slots)
                onRuleChanged(rule.copy(conditions = newConditions))
            },
            onDismiss = { showScheduleSheet = false }
        )
    }

    if (showPauseOptionsSheet) {
        PauseOptionsBottomSheet(
            currentConfig = rule.pauseConfig,
            onConfigSelected = { onRuleChanged(rule.copy(pauseConfig = it)) },
            onDismiss = { showPauseOptionsSheet = false }
        )
    }

    if (showAddScheduleSheet) {
        AddScheduleOrLimitBottomSheet(
            onOptionSelected = { option ->
                if (option == "weekly") {
                    val newConditions = rule.conditions + BlockCondition.WeeklySchedule(slots = emptyList())
                    onRuleChanged(rule.copy(conditions = newConditions))
                    showScheduleSheet = true
                }
                showAddScheduleSheet = false
            },
            onDismiss = { showAddScheduleSheet = false }
        )
    }
}
