package com.prism.screenharmony.flex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.data.*
import com.prism.screenharmony.flex.ui.components.*
import com.prism.screenharmony.flex.utils.formatDelay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBlockPage(
    rule: BlockRule,
    onRuleChanged: (BlockRule) -> Unit,
    onSelectApps: () -> Unit,
    onSave: () -> Unit,
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
                title = { Text(if (rule.name.isEmpty()) "Create Block" else rule.name, fontWeight = FontWeight.Bold) },
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
                onClick = onSave,
                interactionSource = interactionSource,
                modifier = Modifier
                    .padding(16.dp)
                    .scale(scale),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Rounded.Done, contentDescription = null) },
                text = { Text("Done", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
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
                    label = { Text("Block name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item { SectionHeader(title = "What to Block") }

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
                                Text(text = "Select Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (rule.selectedApps.isNotEmpty()) "${rule.selectedApps.size} apps selected" else "Select apps to block (Usage Access)",
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
                                    text = if (rule.selectedWebsites.isNotEmpty()) "${rule.selectedWebsites.size} websites selected" else "Select websites to block (Accessibility)",
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
                        subtitle = "Select days and active time periods",
                        icon = Icons.Rounded.Schedule,
                        onClick = { showAddScheduleSheet = true }
                    )
                }
            }

            item { SectionHeader(title = "Pause & Wall Options") }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Pause Option Row
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
                                        PauseType.STRICT -> "Strict (No Pausing Allowed)"
                                        PauseType.DELAY -> "Delay of ${formatDelay(rule.pauseConfig.extraValue ?: 10)}"
                                        PauseType.TYPE_TEXT -> "Type random text"
                                        PauseType.SCAN_QR -> "Scan QR / Barcode"
                                        PauseType.SCAN_NFC -> "Scan NFC tag"
                                        PauseType.PAUSABLE -> "Pausable instantly"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Quote Wall Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Motivational Quotes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (rule.wallConfig is WallConfig.StandardQuote) "Display quotes on lock screen" else "Direct lock without quotes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = rule.wallConfig is WallConfig.StandardQuote,
                                onCheckedChange = { isChecked ->
                                    onRuleChanged(
                                        rule.copy(wallConfig = if (isChecked) WallConfig.StandardQuote() else WallConfig.Emoji)
                                    )
                                }
                            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteBottomSheet(selectedWebsites: Set<String>, onWebsitesChanged: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newUrl by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isTextFieldFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isTextFieldFocused) {
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Websites", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = onDismiss, shape = CircleShape) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isTextFieldFocused = it.isFocused },
                    placeholder = { Text("e.g. instagram.com") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            onWebsitesChanged(selectedWebsites + newUrl.trim().lowercase())
                            newUrl = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                items(selectedWebsites.toList()) { url ->
                    ListItem(
                        headlineContent = { Text(url) },
                        trailingContent = {
                            IconButton(onClick = { onWebsitesChanged(selectedWebsites - url) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleBottomSheet(timeSlots: List<TimeSlot>, onTimeSlotsChanged: (List<TimeSlot>) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Weekly Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Rounded.Add, contentDescription = "Add Time") }
            }
            ScheduleGraph(timeSlots)
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                items(timeSlots) { slot ->
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                    ListItem(
                        headlineContent = { Text(DayBitmask.toNames(slot.dayBitmask).joinToString(", ")) },
                        supportingContent = { Text("${slot.startTime.format(formatter)} - ${slot.endTime.format(formatter)}") },
                        trailingContent = { IconButton(onClick = { onTimeSlotsChanged(timeSlots - slot) }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete") } }
                    )
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
    if (showAddDialog) {
        AddTimeDialog(onDismiss = { showAddDialog = false }, onAdd = { slot -> onTimeSlotsChanged(timeSlots + slot); showAddDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseOptionsBottomSheet(
    currentConfig: PauseConfig,
    onConfigSelected: (PauseConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewState by remember { mutableStateOf("LIST") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        when (viewState) {
            "DELAY" -> {
                DelaySliderView(
                    initialSeconds = currentConfig.extraValue ?: 10,
                    onSave = { seconds ->
                        onConfigSelected(PauseConfig(type = PauseType.DELAY, extraValue = seconds))
                        onDismiss()
                    },
                    onBack = { viewState = "LIST" }
                )
            }
            "TYPE_TEXT" -> {
                TypeTextConfigView(
                    initialLength = currentConfig.typeTextLength,
                    initialCount = currentConfig.typeTextCount,
                    onSave = { length, count ->
                        onConfigSelected(PauseConfig(type = PauseType.TYPE_TEXT, typeTextLength = length, typeTextCount = count))
                        onDismiss()
                    },
                    onBack = { viewState = "LIST" }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Pause Options", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            OptionCardX(
                                title = "Strict",
                                description = "No Pausing allowed (Recommended)",
                                icon = Icons.Rounded.Lock,
                                isSelected = currentConfig.type == PauseType.STRICT,
                                onClick = { onConfigSelected(PauseConfig(type = PauseType.STRICT)); onDismiss() }
                            )
                        }
                        item {
                            OptionCardX(
                                title = "Delay",
                                description = "Wait duration before unlocking (${formatDelay(currentConfig.extraValue ?: 10)})",
                                icon = Icons.Rounded.Timer,
                                isSelected = currentConfig.type == PauseType.DELAY,
                                onClick = { viewState = "DELAY" }
                            )
                        }
                        item {
                            OptionCardX(
                                title = "Type random text",
                                description = "Requires typing randomly generated text challenge",
                                icon = Icons.Rounded.TextFields,
                                isSelected = currentConfig.type == PauseType.TYPE_TEXT,
                                onClick = { viewState = "TYPE_TEXT" }
                            )
                        }
                        item {
                            OptionCardX(
                                title = "Pausable",
                                description = "Can be turned OFF easily without delay",
                                icon = Icons.Rounded.PauseCircle,
                                isSelected = currentConfig.type == PauseType.PAUSABLE,
                                onClick = { onConfigSelected(PauseConfig(type = PauseType.PAUSABLE)); onDismiss() }
                            )
                        }
                    }

                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleOrLimitBottomSheet(
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OptionCardX(
                title = "Weekly Schedule",
                description = AnnotatedString("Set specific days and times"),
                icon = Icons.Rounded.Schedule,
                isSelected = false,
                onClick = { onOptionSelected("weekly") }
            )

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun CreateBlockCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
