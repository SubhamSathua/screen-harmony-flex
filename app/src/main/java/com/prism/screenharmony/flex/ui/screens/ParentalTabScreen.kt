package com.prism.screenharmony.flex.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.core.content.ContextCompat
import com.prism.screenharmony.flex.data.BlockRule
import com.prism.screenharmony.flex.family.*
import com.prism.screenharmony.flex.ui.viewmodels.PermissionState
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTabScreen(
    permissionState: PermissionState = PermissionState(),
    onNavigateToPermissions: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FamilySyncManager.initialize(context)
        FamilyNotificationHelper.createNotificationChannel(context)
    }

    // Request Notification Permission for API 33+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val familyProfile by FamilySyncManager.familyProfile.collectAsState()
    val connectedDevices by FamilySyncManager.connectedDevices.collectAsState()
    val childRules by FamilySyncManager.childPushedRules.collectAsState()
    val oneTimeDenialAlert by FamilySyncManager.oneTimeDenialAlert.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }
    var showScannerView by remember { mutableStateOf(false) }
    var showManualCodeDialog by remember { mutableStateOf(false) }
    var selectedDeviceForConfigure by remember { mutableStateOf<RemoteChildDevice?>(null) }
    var deviceForUnlinkReview by remember { mutableStateOf<RemoteChildDevice?>(null) }
    var deviceForRemoveConfirm by remember { mutableStateOf<RemoteChildDevice?>(null) }
    var showChildRequestUnlinkDialog by remember { mutableStateOf(false) }
    var showParentLeaveDialog by remember { mutableStateOf(false) }

    // If a device is opened for configuration, render the 3-tab detail screen
    selectedDeviceForConfigure?.let { selected ->
        val liveDevice = connectedDevices.find { it.deviceId == selected.deviceId } ?: selected
        ChildDeviceDetailScreen(
            device = liveDevice,
            onBack = { selectedDeviceForConfigure = null },
            onDeviceRemoved = { selectedDeviceForConfigure = null }
        )
        return
    }

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
                    containerColor = if (familyProfile.role == FamilyRole.CHILD) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
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
                    // Permission Health Icon (Red if <4 baseline, Dark-Yellow/Amber if missing accessibility)
                    if (!permissionState.areBase4PermissionsGranted) {
                        IconButton(onClick = onNavigateToPermissions) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = "Missing Crucial Permissions",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (!permissionState.isAccessibilityGranted) {
                        IconButton(onClick = onNavigateToPermissions) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Accessibility Service Inactive",
                                tint = Color(0xFFF57F17) // Dark Yellow / Amber
                            )
                        }
                    }

                    // Header Refresh Button
                    IconButton(
                        onClick = {
                            FamilySyncManager.forceRefresh(context) { success, msg ->
                                Toast.makeText(context, if (success) "Data refreshed successfully" else "Refresh failed: $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }

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
                        .padding(innerPadding),
                    familyProfile = familyProfile,
                    devices = connectedDevices,
                    onShowQr = { showQrDialog = true },
                    onConfigureDevice = { device ->
                        selectedDeviceForConfigure = device
                    },
                    onOpenUnlinkReview = { device ->
                        deviceForUnlinkReview = device
                    },
                    onOpenRemoveDialog = { device ->
                        deviceForRemoveConfirm = device
                    }
                )
            }

            FamilyRole.CHILD -> {
                ChildProtectedView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    familyProfile = familyProfile,
                    pushedRules = childRules,
                    showDenialAlert = oneTimeDenialAlert,
                    onDismissDenialAlert = { FamilySyncManager.dismissDenialAlert() },
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

    // Parent Unlink Review Popup Dialog
    deviceForUnlinkReview?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceForUnlinkReview = null },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Unlink Request From Child") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${device.model} • ${device.androidVersion} • Battery ${device.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Text(
                        text = "Reason:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (device.unlinkReason.isNotBlank()) "\"${device.unlinkReason}\"" else "No specific reason provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.ignoreUnlinkRequest(device.deviceId) {
                            deviceForUnlinkReview = null
                            Toast.makeText(context, "Request ignored & denied for child", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ignore Request")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val target = device
                        deviceForUnlinkReview = null
                        deviceForRemoveConfirm = target
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Device")
                }
            }
        )
    }

    // Remove Confirmation Dialog (Type "Remove")
    deviceForRemoveConfirm?.let { device ->
        var removeInput by remember { mutableStateOf("") }
        val isConfirmed = removeInput.trim().equals("Remove", ignoreCase = false)

        AlertDialog(
            onDismissRequest = { deviceForRemoveConfirm = null },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Device from Family?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This will permanently disconnect ${device.displayName} and stop all remote controls.", style = MaterialTheme.typography.bodyMedium)
                    Text("To confirm, please type \"Remove\" below:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = removeInput,
                        onValueChange = { removeInput = it },
                        placeholder = { Text("Type \"Remove\"") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.removeAndUnlinkChildDevice(device.deviceId) {
                            deviceForRemoveConfirm = null
                            Toast.makeText(context, "Device unlinked and removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unlink & Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceForRemoveConfirm = null }) {
                    Text("Cancel")
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
                    Text("This sends an unlink request to your parent's phone. They can review device stats, approve, or deny.", style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentDashboardView(
    modifier: Modifier = Modifier,
    familyProfile: FamilyProfile,
    devices: List<RemoteChildDevice>,
    onShowQr: () -> Unit,
    onConfigureDevice: (RemoteChildDevice) -> Unit,
    onOpenUnlinkReview: (RemoteChildDevice) -> Unit,
    onOpenRemoveDialog: (RemoteChildDevice) -> Unit
) {
    val context = LocalContext.current
    var isPullRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            isPullRefreshing = true
            FamilySyncManager.forceRefresh(context) { success, msg ->
                isPullRefreshing = false
                Toast.makeText(context, if (success) "Data refreshed successfully" else "Refresh failed: $msg", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
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
                        onConfigure = { onConfigureDevice(device) },
                        onOpenUnlinkReview = { onOpenUnlinkReview(device) },
                        onOpenRemoveDialog = { onOpenRemoveDialog(device) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildDeviceCard(
    device: RemoteChildDevice,
    onConfigure: () -> Unit,
    onOpenUnlinkReview: () -> Unit,
    onOpenRemoveDialog: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

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
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                    // Rounded M3 Expressive MoreVert Container
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Device Options", modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Device") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Remove Device", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onOpenRemoveDialog()
                                }
                            )
                        }
                    }
                }
            }

            // Unlink Requested Attention Banner
            if (device.unlinkRequested) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenUnlinkReview() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlink Requested by Child", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Tap to review reason & approve/ignore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
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

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(device.displayName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Child Device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a custom name for this device (e.g. Alex's Phone).", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FamilySyncManager.renameChildDevice(device.deviceId, newName)
                        showRenameDialog = false
                        Toast.makeText(context, "Device renamed!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = newName.isNotBlank()
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
}

// =============================================================================
// CHILD PROTECTED VIEW (SEAMLESS HEADER + 3 TABS: BLOCKS, ANALYSIS, CONTROLS)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildProtectedView(
    modifier: Modifier = Modifier,
    familyProfile: FamilyProfile,
    pushedRules: List<BlockRule>,
    showDenialAlert: Boolean,
    onDismissDenialAlert: () -> Unit,
    onRequestUnlink: () -> Unit,
    onCancelUnlink: () -> Unit
) {
    val context = LocalContext.current
    var selectedChildTab by remember { mutableIntStateOf(0) }
    val childTabs = listOf("Blocks", "Analysis", "Controls")
    var isUnlinkRequested by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // One-time Denial Alert Banner
        AnimatedVisibility(
            visible = showDenialAlert,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unlink Request Denied", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Your parent reviewed and denied your unlink request.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = onDismissDenialAlert) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Child Seamless Navigation Tabs (Combined with TopAppBar style)
        PrimaryTabRow(
            selectedTabIndex = selectedChildTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            childTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedChildTab == index,
                    onClick = { selectedChildTab = index },
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
                            Text(
                                text = title,
                                fontWeight = if (selectedChildTab == index) FontWeight.Bold else FontWeight.Normal
                            )
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
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = selectedChildTab,
                label = "ChildTabAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ChildBlocksTabContent(
                        rules = pushedRules
                    )
                    1 -> ChildAnalysisTabContent(
                        context = context,
                        familyProfile = familyProfile
                    )
                    2 -> ChildControlsTabContent(
                        familyProfile = familyProfile,
                        isUnlinkRequested = isUnlinkRequested,
                        onRequestUnlink = {
                            onRequestUnlink()
                            isUnlinkRequested = true
                        },
                        onCancelUnlink = {
                            onCancelUnlink()
                            isUnlinkRequested = false
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// CHILD READ-ONLY BLOCKS TAB (WITHOUT "DEVICE PROTECTED" CARD)
// =============================================================================

@Composable
private fun ChildBlocksTabContent(
    rules: List<BlockRule>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Active Rules Enforced by Parent (${rules.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (rules.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("No Active Restrictions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your parent hasn't pushed any blocking rules to this phone yet.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (rule.selectedApps.isNotEmpty()) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text("${rule.selectedApps.size} Apps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                if (rule.selectedWebsites.isNotEmpty()) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                        Text("${rule.selectedWebsites.size} Websites", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (rule.isEnabled) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = if (rule.isEnabled) "Enforced" else "Paused",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (rule.isEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// CHILD ANALYSIS TAB
// =============================================================================

@Composable
private fun ChildAnalysisTabContent(
    context: Context,
    familyProfile: FamilyProfile
) {
    val todayMinutes = remember { FamilyUsageHelper.getTodayUsageMinutes(context) }
    val topApps = remember { FamilyUsageHelper.getTodayTopApps(context, limit = 6) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today Screen Time Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Today's Screen Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${todayMinutes / 60}h ${todayMinutes % 60}m",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Text(
                text = "Most Used Apps Today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (topApps.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No app usage recorded yet today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(topApps, key = { it.packageName }) { app ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${app.durationMinutes}m",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// CHILD CONTROLS TAB
// =============================================================================

@Composable
private fun ChildControlsTabContent(
    familyProfile: FamilyProfile,
    isUnlinkRequested: Boolean,
    onRequestUnlink: () -> Unit,
    onCancelUnlink: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Protected Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Device Protected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Linked to: ${familyProfile.familyName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = "All focus sessions, app blocks, and website rules are synchronized in real-time with the parent device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Unlink Request Section Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Family Pairing Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "If you need to disconnect this device from family management, you can send an unlink request to your parent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isUnlinkRequested) {
                        OutlinedButton(
                            onClick = onRequestUnlink,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Unlink from Parent")
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("⏳ Unlink Request Sent", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text("Waiting for parent review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = onCancelUnlink) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
