package com.prism.screenharmony.flex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.family.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTabScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FamilySyncManager.initialize(context)
    }

    val familyProfile by FamilySyncManager.familyProfile.collectAsState()
    val connectedDevices by FamilySyncManager.connectedDevices.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showScannerView by remember { mutableStateOf(false) }
    var showManualCodeDialog by remember { mutableStateOf(false) }
    var showPushRuleDialogForDevice by remember { mutableStateOf<RemoteChildDevice?>(null) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }

    if (showScannerView) {
        QrScannerView(
            onQrCodeScanned = { payload ->
                showScannerView = false
                FamilySyncManager.joinFamilyViaQr(context, payload) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Successfully paired with Parent!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, error ?: "Failed to pair", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showScannerView = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Text(
                        text = "Family & Parental Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (familyProfile.role != FamilyRole.STANDALONE) {
                        IconButton(onClick = { showUnlinkConfirmDialog = true }) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = "Unlink Family", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (familyProfile.role) {
            FamilyRole.STANDALONE -> {
                UnpairedRoleSelectionView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    onSetupParent = {
                        FamilySyncManager.setupAsParent(context) { success ->
                            if (success) {
                                showQrDialog = true
                                Toast.makeText(context, "Parent mode ready! Scan QR code on child phone.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to create family", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onSetupChildQr = { showScannerView = true },
                    onSetupChildCode = { showManualCodeDialog = true }
                )
            }

            FamilyRole.PARENT -> {
                ParentDashboardView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                    familyProfile = familyProfile,
                    devices = connectedDevices,
                    onShowQr = { showQrDialog = true },
                    onToggleLock = { device, lock ->
                        FamilySyncManager.toggleRemoteLock(device.deviceId, lock)
                        Toast.makeText(context, if (lock) "Lock command sent" else "Unlock command sent", Toast.LENGTH_SHORT).show()
                    },
                    onAddRule = { device ->
                        showPushRuleDialogForDevice = device
                    }
                )
            }

            FamilyRole.CHILD -> {
                ChildProtectedView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    familyProfile = familyProfile,
                    onUnlink = { showUnlinkConfirmDialog = true }
                )
            }
        }
    }

    // QR Code Dialog
    if (showQrDialog) {
        val qrPayload = remember(familyProfile) {
            JSONObject().apply {
                put("familyId", familyProfile.familyId)
                put("code", familyProfile.pairingCode)
                put("secret", familyProfile.pairingSecret)
                put("name", familyProfile.familyName)
            }.toString()
        }

        val qrBitmap = remember(qrPayload) {
            QrCodeHelper.generateQrBitmap("SHPAIR:$qrPayload", size = 512)
        }

        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pair Child Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan this QR code using the ScreenHarmony app on your child's phone",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(4.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Pairing QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Or enter 6-digit Code:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = familyProfile.pairingCode,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 2.sp
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pairing Code", familyProfile.pairingCode))
                                    Toast.makeText(context, "Pairing code copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showQrDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // Manual Code Dialog
    if (showManualCodeDialog) {
        var enteredCode by remember { mutableStateOf("") }
        var isJoining by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualCodeDialog = false },
            title = { Text("Enter Parent Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-digit code shown on the Parent's phone (e.g. SH-4829).", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = enteredCode,
                        onValueChange = { enteredCode = it.uppercase() },
                        placeholder = { Text("SH-XXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isJoining = true
                        FamilySyncManager.joinFamilyViaCode(context, enteredCode) { success, error ->
                            isJoining = false
                            if (success) {
                                showManualCodeDialog = false
                                Toast.makeText(context, "Successfully paired with Parent!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, error ?: "Failed to pair", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = enteredCode.isNotBlank() && !isJoining
                ) {
                    Text(if (isJoining) "Connecting..." else "Pair Device")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Push Remote Rule Dialog
    showPushRuleDialogForDevice?.let { targetDevice ->
        var blockName by remember { mutableStateOf("") }
        var appsToBlock by remember { mutableStateOf("com.instagram.android, com.google.android.youtube") }

        AlertDialog(
            onDismissRequest = { showPushRuleDialogForDevice = null },
            title = { Text("Push Block Rule to ${targetDevice.deviceName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create a remote block rule that immediately enforces on this child phone.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = blockName,
                        onValueChange = { blockName = it },
                        label = { Text("Rule Name") },
                        placeholder = { Text("Focus / Homework Block") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appsToBlock,
                        onValueChange = { appsToBlock = it },
                        label = { Text("Package Names (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val packages = appsToBlock.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                        val newRule = BlockRule(
                            id = "remote_" + System.currentTimeMillis(),
                            name = if (blockName.isBlank()) "Parental Block" else blockName,
                            selectedApps = packages,
                            isEnabled = true
                        )
                        FamilySyncManager.pushRuleToChild(targetDevice.deviceId, newRule)
                        showPushRuleDialogForDevice = null
                        Toast.makeText(context, "Pushed block rule to ${targetDevice.deviceName}!", Toast.LENGTH_LONG).show()
                    },
                    enabled = blockName.isNotBlank() || appsToBlock.isNotBlank()
                ) {
                    Text("Push Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPushRuleDialogForDevice = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlink Confirmation Dialog
    if (showUnlinkConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Unlink Family?") },
            text = {
                Text("This will remove cloud sync and disconnect all paired devices.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.unlinkFamily(context)
                        showUnlinkConfirmDialog = false
                        Toast.makeText(context, "Unlinked successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// UNPAIRED SELECTION VIEW
// =============================================================================

@Composable
private fun UnpairedRoleSelectionView(
    modifier: Modifier = Modifier,
    onSetupParent: () -> Unit,
    onSetupChildQr: () -> Unit,
    onSetupChildCode: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FamilyRestroom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).fillMaxSize()
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Remote Family Protection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "100% Free Remote App & Web Blocking with Instant QR Pairing and Zero Server Fees.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Role 1: Parent Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = onSetupParent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.SupervisorAccount, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Set up as Parent Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Generate pairing QR code, monitor child devices, and push block rules remotely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Role 2: Child Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            onClick = onSetupChildQr,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.padding(12.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Link as Child Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Scan the QR code on the parent's phone to connect this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        TextButton(onClick = onSetupChildCode) {
            Text("Have a 6-digit pairing code instead? Enter code")
        }
    }
}

// =============================================================================
// PARENT DASHBOARD VIEW
// =============================================================================

@Composable
private fun ParentDashboardView(
    modifier: Modifier = Modifier,
    familyProfile: FamilyProfile,
    devices: List<RemoteChildDevice>,
    onShowQr: () -> Unit,
    onToggleLock: (RemoteChildDevice, Boolean) -> Unit,
    onAddRule: (RemoteChildDevice) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Pairing Info Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Family Pairing Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(familyProfile.pairingCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text("${devices.size} Child Device(s) Linked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    Button(
                        onClick = onShowQr,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show QR")
                    }
                }
            }
        }

        item {
            Text(
                text = "Connected Child Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (devices.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.DevicesOther, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("No Child Devices Connected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Tap 'Show QR' and scan it using the ScreenHarmony app on your child's phone.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(devices, key = { it.deviceId }) { device ->
                ChildDeviceCard(
                    device = device,
                    onToggleLock = { onToggleLock(device, it) },
                    onAddRule = { onAddRule(device) }
                )
            }
        }
    }
}

@Composable
private fun ChildDeviceCard(
    device: RemoteChildDevice,
    onToggleLock: (Boolean) -> Unit,
    onAddRule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(device.deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(device.model.ifBlank { "Android Device" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (device.isOnline) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (device.isOnline) Color(0xFF4CAF50) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (device.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (device.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Telemetry Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Battery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${device.batteryLevel}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text("Screen Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${device.screenTimeMinutes / 60}h ${device.screenTimeMinutes % 60}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Column {
                    Text("Rules Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${device.rulesCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleLock(!device.isLocked) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = if (device.isLocked) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(if (device.isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (device.isLocked) "Unlock" else "Lock Device", fontSize = 12.sp)
                }

                Button(
                    onClick = onAddRule,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AddModerator, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push Rule", fontSize = 12.sp)
                }
            }
        }
    }
}

// =============================================================================
// CHILD PROTECTED VIEW
// =============================================================================

@Composable
private fun ChildProtectedView(
    modifier: Modifier = Modifier,
    familyProfile: FamilyProfile,
    onUnlink: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(24.dp).fillMaxSize()
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Device Protected", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Linked to: ${familyProfile.familyName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("All focus sessions, app blocks, and website filters are synchronized in real-time with the parent device.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Cloud Sync Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("100% Free Firebase WebSocket connected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onUnlink,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.LinkOff, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlink from Family")
        }
    }
}
