package com.prism.screenharmony.flex.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.family.ChildAppUsage
import com.prism.screenharmony.flex.family.FamilySyncManager
import com.prism.screenharmony.flex.family.RemoteChildDevice
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDeviceDetailScreen(
    device: RemoteChildDevice,
    onBack: () -> Unit,
    onDeviceRemoved: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Block", "Analysis", "Controls")

    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }

    // Remote Block Rule Creation & Editing Flow
    var editingRule by remember { mutableStateOf<BlockRule?>(null) }
    var isSelectingApps by remember { mutableStateOf(false) }
    var isAppListGridView by remember { mutableStateOf(false) }

    // Live Rules, Installed Apps, and Telemetry for this Child Device
    var childRules by remember { mutableStateOf<List<BlockRule>>(emptyList()) }
    var childInstalledApps by remember { mutableStateOf<List<com.prism.screenharmony.flex.family.ChildAppInfo>>(emptyList()) }
    var childAppsUsage by remember { mutableStateOf<List<ChildAppUsage>>(emptyList()) }

    DisposableEffect(device.deviceId) {
        val ruleListener = FamilySyncManager.listenChildRules(device.deviceId) { rules ->
            childRules = rules
        }
        val appUsageListener = FamilySyncManager.listenChildScreenTimeApps(device.deviceId) { apps ->
            childAppsUsage = apps
        }
        val installedAppsListener = FamilySyncManager.listenChildInstalledApps(device.deviceId) { apps ->
            childInstalledApps = apps
        }
        onDispose {
            if (ruleListener != null) {
                FamilySyncManager.removeRulesListener(device.deviceId, ruleListener)
            }
            if (appUsageListener != null) {
                FamilySyncManager.removeScreenTimeListener(device.deviceId, appUsageListener)
            }
            if (installedAppsListener != null) {
                FamilySyncManager.removeInstalledAppsListener(device.deviceId, installedAppsListener)
            }
        }
    }

    // Dedicated Full-screen Child App Selection for Remote Block Rule
    if (isSelectingApps && editingRule != null) {
        ChildAppListScreen(
            childName = device.displayName,
            installedApps = childInstalledApps,
            selectedApps = editingRule!!.selectedApps,
            isGridView = isAppListGridView,
            onViewToggle = { isAppListGridView = it },
            onRefresh = {
                FamilySyncManager.requestChildAppSync(device.deviceId) { success ->
                    Toast.makeText(context, if (success) "Requested app scan from ${device.displayName}" else "Failed to send request", Toast.LENGTH_SHORT).show()
                }
            },
            onDone = { selected ->
                editingRule = editingRule!!.copy(selectedApps = selected)
                isSelectingApps = false
            },
            onBack = { isSelectingApps = false }
        )
        return
    }

    // Dedicated Full-screen Child Block Editor Flow (Completely isolated from local blocker)
    if (editingRule != null) {
        ChildBlockEditorScreen(
            childName = device.displayName,
            rule = editingRule!!,
            onRuleChanged = { editingRule = it },
            onSelectApps = { isSelectingApps = true },
            onSave = { ruleToSave ->
                FamilySyncManager.pushRuleToChild(device.deviceId, ruleToSave)
                editingRule = null
                Toast.makeText(context, "Rule saved & pushed to ${device.displayName}!", Toast.LENGTH_SHORT).show()
            },
            onBack = { editingRule = null }
        )
        return
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = device.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.model.ifBlank { "Android Device" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            FamilySyncManager.forceRefresh(context) { success, msg ->
                                Toast.makeText(context, if (success) "Data refreshed successfully" else "Refresh failed: $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Device", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    showRenameDialog = true
                                },
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Device", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showRemoveDialog = true
                                },
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.92f else 1f,
                    animationSpec = tween(durationMillis = 100),
                    label = "CreateBlockFabScale"
                )

                ExtendedFloatingActionButton(
                    onClick = {
                        editingRule = BlockRule(
                            id = "remote_" + UUID.randomUUID().toString().take(8),
                            name = ""
                        )
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .padding(16.dp)
                        .scale(scale),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Create a Block", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // M3 Expressive Secondary Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Rounded.Shield
                                        1 -> Icons.Rounded.Analytics
                                        else -> Icons.Rounded.Tune
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    isPullRefreshing = true
                    FamilySyncManager.forceRefresh(context) { success, msg ->
                        isPullRefreshing = false
                        Toast.makeText(context, if (success) "Data refreshed successfully" else "Refresh failed: $msg", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    label = "TabContentAnimation"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ChildBlocksTabContent(
                            device = device,
                            rules = childRules,
                            onToggleRule = { rule, isEnabled ->
                                FamilySyncManager.toggleRuleOnChild(device.deviceId, rule, isEnabled)
                            },
                            onEditRule = { rule ->
                                editingRule = rule
                            },
                            onDeleteRule = { rule ->
                                FamilySyncManager.deleteRuleOnChild(device.deviceId, rule.id)
                                Toast.makeText(context, "Rule removed", Toast.LENGTH_SHORT).show()
                            },
                            onPauseRule = { rule, duration ->
                                FamilySyncManager.pauseRuleOnChild(device.deviceId, rule, duration)
                            }
                        )
                        1 -> AnalysisTabContent(
                            device = device,
                            appsUsage = childAppsUsage
                        )
                        2 -> ControlsTabContent(
                            device = device,
                            onLockDevice = {
                                FamilySyncManager.lockChildDevice(device.deviceId) { success ->
                                    Toast.makeText(
                                        context,
                                        if (success) "Lock command sent to ${device.displayName}" else "Failed to send lock command",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onOpenRemoveDialog = { showRemoveDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newNameInput by remember { mutableStateOf(device.displayName) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Child Device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Give this device a custom nickname visible on your parent dashboard.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Device Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.renameChildDevice(device.deviceId, newNameInput) {
                            showRenameDialog = false
                            Toast.makeText(context, "Device renamed!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Confirmation Dialog
    if (showRemoveDialog) {
        var confirmText by remember { mutableStateOf("") }
        val isConfirmed = confirmText.trim().equals("Remove", ignoreCase = true)

        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Device Connection?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will completely unlink ${device.displayName} and clear all enforced rules. The child device will return to standalone mode.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Type \"Remove\" below to confirm:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("Remove") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.removeAndUnlinkChildDevice(device.deviceId) {
                            showRemoveDialog = false
                            onDeviceRemoved()
                        }
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// TAB 1: BLOCK RULES TAB (EXACT SAME MATURE BLOCKS SYSTEM)
// =============================================================================

@Composable
private fun ChildBlocksTabContent(
    device: RemoteChildDevice,
    rules: List<BlockRule>,
    onToggleRule: (BlockRule, Boolean) -> Unit,
    onEditRule: (BlockRule) -> Unit,
    onDeleteRule: (BlockRule) -> Unit,
    onPauseRule: (BlockRule, Int) -> Unit
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
                    text = "Tap '+ Create a Block' below to set up apps, websites, or schedules for ${device.displayName}",
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
                        onPause = { duration -> onPauseRule(rule, duration) },
                        isParentSide = true
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
                        onPause = { duration -> onPauseRule(rule, duration) },
                        isParentSide = true
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
                        onPause = { duration -> onPauseRule(rule, duration) },
                        isParentSide = true
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
                        onPause = { duration -> onPauseRule(rule, duration) },
                        isParentSide = true
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

// =============================================================================
// TAB 2: ANALYSIS & SCREEN TIME TAB
// =============================================================================

@Composable
private fun AnalysisTabContent(
    device: RemoteChildDevice,
    appsUsage: List<ChildAppUsage>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Time Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Today's Screen Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${device.screenTimeMinutes / 60}h ${device.screenTimeMinutes % 60}m",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (device.isOnline) "Active today • Live sync active" else "Device offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Child's Most Used Apps Today
        item {
            Text(
                "Most Used Apps Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        if (appsUsage.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Text("No App Usage Recorded Yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Usage statistics will sync here as the child uses apps on their device.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(appsUsage, key = { it.packageName }) { app ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            com.prism.screenharmony.flex.ui.components.RemoteAppIcon(
                                packageName = app.packageName,
                                appName = app.appName,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${app.durationMinutes / 60}h ${app.durationMinutes % 60}m",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// =============================================================================
// TAB 3: CONTROLS TAB (STATUS, SPECS & REMOTE ACTIONS)
// =============================================================================

@Composable
private fun ControlsTabContent(
    device: RemoteChildDevice,
    onLockDevice: () -> Unit,
    onOpenRemoveDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Device Status Card (Top of Controls)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Live Device Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Battery Level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${device.batteryLevel}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            if (device.isCharging) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⚡", fontSize = 12.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Screen State", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (device.isScreenOn) "Screen On" else "Screen Off", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active App", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.currentApp ?: "None / Home", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Device Specifications Card (Top of Controls)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Device Specifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Model", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.model.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("OS Version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.androidVersion.ifBlank { "Android" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Device ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(device.deviceId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Permission Health Status Card
        item {
            var isExpanded by remember { mutableStateOf(false) }
            val perms = device.permissions

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permission Health", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${perms.grantedCount}/${perms.totalCount} Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (perms.areAllGranted) Color(0xFF2E7D32) else if (perms.hasCrucialGranted) Color(0xFFE65100) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (perms.areAllGranted) Color(0xFF1B5E20).copy(alpha = 0.15f) else if (perms.hasCrucialGranted) Color(0xFFF57F17).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (perms.areAllGranted) "All Active" else "${perms.totalCount - perms.grantedCount} Missing",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (perms.areAllGranted) Color(0xFF2E7D32) else if (perms.hasCrucialGranted) Color(0xFFE65100) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            PermissionDetailRow(title = "Usage Access (Apps)", isGranted = perms.isUsageGranted)
                            PermissionDetailRow(title = "Display Over Apps", isGranted = perms.isOverlayGranted)
                            PermissionDetailRow(title = "Battery Optimization Disabled", isGranted = perms.isBatteryIgnored)
                            PermissionDetailRow(title = "Exact Alarms", isGranted = perms.isExactAlarmGranted)
                            PermissionDetailRow(title = "Accessibility Service", isGranted = perms.isAccessibilityGranted)
                            PermissionDetailRow(title = "Notifications", isGranted = perms.isNotificationGranted)
                        }
                    }
                }
            }
        }

        // 4. Remote Instant Lock Button Card with Accessibility Health Warning
        item {
            val isAccessibilityMissing = !device.permissions.isAccessibilityGranted
            var isLockingInProgress by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Lock Child Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Instantly turns off and locks child phone screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isLockingInProgress = true
                                onLockDevice()
                                coroutineScope.launch {
                                    delay(1200L)
                                    isLockingInProgress = false
                                }
                            },
                            enabled = !isLockingInProgress,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isLockingInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text("Locking...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Lock Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Yellow Warning if Accessibility Permission is Missing on Child Device
                    if (isAccessibilityMissing) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accessibility Permission Missing",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "Child device needs Accessibility Service enabled to execute the remote lock screen action.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF5D4037)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Danger Zone: Unlink Device
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unlink Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Disconnect child device and release parental protection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        onClick = onOpenRemoveDialog,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun PermissionDetailRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isGranted) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isGranted) "Active" else "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
