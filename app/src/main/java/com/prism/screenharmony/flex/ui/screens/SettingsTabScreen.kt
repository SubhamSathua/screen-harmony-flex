package com.prism.screenharmony.flex.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.data.LockTimeout
import com.prism.screenharmony.flex.ui.screens.lock.AppLockVerifyDialog
import com.prism.screenharmony.flex.ui.theme.AppColorPalette
import com.prism.screenharmony.flex.ui.theme.AppThemeMode
import com.prism.screenharmony.flex.ui.theme.LocalThemeState
import com.prism.screenharmony.flex.ui.viewmodels.PermissionState
import com.prism.screenharmony.flex.utils.BiometricHelper
import com.prism.screenharmony.flex.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    permissionState: PermissionState,
    onOpenAppLockSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeState = LocalThemeState.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var isColorPaletteExpanded by remember { mutableStateOf(false) }

    // App Lock Live State
    var isAppLockEnabled by remember { mutableStateOf(AppLockManager.isAppLockEnabled) }
    var isBiometricsEnabled by remember { mutableStateOf(AppLockManager.isBiometricsEnabled) }
    var currentTimeout by remember { mutableStateOf(AppLockManager.lockTimeout) }

    var showVerifyOffDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================
            // 1. SECURITY & APP LOCK (3 OPTION CARDS)
            // =========================================================
            item { SectionHeader(title = "Security & App Lock") }

            item {
                GroupedContainer {
                    // Card 1: Enable App Lock Toggle
                    GroupedItemRow(
                        icon = if (isAppLockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        title = "Enable App Lock",
                        subtitle = if (isAppLockEnabled) "Active • PIN required on launch" else "Off • Open app without PIN"
                    ) {
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { targetState ->
                                if (targetState) {
                                    onOpenAppLockSetup()
                                } else {
                                    showVerifyOffDialog = true
                                }
                            }
                        )
                    }

                    ItemDivider()

                    // Card 2: Timeout
                    GroupedItemRow(
                        icon = Icons.Rounded.Timer,
                        title = "Lock Timeout",
                        subtitle = if (isAppLockEnabled) "Locks after ${currentTimeout.label} in background" else "Disabled (App Lock is Off)",
                        enabled = isAppLockEnabled,
                        onClick = if (isAppLockEnabled) { { showTimeoutDialog = true } } else null
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAppLockEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = currentTimeout.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAppLockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    ItemDivider()

                    // Card 3: Biometrics Toggle
                    val isBioAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
                    GroupedItemRow(
                        icon = Icons.Rounded.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = when {
                            !isAppLockEnabled -> "Disabled (App Lock is Off)"
                            !isBioAvailable -> "Unavailable on this device"
                            isBiometricsEnabled -> "Active • Unlock with Fingerprint or Face"
                            else -> "Off • Tap to enable biometric unlock"
                        },
                        enabled = isAppLockEnabled && isBioAvailable
                    ) {
                        Switch(
                            checked = isBiometricsEnabled && isAppLockEnabled,
                            enabled = isAppLockEnabled && isBioAvailable,
                            onCheckedChange = { targetState ->
                                if (activity != null) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = if (targetState) "Enable Biometrics" else "Disable Biometrics",
                                        subtitle = "Verify your fingerprint or face",
                                        onSuccess = {
                                            AppLockManager.isBiometricsEnabled = targetState
                                            isBiometricsEnabled = targetState
                                        },
                                        onError = { /* Keep state */ }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // =========================================================
            // 2. APPEARANCE
            // =========================================================
            item { SectionHeader(title = "Appearance") }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Theme Mode",
                        subtitle = "Select system, light, or dark visual style"
                    ) {
                        SingleChoiceSegmentedRow(
                            selected = themeState.themeMode,
                            onSelect = { themeState.themeMode = it }
                        )
                    }

                    ItemDivider()

                    GroupedItemRow(
                        icon = Icons.Rounded.Contrast,
                        title = "AMOLED Black",
                        subtitle = "Pure pitch black background for OLED screens"
                    ) {
                        Switch(
                            checked = themeState.isAmoled,
                            onCheckedChange = { themeState.isAmoled = it }
                        )
                    }

                    ItemDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isColorPaletteExpanded = !isColorPaletteExpanded }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = themeState.palette.primaryColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(7.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Color Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(themeState.palette.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = if (isColorPaletteExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isColorPaletteExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppColorPalette.entries.forEach { palette ->
                                    val isSelected = themeState.palette == palette
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth().clickable { themeState.palette = palette }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = palette.primaryColor,
                                                modifier = Modifier.size(24.dp).border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            ) {}
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = palette.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 3. PERMISSIONS & BACKGROUND ENFORCEMENT
            // =========================================================
            item { SectionHeader(title = "Permissions & Background Enforcement") }

            item {
                GroupedContainer {
                    // 1. Usage Access
                    GroupedItemRow(
                        icon = Icons.Rounded.QueryStats,
                        title = "Usage Access (Apps)",
                        subtitle = if (permissionState.isUsageGranted) "Active • Detects foreground apps" else "Required • Tap to grant permission"
                    ) {
                        if (permissionState.isUsageGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Button(
                                onClick = { PermissionHelper.openUsageAccessSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ItemDivider()

                    // 2. Display Over Other Apps (Overlay)
                    GroupedItemRow(
                        icon = Icons.Rounded.Layers,
                        title = "Display Over Other Apps",
                        subtitle = if (permissionState.isOverlayGranted) "Active • Pops up lock screen over apps" else "Crucial • Allows lock screen to open in background"
                    ) {
                        if (permissionState.isOverlayGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Button(
                                onClick = { PermissionHelper.openOverlaySettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ItemDivider()

                    // 3. Battery Optimization
                    GroupedItemRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Unrestricted Battery",
                        subtitle = if (permissionState.isBatteryIgnored) "Active • Never killed by OS" else "Recommended • Keeps background service alive"
                    ) {
                        if (permissionState.isBatteryIgnored) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    ItemDivider()

                    // 4. Accessibility
                    GroupedItemRow(
                        icon = Icons.Rounded.Language,
                        title = "Accessibility (Websites)",
                        subtitle = if (permissionState.isAccessibilityGranted) "Active • Inspecting browser URLs" else "Optional • Only needed for websites"
                    ) {
                        if (permissionState.isAccessibilityGranted) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { PermissionHelper.openAccessibilitySettings(context) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // =========================================================
            // 4. ABOUT & SYNC
            // =========================================================
            item { SectionHeader(title = "About & Sync") }

            item {
                GroupedContainer {
                    GroupedItemRow(
                        icon = Icons.Rounded.CloudQueue,
                        title = "Sync Protocol",
                        subtitle = "Firebase Spark (100% Free Cloud Tier)"
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(4.dp)) {
                            Text("FREE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    ItemDivider()

                    val versionName = remember {
                        try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            pInfo.versionName ?: "0.1.0"
                        } catch (e: Exception) {
                            "0.1.0"
                        }
                    }

                    GroupedItemRow(
                        icon = Icons.Rounded.Info,
                        title = "App Version",
                        subtitle = "ScreenHarmony Flex v$versionName"
                    ) {
                        Text(
                            text = "v$versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Verify PIN to Turn OFF App Lock Dialog
    if (showVerifyOffDialog) {
        AppLockVerifyDialog(
            title = "Disable App Lock",
            subtitle = "Enter your current PIN to turn off App Lock",
            onVerified = {
                AppLockManager.disableAppLock()
                isAppLockEnabled = false
                isBiometricsEnabled = false
                showVerifyOffDialog = false
            },
            onDismiss = { showVerifyOffDialog = false }
        )
    }

    // Select Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            icon = { Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Select Lock Timeout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LockTimeout.entries.forEach { timeout ->
                        val isSelected = currentTimeout == timeout
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    AppLockManager.lockTimeout = timeout
                                    currentTimeout = timeout
                                    showTimeoutDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeout.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun GroupedContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun GroupedItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(8.dp).size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailingContent()
    }
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun SingleChoiceSegmentedRow(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = selected == mode
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { onSelect(mode) }
                ) {
                    Text(
                        text = when (mode) {
                            AppThemeMode.SYSTEM -> "Auto"
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.DARK -> "Dark"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
