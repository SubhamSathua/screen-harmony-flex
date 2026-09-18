package com.prism.screenharmony.flex.ui.screens.update

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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
import androidx.compose.ui.window.DialogProperties
import com.prism.screenharmony.flex.update.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KillSwitchDialog(
    item: UpdateCheckResult.KillSwitchTriggered
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { /* Non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GppMaybe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    color = if (item.isGlobal) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, if (item.isGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(
                        text = if (item.isGlobal) "EMERGENCY GLOBAL LOCKDOWN" else "${item.channel.uppercase()} RING PAUSED",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        (context as? Activity)?.finishAffinity()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit Application", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    updateData: UpdateCheckResult.UpdateAvailable,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMandatory = updateData.isFloorEnforced || updateData.updateType == UpdateType.CRITICAL

    val downloadState by ApkDownloader.downloadState.collectAsState()

    AlertDialog(
        onDismissRequest = {
            if (!isMandatory) {
                ApkDownloader.resetState()
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        ),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMandatory) Icons.Rounded.Warning else Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = when {
                                isMandatory -> MaterialTheme.colorScheme.error
                                updateData.updateType == UpdateType.RECOMMENDED -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isMandatory) "Mandatory Update" else "Update Available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Urgency Badge
                    Surface(
                        color = when {
                            isMandatory -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            updateData.updateType == UpdateType.RECOMMENDED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isMandatory) "CRITICAL" else updateData.updateType.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isMandatory -> MaterialTheme.colorScheme.error
                                updateData.updateType == UpdateType.RECOMMENDED -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                Text(
                    text = "v${updateData.config.version} • Current: v${updateData.currentVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Deprecation or Critical Banner
                if (updateData.isFloorEnforced && !updateData.config.deprecationMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = updateData.config.deprecationMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Changelog Section
                if (updateData.config.changelog.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "What's New in v${updateData.config.version}:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        updateData.config.changelog.forEach { logItem ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Text("• ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = logItem,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // In-App Download Progress Card
                when (val state = downloadState) {
                    is DownloadState.Downloading -> {
                        val animatedProgress by animateFloatAsState(targetValue = state.progress, label = "DownloadProgress")
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Downloading APK...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                if (state.totalBytes > 0) {
                                    val currentMb = "%.1f".format(state.downloadedBytes / (1024f * 1024f))
                                    val totalMb = "%.1f".format(state.totalBytes / (1024f * 1024f))
                                    Text("$currentMb MB / $totalMb MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    is DownloadState.Verifying -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Verifying SHA-256 cryptographic digest...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is DownloadState.ReadyToInstall -> {
                        Button(
                            onClick = { ApkDownloader.triggerInstall(context, state.file) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.InstallMobile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Downloaded APK")
                        }
                    }
                    is DownloadState.Failed -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Download Failed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(state.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    is DownloadState.Idle -> {
                        // Options to download
                    }
                }

                // Download Alternative Hub
                if (downloadState is DownloadState.Idle || downloadState is DownloadState.Failed) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Direct APK Option
                        updateData.config.downloads.directApk?.let { directUrl ->
                            Button(
                                onClick = {
                                    scope.launch {
                                        ApkDownloader.downloadAndVerifyApk(
                                            context = context,
                                            downloadUrl = directUrl,
                                            expectedSha256 = updateData.config.sha256,
                                            version = updateData.config.version
                                        ).onSuccess { apkFile ->
                                            ApkDownloader.triggerInstall(context, apkFile)
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download & Install (In-App)")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Obtainium Option
                            updateData.config.downloads.obtainium?.let { obtainiumUri ->
                                OutlinedButton(
                                    onClick = {
                                        val fallback = updateData.config.downloads.github ?: "https://github.com/SubhamSathua/screen-harmony-flex"
                                        UpdateManager.openObtainium(context, obtainiumUri, fallback)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Obtainium", maxLines = 1)
                                }
                            }

                            // GitHub Release Page Option
                            updateData.config.downloads.github?.let { ghUrl ->
                                OutlinedButton(
                                    onClick = {
                                        UpdateManager.openGitHubRelease(context, ghUrl)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("GitHub", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isMandatory) {
                TextButton(
                    onClick = {
                        ApkDownloader.resetState()
                        onDismiss()
                    }
                ) {
                    Text("Later")
                }
            }
        }
    )
}

@Composable
fun UpdateSettingsCard(
    onCheckRequested: () -> Unit
) {
    val context = LocalContext.current
    val currentChannel by UpdateManager.currentChannel.collectAsState()
    val lastCheckTime by UpdateManager.lastCheckedTimestamp.collectAsState()
    val updateResult by UpdateManager.updateResult.collectAsState()

    var showChannelPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
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
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "App Updates & Releases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Channel Selector Chip
                Surface(
                    onClick = { showChannelPicker = true },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentChannel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text(
                text = "ScreenHarmony Flex uses a serverless GitHub Pages manifest to deliver secure releases, emergency patches, and channel updates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current Version & Last Checked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Installed Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "v${UpdateManager.getLocalVersionName(context)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Checked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (lastCheckTime > 0) {
                            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                            sdf.format(Date(lastCheckTime))
                        } else "Never",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Check for Updates Button
            Button(
                onClick = onCheckRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = updateResult !is UpdateCheckResult.Checking,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (updateResult is UpdateCheckResult.Checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking Manifest...")
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for Updates")
                }
            }
        }
    }

    // Channel Picker Dialog
    if (showChannelPicker) {
        AlertDialog(
            onDismissRequest = { showChannelPicker = false },
            title = { Text("Select Update Channel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "stable" to "Production / Stable (Recommended)",
                        "alpha" to "Alpha Testing Ring"
                    ).forEach { (chan, desc) ->
                        Surface(
                            onClick = {
                                UpdateManager.setUpdateChannel(context, chan)
                                showChannelPicker = false
                                onCheckRequested()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentChannel == chan) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(chan.uppercase(), fontWeight = FontWeight.Bold, color = if (currentChannel == chan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Sleek, compact update card designed for top-of-screen placement
 * on primary tabs (Block Page and Parenting Page).
 */
@Composable
fun CompactUpdateCard(
    modifier: Modifier = Modifier,
    onOpenDialog: ((UpdateCheckResult.UpdateAvailable) -> Unit)? = null
) {
    val updateResult by UpdateManager.updateResult.collectAsState()
    val availableUpdate = updateResult as? UpdateCheckResult.UpdateAvailable
    var isDismissedLocally by remember(availableUpdate?.config?.version) { mutableStateOf(false) }
    var showFullDialog by remember { mutableStateOf(false) }

    val isVisible = availableUpdate != null && (!isDismissedLocally || availableUpdate.isFloorEnforced || availableUpdate.updateType == UpdateType.CRITICAL)

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
    ) {
        if (availableUpdate != null) {
            val isCritical = availableUpdate.isFloorEnforced || availableUpdate.updateType == UpdateType.CRITICAL
            val containerColor = if (isCritical) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            }
            val contentColor = if (isCritical) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }

            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCritical) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompact = maxWidth < 340.dp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isCompact) 10.dp else 14.dp,
                                vertical = if (isCompact) 8.dp else 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
                    ) {
                        // Badge / Icon
                        Surface(
                            shape = CircleShape,
                            color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isCompact) 30.dp else 36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isCritical) Icons.Rounded.PriorityHigh else Icons.Rounded.SystemUpdate,
                                    contentDescription = "Update Available",
                                    tint = if (isCritical) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                                )
                            }
                        }

                        // Text Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "v${availableUpdate.config.version}",
                                    style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    maxLines = 1
                                )
                                if (isCritical) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "REQ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (availableUpdate.channel == "alpha") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiary
                                    ) {
                                        Text(
                                            text = "ALPHA",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            val updateSubtitle = when {
                                isCritical -> "Essential security update"
                                !availableUpdate.config.deprecationMessage.isNullOrBlank() -> availableUpdate.config.deprecationMessage
                                availableUpdate.config.changelog.isNotEmpty() -> availableUpdate.config.changelog.first()
                                else -> "Ready to install"
                            }

                            Text(
                                text = updateSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        // Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (onOpenDialog != null) {
                                        onOpenDialog(availableUpdate)
                                    } else {
                                        showFullDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(
                                    horizontal = if (isCompact) 8.dp else 12.dp,
                                    vertical = 4.dp
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isCritical) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "Update",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!isCritical) {
                                IconButton(
                                    onClick = { isDismissedLocally = true },
                                    modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss",
                                        tint = contentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showFullDialog) {
                AppUpdateDialog(
                    updateData = availableUpdate,
                    onDismiss = { showFullDialog = false }
                )
            }
        }
    }
}

