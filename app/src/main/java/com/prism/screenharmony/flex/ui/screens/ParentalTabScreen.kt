package com.prism.screenharmony.flex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
    var selectedDeviceForConfigure by remember { mutableStateOf<RemoteChildDevice?>(null) }
    var showChildRequestUnlinkDialog by remember { mutableStateOf(false) }
    var showParentLeaveDialog by remember { mutableStateOf(false) }

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
                    if (familyProfile.role == FamilyRole.PARENT) {
                        IconButton(onClick = { showParentLeaveDialog = true }) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete Family", tint = MaterialTheme.colorScheme.error)
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
                    onConfigureDevice = { device ->
                        selectedDeviceForConfigure = device
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
                    onRequestUnlink = { showChildRequestUnlinkDialog = true },
                    onCancelUnlink = {
                        FamilySyncManager.cancelUnlinkRequestFromChild(context) { success ->
                            if (success) {
                                Toast.makeText(context, "Unlink request cancelled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Configure Child Device Dialog
    selectedDeviceForConfigure?.let { device ->
        // Keep active instance in sync with state flow
        val liveDevice = connectedDevices.find { it.deviceId == device.deviceId } ?: device

        DeviceConfigureDialog(
            device = liveDevice,
            onDismiss = { selectedDeviceForConfigure = null },
            onRename = { newName ->
                FamilySyncManager.renameChildDevice(device.deviceId, newName)
                Toast.makeText(context, "Device renamed!", Toast.LENGTH_SHORT).show()
            },
            onToggleLock = { lock ->
                FamilySyncManager.toggleRemoteLock(device.deviceId, lock)
                Toast.makeText(context, if (lock) "Lock command sent" else "Unlock command sent", Toast.LENGTH_SHORT).show()
            },
            onPushRule = { rule ->
                FamilySyncManager.pushRuleToChild(device.deviceId, rule)
                Toast.makeText(context, "Rule pushed to ${device.displayName}!", Toast.LENGTH_LONG).show()
            },
            onRemoveDevice = {
                FamilySyncManager.removeAndUnlinkChildDevice(device.deviceId) {
                    selectedDeviceForConfigure = null
                    Toast.makeText(context, "Device unlinked and removed", Toast.LENGTH_LONG).show()
                }
            }
        )
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

    // Child Request Unlink Dialog
    if (showChildRequestUnlinkDialog) {
        var reasonText by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChildRequestUnlinkDialog = false },
            title = { Text("Request Unlink from Parent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This sends an unlink request notification to your parent's phone. They must approve and remove the device.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason (Optional)") },
                        placeholder = { Text("e.g. Setting up my own work schedule") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        FamilySyncManager.requestUnlinkFromChild(context, reasonText) { success ->
                            isSubmitting = false
                            showChildRequestUnlinkDialog = false
                            if (success) {
                                Toast.makeText(context, "Unlink request sent to Parent", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "Sending..." else "Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChildRequestUnlinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Parent Leave Family Confirmation
    if (showParentLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showParentLeaveDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Family Control?") },
            text = {
                Text("This will disconnect all child devices and delete the family group.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.unlinkFamily(context)
                        showParentLeaveDialog = false
                        Toast.makeText(context, "Family reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParentLeaveDialog = false }) {
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
    onConfigureDevice: (RemoteChildDevice) -> Unit
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
                    onConfigure = { onConfigureDevice(device) }
                )
            }
        }
    }
}

@Composable
private fun ChildDeviceCard(
    device: RemoteChildDevice,
    onConfigure: () -> Unit
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
                        Text(device.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (device.customName.isNotBlank()) "${device.deviceName} • ${device.model}" else device.model.ifBlank { "Android Device" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            // Unlink Requested Attention Banner
            if (device.unlinkRequested) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth().clickable { onConfigure() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlink Requested by Child", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Tap Configure to review device details and unlink", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
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

            // Clean Single Action Button: Configuration
            Button(
                onClick = onConfigure,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure Device")
            }
        }
    }
}

// =============================================================================
// DEVICE CONFIGURE & INFO DIALOG
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceConfigureDialog(
    device: RemoteChildDevice,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onPushRule: (BlockRule) -> Unit,
    onRemoveDevice: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(device.displayName) }

    var showPushRuleSection by remember { mutableStateOf(false) }
    var ruleName by remember { mutableStateOf("") }
    var rulePackages by remember { mutableStateOf("") }

    var removeConfirmText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header & Rename
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                isEditingName = false
                                onRename(editedName)
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Original Name: ${device.deviceName} (${device.model})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Unlink Request Info Card (if requested)
                if (device.unlinkRequested) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlink Requested by Child", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                if (device.unlinkReason.isNotBlank()) {
                                    Text("Reason: \"${device.unlinkReason}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                Text("The child has requested to disconnect this phone from family controls. You can review device stats or remove it below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                // Device Specs & Telemetry Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Device Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OS Version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(device.androidVersion.ifBlank { "Android" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Battery Level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${device.batteryLevel}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Today's Screen Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${device.screenTimeMinutes / 60}h ${device.screenTimeMinutes % 60}m", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Active Block Rules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${device.rulesCount} Rules", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Remote Lock Toggle Section
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remote Device Lock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Instantly lock screen with pin / focus wall", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = device.isLocked,
                                onCheckedChange = { onToggleLock(it) }
                            )
                        }
                    }
                }

                // Push Rule Section
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Push Block Rule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showPushRuleSection = !showPushRuleSection }) {
                                    Icon(if (showPushRuleSection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                                }
                            }

                            if (showPushRuleSection) {
                                OutlinedTextField(
                                    value = ruleName,
                                    onValueChange = { ruleName = it },
                                    label = { Text("Rule Name") },
                                    placeholder = { Text("Study Time") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = rulePackages,
                                    onValueChange = { rulePackages = it },
                                    label = { Text("Blocked Packages (comma separated)") },
                                    placeholder = { Text("com.instagram.android, com.google.android.youtube") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val packages = rulePackages.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                                        val newRule = BlockRule(
                                            id = "remote_" + System.currentTimeMillis(),
                                            name = if (ruleName.isBlank()) "Parental Block" else ruleName,
                                            selectedApps = packages,
                                            isEnabled = true
                                        )
                                        onPushRule(newRule)
                                        ruleName = ""
                                        rulePackages = ""
                                        showPushRuleSection = false
                                    },
                                    enabled = rulePackages.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Push Rule to Device")
                                }
                            }
                        }
                    }
                }

                // Remove Device Section (Type "Remove" Confirmation)
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Remove & Unlink Device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("To disconnect and remove this device from family controls, type \"Remove\" below to confirm.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            OutlinedTextField(
                                value = removeConfirmText,
                                onValueChange = { removeConfirmText = it },
                                placeholder = { Text("Type \"Remove\" to confirm") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val isRemoveEnabled = removeConfirmText.trim().equals("Remove", ignoreCase = false)

                            Button(
                                onClick = onRemoveDevice,
                                enabled = isRemoveEnabled,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlink & Remove Device")
                            }
                        }
                    }
                }

                item {
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
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
    onRequestUnlink: () -> Unit,
    onCancelUnlink: () -> Unit
) {
    var isUnlinkRequested by remember { mutableStateOf(false) }

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

        // Child Unlink Request Actions
        if (!isUnlinkRequested) {
            OutlinedButton(
                onClick = {
                    onRequestUnlink()
                    isUnlinkRequested = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.LinkOff, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Unlink from Parent")
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏳ Unlink Request Sent to Parent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Waiting for parent approval and removal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        onCancelUnlink()
                        isUnlinkRequested = false
                    }) {
                        Text("Cancel Request")
                    }
                }
            }
        }
    }
}
