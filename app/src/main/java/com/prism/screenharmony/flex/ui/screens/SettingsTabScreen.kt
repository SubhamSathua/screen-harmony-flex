package com.prism.screenharmony.flex.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.prism.screenharmony.flex.data.AppLockManager
import com.prism.screenharmony.flex.data.LockTimeout
import com.prism.screenharmony.flex.family.*
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
    highlightPermissions: Boolean = false,
    onHighlightFinished: () -> Unit = {},
    onOpenAppLockSetup: () -> Unit = {},
    onOpenRecoverySettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeState = LocalThemeState.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val familyProfile by FamilySyncManager.familyProfile.collectAsState()

    // Pulse animation for permission highlighting
    val pulseBorderWidth = remember { Animatable(0f) }
    val pulseAlpha = remember { Animatable(0f) }

    val scrollState = rememberScrollState()

    LaunchedEffect(highlightPermissions) {
        if (highlightPermissions) {
            scrollState.animateScrollTo(scrollState.maxValue)
            repeat(3) {
                pulseBorderWidth.animateTo(3f, tween(300, easing = FastOutSlowInEasing))
                pulseAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                pulseBorderWidth.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                pulseAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
            onHighlightFinished()
        }
    }

    // Precomputed values lifted outside scroll loop for 60/120 FPS smoothness
    val isBioAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.1.0"
        } catch (e: Exception) {
            "0.1.0"
        }
    }

    var isColorPaletteExpanded by remember { mutableStateOf(false) }

    // App Lock Live State
    var isAppLockEnabled by remember { mutableStateOf(AppLockManager.isAppLockEnabled) }
    var isBiometricsEnabled by remember { mutableStateOf(AppLockManager.isBiometricsEnabled) }
    var currentTimeout by remember { mutableStateOf(AppLockManager.lockTimeout) }

    var showVerifyOffDialog by remember { mutableStateOf(false) }
    var showVerifyRecoveryDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    // Parental Controls Live State
    var selectedUnlinkMode by remember { mutableStateOf(ParentalAuthManager.getSelectedAuthMode(context)) }
    var isUnlinkAuthExpanded by remember { mutableStateOf(false) }
    var showFamilyNameDialog by remember { mutableStateOf(false) }
    var showCustomPinDialog by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================
            // 1. SECURITY & APP LOCK (4 OPTION CARDS)
            // =========================================================
            SectionHeader(title = "Security & App Lock")

            GroupedContainer {
                // Card 1: App Lock Toggle
                GroupedItemRow(
                    icon = Icons.Rounded.Lock,
                    title = "Lock ScreenHarmony",
                    subtitle = if (isAppLockEnabled) "App PIN lock is active" else "Protect app with secure PIN lock"
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

                // Card 2: Lock Timeout Selector
                GroupedItemRow(
                    icon = Icons.Rounded.Timer,
                    title = "Lock Timeout",
                    subtitle = if (isAppLockEnabled) currentTimeout.label else "Disabled (App Lock is Off)",
                    enabled = isAppLockEnabled,
                    onClick = if (isAppLockEnabled) { { showTimeoutDialog = true } } else null
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAppLockEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = currentTimeout.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAppLockEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                ItemDivider()

                // Card 3: Biometric / Fingerprint Unlock
                GroupedItemRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometrics / Fingerprint",
                    subtitle = if (!isBioAvailable) "Not supported or no enrolled biometrics" else if (isAppLockEnabled) "Unlock with fingerprint or face" else "Disabled (App Lock is Off)",
                    enabled = isAppLockEnabled && isBioAvailable
                ) {
                    Switch(
                        checked = isBiometricsEnabled,
                        enabled = isAppLockEnabled && isBioAvailable,
                        onCheckedChange = { targetState ->
                            if (activity != null) {
                                BiometricHelper.showDeviceCredentialPrompt(
                                    activity = activity,
                                    title = "Confirm Biometrics",
                                    subtitle = if (targetState) "Verify to enable biometric unlock" else "Verify to disable biometric unlock",
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

                ItemDivider()

                // Card 4: Forgot Password Recovery Methods
                GroupedItemRow(
                    icon = Icons.Rounded.KeyOff,
                    title = "Forgot Password Methods",
                    subtitle = if (isAppLockEnabled) "Manage Seed Phrase, biometrics & security questions" else "Disabled (App Lock is Off)",
                    enabled = isAppLockEnabled,
                    onClick = if (isAppLockEnabled) { { showVerifyRecoveryDialog = true } } else null
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = if (isAppLockEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }

            // =========================================================
            // 2. PARENTAL CONTROLS SECTION
            // =========================================================
            SectionHeader(title = "Parental Controls")

            GroupedContainer {
                // Parent / Family Display Name Customization
                GroupedItemRow(
                    icon = Icons.Rounded.FamilyRestroom,
                    title = "Parent / Family Name",
                    subtitle = familyProfile.familyName.ifBlank { "ScreenHarmony Family" },
                    onClick = { showFamilyNameDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Name",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                ItemDivider()

                // Unlink Device Password (Expandable with 4 radio options)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isUnlinkAuthExpanded = !isUnlinkAuthExpanded }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlink Device Password", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(selectedUnlinkMode.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (isUnlinkAuthExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = isUnlinkAuthExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isAppPinCreated = isAppLockEnabled

                            // 1. Same as app PIN (Show if app PIN is created)
                            if (isAppPinCreated) {
                                UnlinkAuthOptionRow(
                                    mode = UnlinkAuthMode.APP_PIN,
                                    isSelected = selectedUnlinkMode == UnlinkAuthMode.APP_PIN,
                                    onClick = {
                                        selectedUnlinkMode = UnlinkAuthMode.APP_PIN
                                        ParentalAuthManager.setAuthMode(context, UnlinkAuthMode.APP_PIN)
                                    }
                                )
                            }

                            // 2. Device pass/biometrics
                            UnlinkAuthOptionRow(
                                mode = UnlinkAuthMode.DEVICE_BIOMETRIC,
                                isSelected = selectedUnlinkMode == UnlinkAuthMode.DEVICE_BIOMETRIC,
                                onClick = {
                                    selectedUnlinkMode = UnlinkAuthMode.DEVICE_BIOMETRIC
                                    ParentalAuthManager.setAuthMode(context, UnlinkAuthMode.DEVICE_BIOMETRIC)
                                }
                            )

                            // 3. Custom (Opens pop up with 2 boxes: pin + verify)
                            UnlinkAuthOptionRow(
                                mode = UnlinkAuthMode.CUSTOM_PIN,
                                isSelected = selectedUnlinkMode == UnlinkAuthMode.CUSTOM_PIN,
                                onClick = {
                                    showCustomPinDialog = true
                                }
                            )

                            // 4. None
                            UnlinkAuthOptionRow(
                                mode = UnlinkAuthMode.NONE,
                                isSelected = selectedUnlinkMode == UnlinkAuthMode.NONE,
                                onClick = {
                                    selectedUnlinkMode = UnlinkAuthMode.NONE
                                    ParentalAuthManager.setAuthMode(context, UnlinkAuthMode.NONE)
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================
            // 3. APPEARANCE
            // =========================================================
            SectionHeader(title = "Appearance")

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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isColorPaletteExpanded = !isColorPaletteExpanded }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { themeState.palette = palette }
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

            // =========================================================
            // 4. PERMISSIONS & BACKGROUND ENFORCEMENT
            // =========================================================
            SectionHeader(title = "Permissions & Background Enforcement")

            GroupedContainer(
                modifier = if (pulseAlpha.value > 0.01f) {
                    Modifier.border(
                        width = pulseBorderWidth.value.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha.value),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else Modifier
            ) {
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

                // 4. Exact Alarms & Watchdog
                GroupedItemRow(
                    icon = Icons.Rounded.Alarm,
                    title = "Alarms & Reminders",
                    subtitle = if (permissionState.isExactAlarmGranted) "Active • Wakes up blocker reliably" else "Crucial • Resumes blocking after device kill"
                ) {
                    if (permissionState.isExactAlarmGranted) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Active", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    } else {
                        Button(
                            onClick = { PermissionHelper.openExactAlarmSettings(context) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ItemDivider()

                // 5. Accessibility
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

            // =========================================================
            // 5. ABOUT & SYNC
            // =========================================================
            SectionHeader(title = "About & Sync")

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

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Custom PIN Popup Dialog (2 Boxes: PIN + Verify)
    if (showCustomPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomPinDialog = false },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Set Custom Unlink PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This PIN will be required whenever a device is being unlinked from Family Control.", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("Enter 4-6 digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                confirmPinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("Verify PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    pinError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (isBioAvailable && activity != null) {
                        TextButton(
                            onClick = {
                                BiometricHelper.showDeviceCredentialPrompt(
                                    activity = activity,
                                    title = "Reset Unlink PIN",
                                    subtitle = "Authenticate to reset your unlink PIN",
                                    onSuccess = {
                                        showCustomPinDialog = false
                                        selectedUnlinkMode = UnlinkAuthMode.DEVICE_BIOMETRIC
                                        ParentalAuthManager.setAuthMode(context, UnlinkAuthMode.DEVICE_BIOMETRIC)
                                        Toast.makeText(context, "Switched to Device Lock / Biometrics", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { /* Error */ }
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Forgot PIN? Reset with Biometrics")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                        } else if (pinInput != confirmPinInput) {
                            pinError = "PINs do not match"
                        } else {
                            ParentalAuthManager.setCustomPin(context, pinInput)
                            selectedUnlinkMode = UnlinkAuthMode.CUSTOM_PIN
                            showCustomPinDialog = false
                            Toast.makeText(context, "Custom Unlink PIN saved!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Family Name Customization Dialog
    if (showFamilyNameDialog) {
        var familyNameInput by remember { mutableStateOf(familyProfile.familyName) }

        AlertDialog(
            onDismissRequest = { showFamilyNameDialog = false },
            icon = { Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Customise Family Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a display name for your family group (e.g. \"The Smiths\" or \"Alex's Family\").", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = familyNameInput,
                        onValueChange = { familyNameInput = it },
                        singleLine = true,
                        placeholder = { Text("Family Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.updateFamilyName(context, familyNameInput.trim().ifBlank { "ScreenHarmony Family" }) {
                            showFamilyNameDialog = false
                            Toast.makeText(context, "Family name updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFamilyNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Verify PIN to Turn Off App Lock
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

    // Verify PIN to Open Forgot Recovery Settings
    if (showVerifyRecoveryDialog) {
        AppLockVerifyDialog(
            title = "Recovery Settings",
            subtitle = "Enter your PIN to manage recovery methods",
            onVerified = {
                showVerifyRecoveryDialog = false
                onOpenRecoverySettings()
            },
            onDismiss = { showVerifyRecoveryDialog = false }
        )
    }

    // Lock Timeout Picker Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Select Lock Timeout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockTimeout.entries.forEach { timeout ->
                        val isSelected = currentTimeout == timeout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    AppLockManager.lockTimeout = timeout
                                    currentTimeout = timeout
                                    showTimeoutDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = timeout.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// UNLINK AUTH OPTION ROW COMPOSABLE
// =============================================================================

@Composable
private fun UnlinkAuthOptionRow(
    mode: UnlinkAuthMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// COMMON SETTINGS COMPONENTS
// =============================================================================

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
fun GroupedContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
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
    trailing: @Composable () -> Unit
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
            color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f)
            )
        }

        trailing()
    }
}

@Composable
fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppThemeMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(mode) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                AppThemeMode.SYSTEM -> "Auto"
                                AppThemeMode.LIGHT -> "Light"
                                AppThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
