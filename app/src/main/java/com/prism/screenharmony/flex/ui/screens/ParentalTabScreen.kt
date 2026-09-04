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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.prism.screenharmony.flex.data.*
import com.prism.screenharmony.flex.family.*
import com.prism.screenharmony.flex.ui.components.RemoteAppIcon
import com.prism.screenharmony.flex.ui.components.ScheduleGraph
import com.prism.screenharmony.flex.ui.viewmodels.PermissionState
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalTabScreen(
    permissionState: PermissionState = PermissionState(),
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToParentalSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasPromptedNotificationInSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FamilySyncManager.initialize(context)
        FamilyNotificationHelper.createNotificationChannel(context)
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
    var showParentMenu by remember { mutableStateOf(false) }
    var deviceForPermissionsCard by remember { mutableStateOf<RemoteChildDevice?>(null) }

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
                        text = "Parental Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Warning signs at top: ONLY shown on Child device!
                    if (familyProfile.role == FamilyRole.CHILD) {
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

                    // Parent Top MoreVert Actions: Settings (with triple pulse) & Reset Parental controls
                    if (familyProfile.role == FamilyRole.PARENT) {
                        Box {
                            IconButton(onClick = { showParentMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Parental Options")
                            }
                            DropdownMenu(
                                expanded = showParentMenu,
                                onDismissRequest = { showParentMenu = false },
                                shape = RoundedCornerShape(20.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    onClick = {
                                        showParentMenu = false
                                        onNavigateToParentalSettings()
                                    },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset Parental controls", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showParentMenu = false
                                        showParentLeaveDialog = true
                                    },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                )
                            }
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
                    },
                    onOpenPermissionsCard = { device ->
                        deviceForPermissionsCard = device
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

    // Parent Leave / Reset Parental Controls Confirmation (2-Step Warning + Random Code Flow)
    if (showParentLeaveDialog) {
        ResetParentalControlsDialog(
            onDismiss = { showParentLeaveDialog = false },
            onConfirmed = {
                FamilySyncManager.unlinkFamily(context)
                showParentLeaveDialog = false
                Toast.makeText(context, "Parental controls reset successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Child Permissions Card Dialog
    deviceForPermissionsCard?.let { device ->
        ChildPermissionsCardDialog(
            device = device,
            onDismiss = { deviceForPermissionsCard = null }
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
    onOpenRemoveDialog: (RemoteChildDevice) -> Unit,
    onOpenPermissionsCard: (RemoteChildDevice) -> Unit
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
                        onOpenRemoveDialog = { onOpenRemoveDialog(device) },
                        onOpenPermissionsCard = { onOpenPermissionsCard(device) }
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
    onOpenRemoveDialog: () -> Unit,
    onOpenPermissionsCard: () -> Unit
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
                            onDismissRequest = { showMenu = false },
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
                                    showMenu = false
                                    showRenameDialog = true
                                },
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Device", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onOpenRemoveDialog()
                                },
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
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

            // Clickable Permission Health Status Row
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = when {
                    device.permissions.areAllGranted -> Color(0xFF1B5E20).copy(alpha = 0.12f)
                    device.permissions.hasCrucialGranted -> Color(0xFFF57F17).copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenPermissionsCard() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (device.permissions.areAllGranted) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                        contentDescription = null,
                        tint = when {
                            device.permissions.areAllGranted -> Color(0xFF2E7D32)
                            device.permissions.hasCrucialGranted -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permissions: ${device.permissions.grantedCount}/${device.permissions.totalCount} Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                device.permissions.areAllGranted -> Color(0xFF2E7D32)
                                device.permissions.hasCrucialGranted -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = if (device.permissions.areAllGranted) "All required permissions granted" else "${device.permissions.totalCount - device.permissions.grantedCount} permission(s) missing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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
            val localRules by com.prism.screenharmony.flex.data.BlockRepository.rules.collectAsState()
            val liveRules = remember(pushedRules, localRules) {
                pushedRules.map { pushed ->
                    localRules.find { it.id == pushed.id } ?: pushed
                }
            }

            AnimatedContent(
                targetState = selectedChildTab,
                label = "ChildTabAnimation"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ChildBlocksTabContent(
                        rules = liveRules
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
// CHILD READ-ONLY BLOCKS TAB (3 SECTIONS: ACTIVE, PAUSED, BLOCK INACTIVE)
// =============================================================================

@Composable
private fun ChildBlocksTabContent(
    rules: List<BlockRule>
) {
    var selectedSummaryRule by remember { mutableStateOf<BlockRule?>(null) }
    var showPauseDialogForRule by remember { mutableStateOf<BlockRule?>(null) }

    if (rules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    } else {
        var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                currentTimeMillis = System.currentTimeMillis()
                com.prism.screenharmony.flex.data.BlockRepository.cleanExpiredPauses()
            }
        }

        val activeRules = remember(rules, currentTimeMillis) {
            val currentLocalTime = LocalTime.now()
            val currentDay = DayOfWeek.from(java.time.LocalDate.now())
            rules.filter { it.isEnabled && !it.isPaused() && it.isCurrentlyBlocked(currentLocalTime, currentDay) }
        }
        val pausedRules = remember(rules, currentTimeMillis) {
            rules.filter { it.isEnabled && it.isPaused() }
        }
        val inactiveRules = remember(rules, currentTimeMillis) {
            val currentLocalTime = LocalTime.now()
            val currentDay = DayOfWeek.from(java.time.LocalDate.now())
            rules.filter { !it.isEnabled || (!it.isPaused() && !it.isCurrentlyBlocked(currentLocalTime, currentDay)) }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ACTIVE SECTION
            if (activeRules.isNotEmpty()) {
                item {
                    Text(
                        text = "Active (${activeRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                items(activeRules, key = { it.id }) { rule ->
                    val isStrict = rule.pauseConfig.type == com.prism.screenharmony.flex.data.PauseType.STRICT || rule.blockType == com.prism.screenharmony.flex.data.BlockType.STRICT
                    ChildRuleCard(
                        rule = rule,
                        statusText = "Active",
                        statusColor = MaterialTheme.colorScheme.primary,
                        onClick = { selectedSummaryRule = rule },
                        actionContent = {
                            if (isStrict) {
                                StrictBadge()
                            } else {
                                FilledTonalButton(
                                    onClick = { showPauseDialogForRule = rule },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            }

            // 2. PAUSED SECTION
            if (pausedRules.isNotEmpty()) {
                item {
                    Text(
                        text = "Paused (${pausedRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                    )
                }
                items(pausedRules, key = { it.id }) { rule ->
                    val remainingMillis = ((rule.lastPausedAt ?: 0) + (rule.pauseDurationMinutes ?: 0) * 60 * 1000L - currentTimeMillis).coerceAtLeast(0L)
                    val remainingMins = kotlin.math.ceil(remainingMillis / (60 * 1000f)).toInt().coerceAtLeast(1)
                    val statusText = "Paused (${remainingMins}m)"
                    ChildRuleCard(
                        rule = rule,
                        statusText = statusText,
                        statusColor = MaterialTheme.colorScheme.secondary,
                        onClick = { selectedSummaryRule = rule },
                        actionContent = {
                            FilledTonalButton(
                                onClick = { com.prism.screenharmony.flex.data.BlockRepository.unpauseRule(rule.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            // 3. BLOCK INACTIVE SECTION (NO PAUSE BUTTON!)
            if (inactiveRules.isNotEmpty()) {
                item {
                    Text(
                        text = "Block Inactive (${inactiveRules.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                    )
                }
                items(inactiveRules, key = { it.id }) { rule ->
                    val isStrict = rule.pauseConfig.type == com.prism.screenharmony.flex.data.PauseType.STRICT || rule.blockType == com.prism.screenharmony.flex.data.BlockType.STRICT
                    ChildRuleCard(
                        rule = rule,
                        statusText = if (!rule.isEnabled) "Disabled" else "Off Schedule",
                        statusColor = MaterialTheme.colorScheme.outline,
                        onClick = { selectedSummaryRule = rule },
                        actionContent = {
                            if (isStrict) {
                                StrictBadge()
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Off Schedule", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Read-Only Summary Bottom Sheet of the Parent Rule
    selectedSummaryRule?.let { rule ->
        ChildBlockSummaryBottomSheet(
            rule = rule,
            onDismiss = { selectedSummaryRule = null }
        )
    }

    // Pause Challenge & Duration Picker Dialog for Child (Faces Parent's PauseMode / Delay / Quote)
    showPauseDialogForRule?.let { rule ->
        val pauseType = rule.pauseConfig.type
        val delayDuration = rule.pauseConfig.extraValue ?: 10

        if (pauseType == com.prism.screenharmony.flex.data.PauseType.PAUSABLE) {
            SimplePauseDurationDialog(
                ruleName = rule.name,
                onConfirm = { mins ->
                    com.prism.screenharmony.flex.data.BlockRepository.pauseRule(rule.id, mins)
                    showPauseDialogForRule = null
                },
                onDismiss = { showPauseDialogForRule = null }
            )
        } else {
            DelayPauseWarningDialog(
                title = "Pause Block",
                ruleName = rule.name,
                durationSeconds = if (pauseType == com.prism.screenharmony.flex.data.PauseType.DELAY) delayDuration else 5,
                onConfirm = { mins ->
                    com.prism.screenharmony.flex.data.BlockRepository.pauseRule(rule.id, mins)
                    showPauseDialogForRule = null
                },
                onDismiss = { showPauseDialogForRule = null }
            )
        }
    }
}

@Composable
private fun ChildRuleCard(
    rule: BlockRule,
    statusText: String,
    statusColor: Color,
    onClick: () -> Unit,
    actionContent: @Composable () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name.ifBlank { "Parental Block" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (rule.selectedApps.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("${rule.selectedApps.size} Apps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    if (rule.selectedWebsites.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("${rule.selectedWebsites.size} Sites", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Text(statusText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = statusColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            actionContent()
        }
    }
}

@Composable
private fun StrictBadge() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Strict", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildBlockSummaryBottomSheet(
    rule: BlockRule,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val pm = remember { context.packageManager }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name.ifBlank { "Parental Block" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enforced by Parent • Read Only",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Restriction Mode Card
                item {
                    val isStrict = rule.pauseConfig.type == PauseType.STRICT || rule.blockType == BlockType.STRICT
                    val isDelay = rule.pauseConfig.type == PauseType.DELAY
                    val isTypeText = rule.pauseConfig.type == PauseType.TYPE_TEXT
                    val delaySec = rule.pauseConfig.extraValue ?: 10

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isStrict) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (isStrict) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isStrict -> Icons.Rounded.Lock
                                    isDelay -> Icons.Rounded.Timer
                                    isTypeText -> Icons.Rounded.TextFields
                                    else -> Icons.Rounded.PauseCircle
                                },
                                contentDescription = null,
                                tint = if (isStrict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restriction Mode: " + when {
                                        isStrict -> "Strict (Unpausable)"
                                        isDelay -> "Delay Countdown (${delaySec}s)"
                                        isTypeText -> "Typing Challenge"
                                        else -> "Standard (Pausable)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStrict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        isStrict -> "This restriction cannot be paused or bypassed by child."
                                        isDelay -> "Pausing requires waiting through a ${delaySec}-second countdown."
                                        isTypeText -> "Pausing requires completing a typing challenge."
                                        else -> "Can be paused directly from parental dashboard."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Blocked Apps Section
                if (rule.selectedApps.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Blocked Apps (${rule.selectedApps.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rule.selectedApps.forEach { pkg ->
                                        val appLabel = remember(pkg) {
                                            try {
                                                val info = pm.getApplicationInfo(pkg, 0)
                                                pm.getApplicationLabel(info).toString()
                                            } catch (_: Exception) {
                                                pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RemoteAppIcon(
                                                packageName = pkg,
                                                appName = appLabel,
                                                size = 36.dp,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = appLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = pkg,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Blocked Websites Section
                if (rule.selectedWebsites.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Blocked Websites (${rule.selectedWebsites.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rule.selectedWebsites.forEach { site ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Rounded.Language,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = site,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Web Intercept Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Active Schedule Section (with exact Weekly Schedule graph and time written below)
                item {
                    val schedule = rule.weeklySchedule
                    val hasSlots = schedule != null && schedule.slots.isNotEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Active Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hasSlots) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Exact Weekly Schedule Graph!
                                    ScheduleGraph(timeSlots = schedule!!.slots)

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                    // Time slots written clearly below
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                                    schedule.slots.forEach { slot ->
                                        val daysText = DayBitmask.toNames(slot.dayBitmask).joinToString(", ")
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = daysText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${slot.startTime.format(formatter)} - ${slot.endTime.format(formatter)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Always Active (24/7)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "This restriction is enforced continuously throughout the week.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
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

// =============================================================================
// CHILD PERMISSIONS CARD DIALOG
// =============================================================================

@Composable
fun ChildPermissionsCardDialog(
    device: RemoteChildDevice,
    onDismiss: () -> Unit
) {
    val perms = device.permissions
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (perms.areAllGranted) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                contentDescription = null,
                tint = if (perms.areAllGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "${device.displayName} Permissions",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${perms.grantedCount} of ${perms.totalCount} permissions are active on this child device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                PermissionStatusItem(
                    name = "Usage Access (Apps)",
                    description = "Detects active running apps to enforce blocks",
                    isGranted = perms.isUsageGranted
                )

                PermissionStatusItem(
                    name = "Display Over Other Apps",
                    description = "Displays the lock screen over apps",
                    isGranted = perms.isOverlayGranted
                )

                PermissionStatusItem(
                    name = "Battery Optimization Disabled",
                    description = "Prevents Android from killing blocker in background",
                    isGranted = perms.isBatteryIgnored
                )

                PermissionStatusItem(
                    name = "Exact Alarms",
                    description = "Wakes up device for scheduled block times",
                    isGranted = perms.isExactAlarmGranted
                )

                PermissionStatusItem(
                    name = "Accessibility Service",
                    description = "Monitors websites & prevents tamper/uninstall",
                    isGranted = perms.isAccessibilityGranted
                )

                PermissionStatusItem(
                    name = "Notifications",
                    description = "Alerts and unlink requests delivery",
                    isGranted = perms.isNotificationGranted
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PermissionStatusItem(
    name: String,
    description: String,
    isGranted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isGranted) Color(0xFF1B5E20).copy(alpha = 0.08f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = if (isGranted) "Active" else "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// =============================================================================
// RESET PARENTAL CONTROLS DIALOG (2-STEP WARNING + RANDOM CODE)
// =============================================================================

@Composable
fun ResetParentalControlsDialog(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var randomCode by remember { mutableStateOf((1000..9999).random().toString()) }
    var enteredText by remember { mutableStateOf("") }

    val requiredPhrase = remember(randomCode) { "remove $randomCode" }
    val isCodeMatch = enteredText.trim().equals(requiredPhrase, ignoreCase = true)

    if (step == 1) {
        // STEP 1: WARNING & WHAT WILL HAPPEN
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Parental Controls?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Resetting parental controls will permanently remove family supervision from all connected devices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "What will happen:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "All connected child devices will be disconnected and unlinked immediately.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "All remote app blocks, website filters, and schedules will be permanently cleared.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Your family pairing code and cloud profile will be completely erased.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        step = 2
                        randomCode = (1000..9999).random().toString()
                        enteredText = ""
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else {
        // STEP 2: CODE VERIFICATION & REMOVE
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Type Confirmation Code",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "To confirm and permanently reset, type the phrase below into the box:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target Code Display Box (above text-field)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Required confirmation phrase:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = requiredPhrase,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = enteredText,
                        onValueChange = { enteredText = it },
                        placeholder = { Text(requiredPhrase) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmed,
                    enabled = isCodeMatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { step = 1 }) {
                    Text("Back")
                }
            }
        )
    }
}
